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

    val ALLOWED_QUESTION_TYPES = setOf("multiple_choice", "short_answer", "fill_blank")

    private const val SESSION_LIMIT = 5
    private const val MESSAGES_PER_SESSION = 30
    private const val PAIR_LIMIT = 8
    private const val SNIPPET_LENGTH = 300
    private const val MAX_TOPIC_LENGTH = 200
    private const val QUIZ_MAX_TOKENS = 1500
    private const val TOKENS_PER_QUESTION = 220
    private const val MIN_QUESTION_COUNT = 1
    private const val MAX_QUESTION_COUNT = 15
    private const val DEFAULT_QUESTION_COUNT = 5
    private const val MIN_VALID_ITEMS = 3
    private const val IMAGE_PREFIX = "[Image attached]"
    val ALLOWED_DIFFICULTIES = setOf("easy", "medium", "hard")
    private const val DEFAULT_DIFFICULTY = "medium"

    private const val SYSTEM_PROMPT =
        "You are a quiz generator for students. Respond with ONE valid JSON object only. " +
                "No markdown, no code fences, no text outside the JSON."

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
correctIndex is 0-based. Only include item objects whose "type" is in the allowed question types.
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
    private fun buildDifficultyRule(difficulty: String): String = when (difficulty) {
        "easy" -> "Difficulty level: EASY. Focus on basic definitions, direct recall, and straightforward concepts."
        "hard" -> "Difficulty level: HARD. Focus on deeper analysis, edge cases, multi-step reasoning, and subtle distinctions."
        else -> "Difficulty level: MEDIUM. Balance conceptual understanding and practical application."
    }

    /**
     * Provide exactly one source:
     * - topic: generate from a typed topic;
     * - sessionId: generate from one selected conversation.
     *
     * Optionally customize:
     * - questionCount: total number of questions (1..15)
     * - allowedTypes: subset of {"multiple_choice", "short_answer", "fill_blank"}
     */
    suspend fun generate(
        uid: String,
        topic: String? = null,
        sessionId: String? = null,
        questionCount: Int = DEFAULT_QUESTION_COUNT,
        allowedTypes: Set<String> = ALLOWED_QUESTION_TYPES,
        difficulty: String = DEFAULT_DIFFICULTY
    ): Quiz {
        val cleanTopic = topic?.trim().orEmpty().take(MAX_TOPIC_LENGTH)
        val cleanSessionId = sessionId?.trim().orEmpty()
        val cleanQuestionCount = questionCount.coerceIn(MIN_QUESTION_COUNT, MAX_QUESTION_COUNT)
        val cleanTypes = allowedTypes
            .map { it.trim() }
            .filter { it in ALLOWED_QUESTION_TYPES }
            .toSet()
            .ifEmpty { ALLOWED_QUESTION_TYPES }
        val cleanDifficulty = difficulty.trim().lowercase().takeIf { it in ALLOWED_DIFFICULTIES }
            ?: DEFAULT_DIFFICULTY

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
                prompt = buildTopicPrompt(
                    educationLevel = educationLevel,
                    topic = cleanTopic,
                    questionCount = cleanQuestionCount,
                    allowedTypes = cleanTypes,
                    difficulty = cleanDifficulty
                )
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
                prompt = buildChatPrompt(
                    educationLevel = educationLevel,
                    sourceText = sourceText,
                    sourceTitle = sourceTitle,
                    questionCount = cleanQuestionCount,
                    allowedTypes = cleanTypes,
                    difficulty = cleanDifficulty
                )
            }

            else -> {
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
                prompt = buildChatPrompt(
                    educationLevel = educationLevel,
                    sourceText = sourceText,
                    sourceTitle = sourceTitle,
                    questionCount = cleanQuestionCount,
                    allowedTypes = cleanTypes,
                    difficulty = cleanDifficulty
                )
            }
        }

        val maxTokens = maxOf(QUIZ_MAX_TOKENS, cleanQuestionCount * TOKENS_PER_QUESTION)
        var lastParseError: QuizParseException? = null
        repeat(2) {
            val raw = callAi(userPrompt = prompt, maxTokens = maxTokens)
            try {
                return parse(
                    raw = raw,
                    sourceIds = sourceIds,
                    fallbackTopic = fallbackTopic,
                    sourceType = sourceType,
                    sourceId = sourceId,
                    sourceTitle = sourceTitle,
                    expectedCount = cleanQuestionCount,
                    allowedTypes = cleanTypes,
                    difficulty = cleanDifficulty
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

    private fun buildQuestionRules(
        questionCount: Int,
        allowedTypes: Set<String>,
        difficulty: String
    ): String {
        val orderedTypes = ALLOWED_QUESTION_TYPES.filter { it in allowedTypes }
        val descriptions = orderedTypes.map { type ->
            when (type) {
                "multiple_choice" -> "multiple_choice (4 options each)"
                "short_answer" -> "short_answer"
                "fill_blank" -> "fill_blank (use ____ for the blank)"
                else -> type
            }
        }

        val typeRule = if (descriptions.size == 1) {
            "Use ONLY the question type: ${descriptions.first()}."
        } else {
            "Use ONLY these question types, distributed as evenly as possible: ${descriptions.joinToString(", ")}."
        }

        return """
Create exactly $questionCount questions in total.
$typeRule
${buildDifficultyRule(difficulty)}
Keep each explanation to one or two sentences.
""".trimIndent()
    }

    private fun buildChatPrompt(
        educationLevel: String,
        sourceText: String,
        sourceTitle: String,
        questionCount: Int,
        allowedTypes: Set<String>,
        difficulty: String
    ): String {
        val rules = buildQuestionRules(questionCount, allowedTypes, difficulty)
        return """
Create a practice quiz for a $educationLevel student based ONLY on the following conversation: "$sourceTitle".
Use the student's questions and the AI's answers as the source of facts.
Do not introduce unrelated topics or facts that are not supported by the conversation.
Write all quiz titles, questions, options, answers and explanations in Vietnamese. Keep JSON keys and enum values in English.
$rules
If a question says it is a problem shown in a photo, base your quiz question on the content of its answer.

$QUIZ_FORMAT

Conversation content:
$sourceText
""".trimIndent()
    }

    private fun buildTopicPrompt(
        educationLevel: String,
        topic: String,
        questionCount: Int,
        allowedTypes: Set<String>,
        difficulty: String
    ): String {
        val rules = buildQuestionRules(questionCount, allowedTypes, difficulty)
        return """
Create a practice quiz for a $educationLevel student about this topic: "$topic".
Write all quiz titles, questions, options, answers and explanations in Vietnamese. Keep JSON keys and enum values in English.
$rules

$QUIZ_FORMAT
""".trimIndent()
    }

    private suspend fun callAi(
        userPrompt: String,
        maxTokens: Int
    ): String {
        val builder = StringBuilder()
        OpenAIApiClient.askQuestion(
            prompt = userPrompt,
            systemPrompt = SYSTEM_PROMPT,
            maxTokens = maxTokens
        ).collect { builder.append(it) }
        return builder.toString()
    }

    private fun parse(
        raw: String,
        sourceIds: List<String>,
        fallbackTopic: String,
        sourceType: String,
        sourceId: String,
        sourceTitle: String,
        expectedCount: Int,
        allowedTypes: Set<String>,
        difficulty: String = DEFAULT_DIFFICULTY
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
                parseItem(array.optJSONObject(i), allowedTypes)?.let { add(it) }
                if (size >= expectedCount) break
            }
        }

        val minRequired = minOf(MIN_VALID_ITEMS, expectedCount)
        if (items.size < minRequired) {
            throw QuizParseException("too few valid questions")
        }

        val resolvedDifficulty = difficulty.trim().lowercase().takeIf { it in ALLOWED_DIFFICULTIES }
            ?: root.optString("difficulty")
                .trim()
                .lowercase()
                .takeIf { it in ALLOWED_DIFFICULTIES }
            ?: DEFAULT_DIFFICULTY

        return Quiz(
            title = root.optString("title").trim()
                .ifBlank { fallbackTopic.ifBlank { "Practice quiz" } },
            subject = root.optString("subject").trim().lowercase(),
            topic = root.optString("topic").trim().ifBlank { fallbackTopic },
            difficulty = resolvedDifficulty,
            xpReward = items.size * 10,
            sourceQuestionIds = sourceIds,
            items = items,
            sourceType = sourceType,
            sourceId = sourceId,
            sourceTitle = sourceTitle
        )
    }

    private fun parseItem(
        obj: JSONObject?,
        allowedTypes: Set<String>
    ): QuizItem? {
        if (obj == null) return null
        val type = obj.optString("type").trim()
        if (type !in allowedTypes) return null

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