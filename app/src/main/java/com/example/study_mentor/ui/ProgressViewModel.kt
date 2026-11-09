
package com.example.study_mentor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_mentor.data.ProgressRepository
import com.example.study_mentor.data.ProgressSession
import com.example.study_mentor.data.QuizAttempt
import com.example.study_mentor.data.QuizProgressRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class SubjectProgress(
    val subject: String,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val percentage: Int
)

data class ProgressUiState(
    val isLoading: Boolean = true,
    val studySessions: Int? = null,
    val studyStreak: Int? = null,
    val weeklyActivity: List<Int> = List(7) { 0 },
    val quizzesDone: Int? = null,
    val completion: Int? = null,
    val subjectProgress: List<SubjectProgress> = emptyList(),
    val error: String? = null
)

class ProgressViewModel(
    private val progressRepository: ProgressRepository =
        ProgressRepository(),
    private val quizRepository: QuizProgressRepository =
        QuizProgressRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = _state.asStateFlow()

    private var chatJob: Job? = null
    private var quizJob: Job? = null

    private var chatSessions: List<ProgressSession> = emptyList()
    private var quizAttempts: List<QuizAttempt> = emptyList()

    private var chatLoaded = false
    private var quizLoaded = false
    private var chatError: String? = null
    private var quizError: String? = null

    fun observeProgress(uid: String?) {
        chatJob?.cancel()
        quizJob?.cancel()

        chatSessions = emptyList()
        quizAttempts = emptyList()
        chatLoaded = false
        quizLoaded = false
        chatError = null
        quizError = null

        if (uid.isNullOrBlank()) {
            _state.value = ProgressUiState(
                isLoading = false,
                error = "Sign in to view your learning progress"
            )
            return
        }

        _state.value = ProgressUiState(isLoading = true)

        chatJob = viewModelScope.launch {
            progressRepository.observeStudySessions(uid)
                .catch { exception ->
                    chatError = exception.localizedMessage
                        ?: "Unable to load AI chat progress"
                    chatLoaded = true
                    updateProgress()
                }
                .collect { sessions ->
                    chatSessions = sessions
                    chatLoaded = true
                    updateProgress()
                }
        }

        quizJob = viewModelScope.launch {
            quizRepository.observeQuizAttempts(uid)
                .catch { exception ->
                    quizError = exception.localizedMessage
                        ?: "Unable to load quiz progress"
                    quizLoaded = true
                    updateProgress()
                }
                .collect { attempts ->
                    quizAttempts = attempts
                    quizLoaded = true
                    updateProgress()
                }
        }
    }

    private fun updateProgress() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val sessionDates = chatSessions.map {
            Instant.ofEpochMilli(it.createdAt)
                .atZone(zone)
                .toLocalDate()
        }

        val activity = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            sessionDates.count { it == date }
        }

        val activeDates = sessionDates.toSet()
        var streak = 0
        var day = today

        if (day !in activeDates) {
            day = day.minusDays(1)
        }

        while (day in activeDates) {
            streak++
            day = day.minusDays(1)
        }

        val totalCorrect = quizAttempts.sumOf { it.score }
        val totalQuestions = quizAttempts.sumOf {
            it.totalQuestions
        }

        val completion = if (totalQuestions > 0) {
            (totalCorrect * 100.0 / totalQuestions)
                .roundToInt()
        } else {
            null
        }

        val subjects = quizAttempts
            .groupBy {
                it.subject.ifBlank { "Other" }
            }
            .map { (subject, attempts) ->
                val correct = attempts.sumOf { it.score }
                val total = attempts.sumOf {
                    it.totalQuestions
                }

                SubjectProgress(
                    subject = subject,
                    correctAnswers = correct,
                    totalQuestions = total,
                    percentage = if (total > 0) {
                        (correct * 100.0 / total).roundToInt()
                    } else {
                        0
                    }
                )
            }
            .sortedBy { it.subject }

        _state.value = ProgressUiState(
            isLoading = !chatLoaded || !quizLoaded,
            studySessions = if (chatLoaded && chatError == null) {
                chatSessions.size
            } else {
                null
            },
            studyStreak = if (chatLoaded && chatError == null) {
                streak
            } else {
                null
            },
            weeklyActivity = activity,
            quizzesDone = if (quizLoaded && quizError == null) {
                quizAttempts.size
            } else {
                null
            },
            completion = if (quizLoaded && quizError == null) {
                completion
            } else {
                null
            },
            subjectProgress = if (quizLoaded && quizError == null) {
                subjects
            } else {
                emptyList()
            },
            error = listOfNotNull(chatError, quizError)
                .joinToString("\n")
                .ifBlank { null }
        )
    }
}
