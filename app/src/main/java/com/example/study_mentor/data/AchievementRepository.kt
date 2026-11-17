
package com.example.study_mentor.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.sqrt

object AchievementRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun getAchievementRef(
        uid: String,
        achievementId: String
    ) = db.collection("users")
        .document(uid)
        .collection("achievements")
        .document(achievementId)

    private fun getStatisticsRef(uid: String) =
        db.collection("users")
            .document(uid)
            .collection("statistics")
            .document("summary")

    private fun calculateLevel(xp: Long): Int {
        if (xp < 0L) return 1

        val value = xp / 25.0

        val level = (
                (-1.0 + sqrt(9.0 + 4.0 * value)) / 2.0
                ).toInt()

        return maxOf(1, level)
    }

    private fun isEligible(
        achievementId: String,
        progress: AchievementProgress
    ): Boolean {
        return when (achievementId) {
            "first_question" ->
                progress.totalQuestionsAsked >= 1

            "first_quiz" ->
                progress.totalQuizAttempts >= 1

            "quiz_2" ->
                progress.totalQuizAttempts >= 2

            "quiz_10" ->
                progress.totalQuizAttempts >= 10

            "quiz_50" ->
                progress.totalQuizAttempts >= 50

            "correct_10" ->
                progress.quizCorrectTotal >= 10

            "questions_50" ->
                progress.totalQuestionsAsked >= 50

            "streak_3" ->
                progress.dayStreak >= 3

            "streak_7" ->
                progress.dayStreak >= 7

            "xp_100" ->
                progress.totalXp >= 100

            "level_5" ->
                progress.level >= 5

            "level_10" ->
                progress.level >= 10

            "level_15" ->
                progress.level >= 15

            else -> false
        }
    }

    suspend fun checkAndUnlockAchievements(
        uid: String,
        progress: AchievementProgress
    ): List<String> {

        val unlockedAchievements = mutableListOf<String>()

        var currentProgress = progress

        var foundNewAchievement: Boolean

        do {
            foundNewAchievement = false

            for (achievement in AchievementDefinitions.achievements) {

                if (!isEligible(
                        achievement.id,
                        currentProgress
                    )
                ) continue

                val achievementRef = getAchievementRef(
                    uid,
                    achievement.id
                )

                val statsRef = getStatisticsRef(uid)

                val result = db.runTransaction { transaction ->

                    // Read all documents before writing.
                    val achievementSnapshot =
                        transaction.get(achievementRef)

                    if (
                        achievementSnapshot.getBoolean("isUnlocked")
                        == true
                    ) {
                        return@runTransaction null
                    }

                    val statsSnapshot = transaction.get(statsRef)

                    val oldXp =
                        statsSnapshot.getLong("totalXp") ?: 0L

                    val newXp = Math.addExact(
                        oldXp,
                        achievement.rewardXp.toLong()
                    )

                    val newLevel = calculateLevel(newXp)

                    val achievementData = hashMapOf<String, Any>(
                        "achievementId" to achievement.id,
                        "name" to achievement.name,
                        "description" to achievement.description,
                        "requirement" to achievement.requirement,
                        "rewardXp" to achievement.rewardXp,
                        "isUnlocked" to true,
                        "unlockedAt" to FieldValue.serverTimestamp()
                    )

                    transaction.set(
                        achievementRef,
                        achievementData
                    )

                    val statsUpdates = hashMapOf<String, Any>(
                        "totalXp" to newXp,
                        "level" to newLevel
                    )

// Update statistics/summary
                    transaction.set(
                        statsRef,
                        statsUpdates,
                        SetOptions.merge()
                    )

// Synchronize users/{uid}
                    val userRef = db.collection("users")
                        .document(uid)

                    transaction.set(
                        userRef,
                        mapOf(
                            "totalXp" to newXp,
                            "level" to newLevel,
                            "dayStreak" to (
                                    statsSnapshot.getLong("dayStreak")?.toInt() ?: 0
                                    ),
                            "longestStreak" to (
                                    statsSnapshot.getLong("longestStreak")?.toInt() ?: 0
                                    )
                        ),
                        SetOptions.merge()
                    )

                    newXp to newLevel
                }.await()

                if (result != null) {

                    unlockedAchievements.add(achievement.id)

                    currentProgress = currentProgress.copy(
                        totalXp = result.first,
                        level = result.second
                    )

                    foundNewAchievement = true
                }
            }

        } while (foundNewAchievement)

        return unlockedAchievements
    }

    suspend fun getAchievements(
        uid: String
    ): List<AchievementStatus> {

        val snapshot = db.collection("users")
            .document(uid)
            .collection("achievements")
            .get()
            .await()

        val unlockedIds = snapshot.documents
            .filter {
                it.getBoolean("isUnlocked") == true
            }
            .map {
                it.id
            }
            .toSet()

        return AchievementDefinitions.achievements.map { achievement ->

            AchievementStatus(
                achievement = achievement,
                isUnlocked = achievement.id in unlockedIds
            )
        }
    }
}
