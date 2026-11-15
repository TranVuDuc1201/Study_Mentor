
package com.example.study_mentor.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeParseException

class UserStatisticsRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Get statistics document of current user
    private fun statisticsRef(): DocumentReference {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User is not logged in")

        return db.collection("users")
            .document(uid)
            .collection("statistics")
            .document("summary")
    }

    // Update daily activity inside a Firestore transaction
    private fun updateDailyActivity(
        snapshot: com.google.firebase.firestore.DocumentSnapshot,
        updates: MutableMap<String, Any>
    ) {
        val today = LocalDate.now()

        val lastActiveDate =
            snapshot.getString("lastActiveDate") ?: ""

        val oldStreak =
            snapshot.getLong("dayStreak")?.toInt() ?: 0

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

        updates["dayStreak"] = newStreak
        updates["longestStreak"] = maxOf(
            oldLongestStreak,
            newStreak
        )
        updates["lastActiveDate"] = today.toString()
        updates["updatedAt"] = FieldValue.serverTimestamp()
    }

    // 1. Record a question asked to AI
    suspend fun recordAiQuestion(subject: String) {
        val ref = statisticsRef()

        val normalizedSubject = subject.trim()
            .ifEmpty { "Other" }

        db.runTransaction { transaction ->

            val snapshot = transaction.get(ref)

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

            updateDailyActivity(snapshot, updates)

            transaction.set(
                ref,
                updates,
                SetOptions.merge()
            )

            null
        }.await()
    }

    // 2. Record a completed quiz
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

            updateDailyActivity(snapshot, updates)

            transaction.set(
                ref,
                updates,
                SetOptions.merge()
            )

            null
        }.await()
    }

    // 3. Record daily activity
    suspend fun recordDailyActivity() {
        val ref = statisticsRef()

        db.runTransaction { transaction ->

            val snapshot = transaction.get(ref)

            val updates = mutableMapOf<String, Any>()

            updateDailyActivity(snapshot, updates)

            transaction.set(
                ref,
                updates,
                SetOptions.merge()
            )

            null
        }.await()
    }

    // 4. Record review time in seconds
    suspend fun recordReviewTime(seconds: Int) {
        require(seconds >= 0) {
            "Review time cannot be negative"
        }

        val ref = statisticsRef()

        db.runTransaction { transaction ->

            val snapshot = transaction.get(ref)

            val oldTime =
                snapshot.getLong("reviewTimeSeconds") ?: 0L

            val updates = mutableMapOf<String, Any>(
                "reviewTimeSeconds" to oldTime + seconds
            )

            updateDailyActivity(snapshot, updates)

            transaction.set(
                ref,
                updates,
                SetOptions.merge()
            )

            null
        }.await()
    }
}
