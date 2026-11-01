package com.example.study_mentor.ui

/** Pure form rules: usable by both Compose screens and local JVM tests. */
object AuthValidation {
    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Email is required"
        !emailPattern.matches(email.trim()) -> "Enter a valid email address"
        else -> null
    }

    fun nameError(name: String): String? = when {
        name.isBlank() -> "Name is required"
        name.trim().length < 2 -> "Name must contain at least 2 characters"
        name.trim().length > 60 -> "Name must be at most 60 characters"
        else -> null
    }

    fun loginPasswordError(password: String): String? = when {
        password.isEmpty() -> "Password is required"
        else -> null
    }

    fun registrationPasswordError(password: String): String? = when {
        password.length < 8 -> "Use at least 8 characters"
        password.none(Char::isLetter) -> "Include at least one letter"
        password.none(Char::isDigit) -> "Include at least one number"
        else -> null
    }

    fun confirmPasswordError(password: String, confirmation: String): String? = when {
        confirmation.isEmpty() -> "Confirm your password"
        confirmation != password -> "Passwords do not match"
        else -> null
    }

    fun passwordStrength(password: String): Int = listOf(
        password.length >= 8,
        password.any(Char::isLetter),
        password.any(Char::isDigit),
        password.any { !it.isLetterOrDigit() }
    ).count { it }
}
