package com.example.study_mentor.data

import com.example.study_mentor.OpenAIApiClient
import com.example.study_mentor.ui.ChatViewModel
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

private class QuizParseException(message: String) : Exception(message)

object QuizGenerator {

    private const val SESSION_LIMIT = 5
    private const val MESSAGES_PER_SESSION = 30
    private const val PAIR_LIMIT = 8
    private const val SNIPPET_LENGTH = 300
    private const val MAX_TOPIC_LENGTH = 200
    private const val QUIZ_MAX_TOKENS = 1500
    private const val MIN_VALID_ITEMS = 3
    private const val IMAGE_PREFIX = "[Image attached]"

    private const val SYSTEM_PROMPT =
        "You are a quiz generator for students. Respond with ONE valid JSON object only. " +
            "No markdown, no code fences, no text outside the JSON."

    private const val COMMON_RULES =
        "Create exactly 5 questions: 3 multiple_choice (4 options each), 1 short_answer, " +
            "1 fill_blank (use ____ for the blank).\n" +
            "Keep each explanation to one or two sentences."

    private val QUIZ_FORMAT = """
Return ONLY a JSON object in this exact shape:
{
  "title": "short quiz title",
  "subject": "math | science | programming | history | languages | other",
  "topic": "short topic name",
  "difficulty": "easy | medium | hard",
  "items": [
    {"type": "multiple_choice", "prompt": "...", "options": ["...", "...", "...", "..."], "correctIndex": 0, "explanation": "..."},
    {"type": "short_answer", "prompt": "...", "answer": "...", "explanation": "..."},
    {"type": "fill_blank", "prompt": "... ____ ...", "answer": "...", "explanation": "..."}
  ]
}
correctIndex is 0-based.
""".trimIndent()

    private data class ChatMsg(
        val id: String,
        val role: String,
        val text: String,
        val timestamp: Long
    )

    private data class QaPair(
        val id: String,
        val question: String,
        val answer: String,
        val timestamp: Long
    )

    private val db = FirebaseFirestore.getInstance()

