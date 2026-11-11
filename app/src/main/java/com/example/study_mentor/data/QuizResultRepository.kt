package com.example.study_mentor.data


import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object QuizResultRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun saveQuizResult(
        uid: String,
        quizId: String,
        quizTitle: String,
        subject: String,
        topic: String,
        difficulty: String,
        score: Int,
        totalQuestions: Int,
        xpEarned: Int,
        answers: List<Map<String, Any?>>
    ): String {

        val resultRef = db
            .collection("users")
            .document(uid)
            .collection("quiz_results")
            .document()

        val percentage = if (totalQuestions > 0) {
            (score.toDouble() / totalQuestions.toDouble()) * 100
        } else {
            0.0
        }

        val data = hashMapOf(
            "resultId" to resultRef.id,
            "quizId" to quizId,
            "quizTitle" to quizTitle,
            "subject" to subject,
            "topic" to topic,
            "difficulty" to difficulty,
            "score" to score,
            "totalQuestions" to totalQuestions,
            "percentage" to percentage,
            "xpEarned" to xpEarned,
            "answers" to answers,
            "completedAt" to FieldValue.serverTimestamp()
        )

        resultRef.set(data).await()

        return resultRef.id
    }
}