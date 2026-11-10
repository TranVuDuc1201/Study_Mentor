package com.example.study_mentor.data
import com.google.firebase.Timestamp
data class UserStatistics(
    val totalQuestionsAsked: Int = 0,

    val subjectCounts: Map<String, Int> = emptyMap(),

    val totalQuizAttempts: Int = 0,
    val quizCorrectTotal: Int = 0,
    val quizAnsweredTotal: Int = 0,
    val currentAccuracy: Double = 0.0,

    val dayStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String = "",

    val reviewTimeSeconds: Int = 0,

    val updatedAt: Timestamp? = null
)