    /**
     * Returns chat sessions for the selection dialog.
     * The collection path and message fields match the existing chat generator:
     * users/{uid}/chat_sessions/{sessionId}/messages.
     */
    suspend fun loadChatSessions(uid: String): List<QuizChatSessionOption> {
        val sessions = db.collection("users")
            .document(uid)
            .collection("chat_sessions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .get()
            .await()

        return sessions.documents.map { session ->
            val recentMessages = session.reference.collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()

            val preview = recentMessages.documents
                .firstOrNull { it.getString("role") == "user" }
                ?.getString("text")
                .orEmpty()
                .removePrefix(IMAGE_PREFIX)
                .trim()
                .ifBlank { "Chưa có nội dung xem trước" }

            // If your chat documents use another title field, update this fallback.
            val storedTitle = session.getString("title")
                ?: session.getString("name")

            QuizChatSessionOption(
                id = session.id,
                title = storedTitle?.takeIf { it.isNotBlank() }
                    ?: preview.take(45).let {
                        if (it.length >= 45) "$it..." else it
                    },
                preview = preview
            )
        }
    }

    /**
     * Provide exactly one source:
     * - topic: generate from a typed topic;
     * - sessionId: generate from one selected conversation.
     *
     * The default empty source is retained for compatibility with older callers:
     * it generates from recent conversations.
     */
    suspend fun generate(
        uid: String,
        topic: String? = null,
        sessionId: String? = null
    ): Quiz {
        val cleanTopic = topic?.trim().orEmpty().take(MAX_TOPIC_LENGTH)
        val cleanSessionId = sessionId?.trim().orEmpty()

        if (cleanTopic.isNotEmpty() && cleanSessionId.isNotEmpty()) {
            throw IllegalArgumentException("Choose either a topic or a chat, not both.")
        }

        val userRef = db.collection("users").document(uid)
        val educationLevel = userRef.get().await()
            .getString("educationLevel")
            .orEmpty()
            .ifBlank { "general" }
            .replace('_', ' ')

        val prompt: String
        val sourceIds: List<String>
        val sourceType: String
        val sourceId: String
        val sourceTitle: String
        val fallbackTopic: String

        when {
            cleanTopic.isNotBlank() -> {
                prompt = buildTopicPrompt(educationLevel, cleanTopic)
                sourceIds = emptyList()
                sourceType = "topic"
                sourceId = cleanTopic
                sourceTitle = cleanTopic
                fallbackTopic = cleanTopic
            }

            cleanSessionId.isNotBlank() -> {
                val sessionRef = userRef.collection("chat_sessions").document(cleanSessionId)
                val sessionSnapshot = sessionRef.get().await()
                if (!sessionSnapshot.exists()) {
                    throw IllegalStateException("The selected conversation could not be found.")
                }

                val pairs = loadPairsFromSession(sessionRef)
                if (pairs.isEmpty()) {
                    throw IllegalStateException(
                        "This conversation does not contain enough question-and-answer content to create a quiz."
                    )
                }

                val storedTitle = sessionSnapshot.getString("title")
                    ?: sessionSnapshot.getString("name")
                sourceTitle = storedTitle?.takeIf { it.isNotBlank() }
                    ?: pairs.first().question.take(45)
                sourceIds = pairs.map { it.id }
                sourceType = "chat"
                sourceId = cleanSessionId
                fallbackTopic = sourceTitle

                val sourceText = pairs.mapIndexed { index, pair ->
                    "${index + 1}. Question: ${pair.question.take(SNIPPET_LENGTH)}\n" +
                        "   Answer: ${pair.answer.take(SNIPPET_LENGTH)}"
                }.joinToString("\n")
                prompt = buildChatPrompt(educationLevel, sourceText, sourceTitle)
            }

            else -> {
                // Compatibility fallback for any existing caller still using generate(uid).
                val pairs = loadRecentPairs(userRef)
                if (pairs.isEmpty()) {
                    throw IllegalStateException(
                        "Please enter a topic or select a conversation before generating a quiz."
                    )
                }
                sourceIds = pairs.map { it.id }
                sourceType = "chat"
                sourceId = ""
                sourceTitle = "Recent conversations"
                fallbackTopic = sourceTitle
                val sourceText = pairs.mapIndexed { index, pair ->
                    "${index + 1}. Question: ${pair.question.take(SNIPPET_LENGTH)}\n" +
                        "   Answer: ${pair.answer.take(SNIPPET_LENGTH)}"
                }.joinToString("\n")
                prompt = buildChatPrompt(educationLevel, sourceText, sourceTitle)
            }
        }

        var lastParseError: QuizParseException? = null
        repeat(2) {
            val raw = callAi(prompt)
            try {
                return parse(
                    raw = raw,
                    sourceIds = sourceIds,
                    fallbackTopic = fallbackTopic,
                    sourceType = sourceType,
                    sourceId = sourceId,
                    sourceTitle = sourceTitle
                )
            } catch (e: QuizParseException) {
                lastParseError = e
            }
        }

        throw IllegalStateException(
            "The AI returned an invalid quiz (${lastParseError?.message}). Please try again."
        )
    }

    private suspend fun loadRecentPairs(userRef: DocumentReference): List<QaPair> {
        val sessions = userRef.collection("chat_sessions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(SESSION_LIMIT.toLong())
            .get()
            .await()

        val allPairs = mutableListOf<QaPair>()
        sessions.documents.forEach { session ->
            allPairs += loadPairsFromSession(session.reference)
        }
        return allPairs.sortedByDescending { it.timestamp }.take(PAIR_LIMIT)
    }

    private suspend fun loadPairsFromSession(
        sessionRef: DocumentReference
    ): List<QaPair> {
        val snapshot = sessionRef.collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(MESSAGES_PER_SESSION.toLong())
            .get()
            .await()

        val messages = snapshot.documents.mapNotNull { doc ->
            val role = doc.getString("role") ?: return@mapNotNull null
            ChatMsg(
                id = doc.id,
                role = role,
                text = doc.getString("text").orEmpty(),
                timestamp = doc.getLong("timestamp") ?: 0L
            )
        }.reversed()

        val pairs = mutableListOf<QaPair>()
        var pending: ChatMsg? = null

        for (message in messages) {
            if (message.role == "user") {
                pending = message
            } else if (message.role == "ai" && pending != null) {
                val userText = pending.text.removePrefix(IMAGE_PREFIX).trim()
                val question = if (
                    userText.isBlank() ||
                    userText == ChatViewModel.DEFAULT_IMAGE_PROMPT
                ) {
                    "(a problem shown in a photo - the answer below describes it)"
                } else {
                    userText
                }

                if (message.text.isNotBlank()) {
                    pairs += QaPair(
                        id = pending.id,
                        question = question,
                        answer = message.text,
                        timestamp = pending.timestamp
                    )
                }
                pending = null
            }
        }

        return pairs.sortedByDescending { it.timestamp }.take(PAIR_LIMIT)
    }

    private fun buildChatPrompt(
        educationLevel: String,
        sourceText: String,
        sourceTitle: String
    ): String = """
Create a practice quiz for a $educationLevel student based ONLY on the following conversation: "$sourceTitle".
Use the student's questions and the AI's answers as the source of facts.
Do not introduce unrelated topics or facts that are not supported by the conversation.
Write all quiz titles, questions, options, answers and explanations in Vietnamese. Keep JSON keys and enum values in English.
$COMMON_RULES
If a question says it is a problem shown in a photo, base your quiz question on the content of its answer.

$QUIZ_FORMAT

Conversation content:
$sourceText
""".trimIndent()

    private fun buildTopicPrompt(educationLevel: String, topic: String): String = """
Create a practice quiz for a $educationLevel student about this topic: "$topic".
Write all quiz titles, questions, options, answers and explanations in Vietnamese. Keep JSON keys and enum values in English.
$COMMON_RULES

$QUIZ_FORMAT
""".trimIndent()

    private suspend fun callAi(userPrompt: String): String {
        val builder = StringBuilder()
        OpenAIApiClient.askQuestion(
            prompt = userPrompt,
            systemPrompt = SYSTEM_PROMPT,
            maxTokens = QUIZ_MAX_TOKENS
        ).collect { builder.append(it) }
        return builder.toString()
    }

    private fun parse(
        raw: String,
        sourceIds: List<String>,
        fallbackTopic: String,
        sourceType: String,
        sourceId: String,
        sourceTitle: String
    ): Quiz {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) {
            throw QuizParseException("no JSON found")
        }

        val root = try {
            JSONObject(raw.substring(start, end + 1))
        } catch (e: Exception) {
            throw QuizParseException("malformed JSON")
        }

        val array = root.optJSONArray("items")
            ?: throw QuizParseException("no items")

        val items = buildList {
            for (i in 0 until array.length()) {
                parseItem(array.optJSONObject(i))?.let { add(it) }
            }
        }

        if (items.size < MIN_VALID_ITEMS) {
            throw QuizParseException("too few valid questions")
        }

        val difficulty = root.optString("difficulty")
            .trim()
            .lowercase()
            .takeIf { it in setOf("easy", "medium", "hard") }
            ?: "medium"

        return Quiz(
            title = root.optString("title").trim()
                .ifBlank { fallbackTopic.ifBlank { "Practice quiz" } },
            subject = root.optString("subject").trim().lowercase(),
            topic = root.optString("topic").trim().ifBlank { fallbackTopic },
            difficulty = difficulty,
            xpReward = items.size * 10,
            sourceQuestionIds = sourceIds,
            items = items,
            sourceType = sourceType,
            sourceId = sourceId,
            sourceTitle = sourceTitle
        )
    }

    private fun parseItem(obj: JSONObject?): QuizItem? {
        if (obj == null) return null
        val type = obj.optString("type").trim()
        val prompt = obj.optString("prompt").trim()
        val explanation = obj.optString("explanation").trim()
        if (prompt.isBlank()) return null

        return when (type) {
            "multiple_choice" -> {
                val optionsArray = obj.optJSONArray("options") ?: return null
                val options = (0 until optionsArray.length())
                    .map { optionsArray.optString(it).trim() }
                val correctIndex = obj.optInt("correctIndex", -1)

                if (
                    options.size < 2 ||
                    options.any { it.isBlank() } ||
                    correctIndex !in options.indices
                ) {
                    null
                } else {
                    QuizItem(
                        type = type,
                        prompt = prompt,
                        options = options,
                        correctIndex = correctIndex,
                        explanation = explanation
                    )
                }
            }

            "short_answer", "fill_blank" -> {
                val answer = obj.optString("answer").trim()
                if (answer.isBlank()) null
                else QuizItem(
                    type = type,
                    prompt = prompt,
                    answer = answer,
                    explanation = explanation
                )
            }

            else -> null
        }
    }
}
