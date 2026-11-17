
package com.example.study_mentor.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class LeaderboardUser(
    val uid: String = "",
    val name: String = "Student",
    val photoUrl: String? = null,
    val totalXp: Long = 0L,
    val level: Int = 1,
    val dayStreak: Int = 0,
    val longestStreak: Int = 0,
    val rank: Int = 0
)

class LeaderboardRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun observeLeaderboard(): Flow<List<LeaderboardUser>> =
        callbackFlow {

            val registration = db.collection("users")
                .orderBy("totalXp", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null) return@addSnapshotListener

                    val users = snapshot.documents.mapIndexed { index, document ->

                        LeaderboardUser(
                            uid = document.id,
                            name = document.getString("name")
                                ?.takeIf { it.isNotBlank() }
                                ?: "Student",

                            photoUrl = document.getString("photoUrl"),

                            totalXp = document.getLong("totalXp") ?: 0L,

                            level = document.getLong("level")
                                ?.toInt() ?: 1,

                            rank = index + 1
                        )
                    }

                    trySend(users)
                }

            awaitClose {
                registration.remove()
            }
        }

    fun observeStreakLeaderboard(): Flow<List<LeaderboardUser>> =
        callbackFlow {
            val registration = db.collection("users")
                .orderBy("dayStreak", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null) return@addSnapshotListener

                    val users = snapshot.documents.mapIndexed { index, document ->
                        LeaderboardUser(
                            uid = document.id,
                            name = document.getString("name")
                                ?.takeIf { it.isNotBlank() } ?: "Student",
                            photoUrl = document.getString("photoUrl"),
                            totalXp = document.getLong("totalXp") ?: 0L,
                            level = document.getLong("level")?.toInt() ?: 1,
                            dayStreak = document.getLong("dayStreak")?.toInt() ?: 0,
                            longestStreak = document.getLong("longestStreak")?.toInt() ?: 0,
                            rank = index + 1
                        )
                    }

                    trySend(users)
                }

            awaitClose {
                registration.remove()
            }
        }


    fun getCurrentUid(): String {
        return auth.currentUser?.uid.orEmpty()
    }
}
