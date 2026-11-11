
package com.example.study_mentor.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class ProgressSession(
    val id: String,
    val createdAt: Long
)

data class QuizProgressRecord(
    val id: String,
    val subject: String,
    val score: Int,
    val totalQuestions: Int
)

class ProgressRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    /**
     * Observe AI chat sessions belonging to one user.
     */
    fun observeStudySessions(
        uid: String
    ): Flow<List<ProgressSession>> = callbackFlow {

        val registration = firestore
            .collection("users")
            .document(uid)
            .collection("chat_sessions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val sessions = snapshot?.documents
                    ?.mapNotNull { document ->
                        val createdAt =
                            document.getLong("createdAt")

                        if (createdAt != null) {
                            ProgressSession(
                                id = document.id,
                                createdAt = createdAt
                            )
                        } else {
                            null
                        }
                    }
                    .orEmpty()

                trySend(sessions)
            }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Observe completed quiz results belonging to one user.
     *
     * Firestore path:
     * users/{uid}/quiz_results/{resultId}
     */
    fun observeQuizResults(
        uid: String
    ): Flow<List<QuizProgressRecord>> = callbackFlow {

        val registration = firestore
            .collection("users")
            .document(uid)
            .collection("quiz_results")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val results = snapshot?.documents
                    ?.mapNotNull { document ->

                        val score =
                            document.getLong("score")?.toInt()

                        val totalQuestions =
                            document.getLong("totalQuestions")?.toInt()

                        if (
                            score == null ||
                            totalQuestions == null ||
                            totalQuestions < 0 ||
                            score !in 0..totalQuestions
                        ) {
                            return@mapNotNull null
                        }

                        val subject = document
                            .getString("subject")
                            ?.trim()
                            .orEmpty()
                            .ifBlank { "Other" }

                        QuizProgressRecord(
                            id = document.id,
                            subject = subject,
                            score = score,
                            totalQuestions = totalQuestions
                        )
                    }
                    .orEmpty()

                trySend(results)
            }

        awaitClose {
            registration.remove()
        }
    }
}
