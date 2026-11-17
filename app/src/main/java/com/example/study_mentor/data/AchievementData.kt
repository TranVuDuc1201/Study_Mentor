package com.example.study_mentor.data




data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val requirement: String,
    val rewardXp: Int
)



data class AchievementProgress(
    val totalQuestionsAsked: Int = 0,
    val totalQuizAttempts: Int = 0,
    val quizCorrectTotal: Int = 0,
    val dayStreak: Int = 0,
    val totalXp: Long = 0L,
    val level: Int = 1
)

object AchievementDefinitions {

    val achievements = listOf(

        Achievement(
            "first_question",
            "Curious Mind",
            "Ask your first question to AI",
            "1 AI question",
            10
        ),

        Achievement(
            "first_quiz",
            "First Step",
            "Complete your first quiz",
            "1 quiz",
            10
        ),

        Achievement(
            "quiz_10",
            "Quiz Beginner",
            "Complete 10 quizzes",
            "10 quizzes",
            30
        ),

        Achievement(
            "quiz_2",
            "Quiz Beginner",
            "Complete 2 quizzes",
            "2 quizzes",
            30
        ),

        Achievement(
            "quiz_50",
            "Quiz Master",
            "Complete 50 quizzes",
            "50 quizzes",
            100
        ),

        Achievement(
            "correct_10",
            "Perfect Start",
            "Answer 10 questions correctly",
            "10 correct answers",
            30
        ),

        Achievement(
            "questions_50",
            "Knowledge Seeker",
            "Ask AI 50 questions",
            "50 AI questions",
            50
        ),

        Achievement(
            "streak_3",
            "Consistency",
            "Study for 3 consecutive days",
            "3 day streak",
            30
        ),

        Achievement(
            "streak_7",
            "Dedicated Learner",
            "Study for 7 consecutive days",
            "7 day streak",
            70
        ),

        Achievement(
            "xp_100",
            "Rising Star",
            "Earn 100 XP",
            "100 XP",
            30
        ),

        Achievement(
            "level_5",
            "Advanced Learner",
            "Reach level 5",
            "Level 5",
            100
        ),

        Achievement(
                "level_10",
        "Advanced Learner",
        "Reach level 10",
        "Level 10",
        100
    ),

        Achievement(
                "level_15",
        "Advanced Learner",
        "Reach level 15",
        "Level 15",
        100
    )
    )
}
