package com.example.study_mentor.data

data class Question(
    val id: Int,
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int
)

data class QuizItem(
    val id: String,
    val title: String,
    val subject: String,
    val difficulty: String,
    val subtitle: String,
    val questions: List<Question>
)

data class QuizResult(
    val quizTitle: String,
    val score: Int,
    val totalQuestions: Int,
    val xpEarned: Int
)
