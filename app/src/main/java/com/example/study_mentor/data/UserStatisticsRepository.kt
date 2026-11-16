
package com.example.study_mentor.data

import com.example.study_mentor.ui.AchievementEventManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.math.sqrt
import com.google.firebase.firestore.Transaction

class UserStatisticsRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun currentUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User is not logged in")
    }

    private fun statisticsRef(): DocumentReference {
        return db.collection("users")
            .document(currentUid())
            .collection("statistics")
            .document("summary")
    }

    // Read updated statistics and check achievements
    suspend fun checkAchievements() {
        val uid = currentUid()
        val snapshot = statisticsRef().get().await()

        val progress = AchievementProgress(
            totalQuestionsAsked =
                snapshot.getLong("totalQuestionsAsked")?.toInt() ?: 0,

            totalQuizAttempts =
                snapshot.getLong("totalQuizAttempts")?.toInt() ?: 0,

            quizCorrectTotal =
                snapshot.getLong("quizCorrectTotal")?.toInt() ?: 0,

            dayStreak =
                snapshot.getLong("dayStreak")?.toInt() ?: 0,

            totalXp =
                snapshot.getLong("totalXp") ?: 0L,

            level =
                snapshot.getLong("level")?.toInt() ?: 1
        )

        val unlocked = AchievementRepository
            .checkAndUnlockAchievements(uid, progress)

        if (unlocked.isNotEmpty()) {
            AchievementEventManager.notifyUnlocked(unlocked)
        }
    }

    // Update daily activity

    private fun updateDailyActivity(
        snapshot: com.google.firebase.firestore.DocumentSnapshot,
        updates: MutableMap<String, Any>,
        transaction: Transaction,
        userRef: DocumentReference
    ) {
        val today = LocalDate.now()

        val lastActiveDate = snapshot.getString("lastActiveDate") ?: ""

        val oldStreak = snapshot.getLong("dayStreak")?.toInt() ?: 0

        val oldLongestStreak =
            snapshot.getLong("longestStreak")?.toInt() ?: 0

        val newStreak = try {
            when {
                lastActiveDate.isEmpty() -> 1

                LocalDate.parse(lastActiveDate) == today ->
                    oldStreak

                LocalDate.parse(lastActiveDate).plusDays(1) == today ->
                    oldStreak + 1

                else -> 1
            }
        } catch (e: DateTimeParseException) {
            1
        }

        val newLongestStreak = maxOf(
            oldLongestStreak,
            newStreak
        )

        updates["dayStreak"] = newStreak
        updates["longestStreak"] = newLongestStreak
        updates["lastActiveDate"] = today.toString()
        updates["updatedAt"] = FieldValue.serverTimestamp()

        // Get the latest XP and level from statistics/summary.
        // If the current transaction changes them, use the new values.
        val totalXp = updates["totalXp"]
            ?: (snapshot.getLong("totalXp") ?: 0L)

        val level = updates["level"]
            ?: (snapshot.getLong("level")?.toInt() ?: 1)

        // Synchronize all four fields to users/{uid}
        transaction.set(
            userRef,
            mapOf(
                "dayStreak" to newStreak,
                "longestStreak" to newLongestStreak,
                "totalXp" to totalXp,
                "level" to level
            ),
            SetOptions.merge()
        )
    }


    // 1. Record AI question
    suspend fun recordAiQuestion(subject: String) {
        val ref = statisticsRef()

        val normalizedSubject = subject.trim()
            .ifEmpty { "Other" }

        db.runTransaction { transaction ->

            val snapshot = transaction.get(ref)

            val uid = currentUid()

            val userRef = db.collection("users")
                .document(uid)
            val oldTotal =
                snapshot.getLong("totalQuestionsAsked")?.toInt() ?: 0

            val oldSubjects =
                snapshot.get("subjectCounts") as? Map<*, *>
                    ?: emptyMap<Any, Any>()

            val updatedSubjects = mutableMapOf<String, Int>()

            oldSubjects.forEach { (key, value) ->
                if (key is String && value is Number) {
                    updatedSubjects[key] = value.toInt()
                }
            }

            updatedSubjects[normalizedSubject] =
                (updatedSubjects[normalizedSubject] ?: 0) + 1

            val updates = mutableMapOf<String, Any>(
                "totalQuestionsAsked" to oldTotal + 1,
                "subjectCounts" to updatedSubjects
            )

            updateDailyActivity(
                snapshot = snapshot,
                updates = updates,
                transaction = transaction,
                userRef = userRef
            )

            transaction.set(
                ref,
                updates,
                SetOptions.merge()
            )

            null
        }.await()

        // Check AI achievements and streak achievements
        checkAchievements()
    }

    // 2. Record completed quiz
    suspend fun recordQuizResult(
        correctAnswers: Int,
        totalQuestions: Int
    ) {
        require(totalQuestions >= 0) {
            "Total questions cannot be negative"
        }

        require(correctAnswers in 0..totalQuestions) {
            "Correct answers must be between 0 and total questions"
        }

        val ref = statisticsRef()

        db.runTransaction { transaction ->

            val snapshot = transaction.get(ref)
            val uid = currentUid()

            val userRef = db.collection("users")
                .document(uid)
            val oldAttempts =
                snapshot.getLong("totalQuizAttempts")?.toInt() ?: 0

            val oldCorrect =
                snapshot.getLong("quizCorrectTotal")?.toInt() ?: 0

            val oldAnswered =
                snapshot.getLong("quizAnsweredTotal")?.toInt() ?: 0

            val newAttempts = oldAttempts + 1
            val newCorrect = oldCorrect + correctAnswers
            val newAnswered = oldAnswered + totalQuestions

            val accuracy = if (newAnswered > 0) {
                newCorrect.toDouble() / newAnswered * 100.0
            } else {
                0.0
            }

            val updates = mutableMapOf<String, Any>(
                "totalQuizAttempts" to newAttempts,
                "quizCorrectTotal" to newCorrect,
                "quizAnsweredTotal" to newAnswered,
                "currentAccuracy" to accuracy
            )

            updateDailyActivity(
                snapshot = snapshot,
                updates = updates,
                transaction = transaction,
                userRef = userRef
            )

            transaction.set(
                ref,
                updates,
                SetOptions.merge()
            )

            null
        }.await()

        // Check quiz achievements and streak achievements
        checkAchievements()
    }

    // 3. Record daily activity
    suspend fun recordDailyActivity() {
        val ref = statisticsRef()

        db.runTransaction { transaction ->

            val snapshot = transaction.get(ref)

            val uid = currentUid()

            val userRef = db.collection("users")
                .document(uid)
            val updates = mutableMapOf<String, Any>()

            updateDailyActivity(
                snapshot = snapshot,
                updates = updates,
                transaction = transaction,
                userRef = userRef
            )

            transaction.set(
                ref,
                updates,
                SetOptions.merge()
            )

            null
        }.await()

        // Check streak achievements
        checkAchievements()
    }

    // 4. Record review time
    suspend fun recordReviewTime(seconds: Int) {
        require(seconds >= 0) {
            "Review time cannot be negative"
        }

        val ref = statisticsRef()

        db.runTransaction { transaction ->

            val snapshot = transaction.get(ref)

            val uid = currentUid()

            val userRef = db.collection("users")
                .document(uid)
            val oldTime =
                snapshot.getLong("reviewTimeSeconds") ?: 0L

            val updates = mutableMapOf<String, Any>(
                "reviewTimeSeconds" to oldTime + seconds
            )

            updateDailyActivity(
                snapshot = snapshot,
                updates = updates,
                transaction = transaction,
                userRef = userRef
            )

            transaction.set(
                ref,
                updates,
                SetOptions.merge()
            )

            null
        }.await()

        checkAchievements()
    }

    // 5. Add XP

    // 5. Add XP and sync leaderboard data
    suspend fun addXp(xp: Long) {
        require(xp > 0) {
            "XP must be greater than 0"
        }

        val uid = currentUid()

        val userRef = db.collection("users")
            .document(uid)

        val statsRef = userRef.collection("statistics")
            .document("summary")

        db.runTransaction { transaction ->

            // Read both documents before writing
            val statsSnapshot = transaction.get(statsRef)
            transaction.get(userRef)

            val oldXp = statsSnapshot.getLong("totalXp") ?: 0L
            val newXp = Math.addExact(oldXp, xp)

            val newLevel = calculateLevel(newXp)

            val updates = mutableMapOf<String, Any>(
                "totalXp" to newXp,
                "level" to newLevel
            )

            updateDailyActivity(
                snapshot = statsSnapshot,
                updates = updates,
                transaction = transaction,
                userRef = userRef
            )

            // Update statistics/summary
            transaction.set(
                statsRef,
                updates,
                SetOptions.merge()
            )

            // Sync XP and Level to users/{uid}


            null
        }.await()

        // Check XP, level and streak achievements
        checkAchievements()
    }


    // LEVEL CALCULATION

    private fun calculateLevel(xp: Long): Int {
        if (xp < 0L) return 1

        val value = xp / 25.0

        val level = (
                (-1.0 + sqrt(9.0 + 4.0 * value)) / 2.0
                ).toInt()

        return maxOf(1, level)
    }

    fun getXpRequiredForLevel(level: Int): Long {
        require(level >= 1) {
            "Level must be at least 1"
        }

        val n = level.toLong() - 1L

        return 25L * n * (n + 3L)
    }

    fun calculateProgress(xp: Long): Float {
        if (xp < 0L) return 0f

        val level = calculateLevel(xp)

        val currentLevelXp = getXpRequiredForLevel(level)
        val nextLevelXp = getXpRequiredForLevel(level + 1)

        val progressXp = xp - currentLevelXp
        val requiredXp = nextLevelXp - currentLevelXp

        if (requiredXp <= 0L) return 1f

        return (
                progressXp.toFloat() / requiredXp.toFloat()
                ).coerceIn(0f, 1f)
    }
}
