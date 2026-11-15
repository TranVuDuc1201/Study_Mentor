package com.example.study_mentor.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/** One question inside a quiz. type = multiple_choice | short_answer | fill_blank */
data class QuizItem(
    val type: String = "multiple_choice",
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val answer: String = "",
    val explanation: String = ""
)

/** A conversation displayed in the quiz source selector. */
data class QuizChatSessionOption(
    val id: String = "",
    val title: String = "",
    val preview: String = ""
)

/** Maps to users/{uid}/quizzes/{quizId}. */
data class Quiz(
    val quizId: String = "",
    val title: String = "",
    val subject: String = "",
    val topic: String = "",
    val difficulty: String = "medium",
    val xpReward: Int = 0,
    val sourceQuestionIds: List<String> = emptyList(),
    val items: List<QuizItem> = emptyList(),
    val sourceType: String = "topic", // "topic" or "chat"
    val sourceId: String = "",
    val sourceTitle: String = "",
    val createdAt: Date? = null
)

object QuizRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun quizzesRef(uid: String) =
        db.collection("users").document(uid).collection("quizzes")

    suspend fun saveQuiz(uid: String, quiz: Quiz): String {
        val ref = quizzesRef(uid).document()
        val data = mapOf(
            "quizId" to ref.id,
            "title" to quiz.title,
            "subject" to quiz.subject,
            "topic" to quiz.topic,
            "difficulty" to quiz.difficulty,
            "xpReward" to quiz.xpReward,
            "sourceQuestionIds" to quiz.sourceQuestionIds,
            "sourceType" to quiz.sourceType,
            "sourceId" to quiz.sourceId,
            "sourceTitle" to quiz.sourceTitle,
            "items" to quiz.items.map {
                mapOf(
                    "type" to it.type,
                    "prompt" to it.prompt,
                    "options" to it.options,
                    "correctIndex" to it.correctIndex,
                    "answer" to it.answer,
                    "explanation" to it.explanation
                )
            },
            "createdAt" to FieldValue.serverTimestamp()
        )
        ref.set(data).await()
        return ref.id
    }

    fun observeQuizzes(uid: String): Flow<List<Quiz>> = callbackFlow {
        val registration = quizzesRef(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Quiz::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }
}
