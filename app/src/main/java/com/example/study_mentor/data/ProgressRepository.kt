
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

class ProgressRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

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
}
