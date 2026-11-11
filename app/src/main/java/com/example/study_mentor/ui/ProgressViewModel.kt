
package com.example.study_mentor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_mentor.data.ProgressRepository
import com.example.study_mentor.data.ProgressSession
import com.example.study_mentor.data.QuizProgressRecord
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
        ProgressRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = _state.asStateFlow()

    private var chatJob: Job? = null
    private var quizJob: Job? = null

    private var chatSessions: List<ProgressSession> = emptyList()

    private var chatLoaded = false
    private var quizLoaded = false

    private var chatError: String? = null
    private var quizError: String? = null

    fun observeProgress(uid: String?) {

        chatJob?.cancel()
        quizJob?.cancel()

        chatSessions = emptyList()

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

        // Observe AI Chat progress
        chatJob = viewModelScope.launch {

            progressRepository.observeStudySessions(uid)
                .catch { exception ->

                    chatLoaded = true
                    chatError = exception.localizedMessage
                        ?: "Unable to load AI chat progress"

                    updateLoadingState()
                }
                .collect { sessions ->

                    chatSessions = sessions
                    chatLoaded = true
                    chatError = null

                    updateChatProgress()
                    updateLoadingState()
                }
        }

        // Observe real Quiz results from Firestore
        quizJob = viewModelScope.launch {

            progressRepository.observeQuizResults(uid)
                .catch { exception ->

                    quizLoaded = true
                    quizError = exception.localizedMessage
                        ?: "Unable to load quiz progress"

                    updateLoadingState()
                }
                .collect { results ->

                    quizLoaded = true
                    quizError = null

                    updateQuizProgress(results)
                    updateLoadingState()
                }
        }
    }

    /**
     * Calculate AI Chat sessions, streak and weekly activity.
     */
    private fun updateChatProgress() {

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

        _state.value = _state.value.copy(
            studySessions = chatSessions.size,
            studyStreak = streak,
            weeklyActivity = activity
        )
    }

    /**
     * Calculate Quiz statistics from actual saved quiz results.
     */
    private fun updateQuizProgress(
        results: List<QuizProgressRecord>
    ) {

        val totalCorrect = results.sumOf {
            it.score
        }

        val totalQuestions = results.sumOf {
            it.totalQuestions
        }

        // Weighted accuracy across all completed quizzes.
        val accuracy = if (totalQuestions > 0) {
            (totalCorrect * 100.0 / totalQuestions)
                .roundToInt()
        } else {
            null
        }

        // Calculate progress separately for each subject.
        val subjects = results
            .groupBy { it.subject }
            .map { (subject, attempts) ->

                val correct = attempts.sumOf {
                    it.score
                }

                val total = attempts.sumOf {
                    it.totalQuestions
                }

                SubjectProgress(
                    subject = subject,
                    correctAnswers = correct,
                    totalQuestions = total,
                    percentage = if (total > 0) {
                        (correct * 100.0 / total)
                            .roundToInt()
                    } else {
                        0
                    }
                )
            }
            .sortedBy { it.subject }

        _state.value = _state.value.copy(
            quizzesDone = results.size,
            completion = accuracy,
            subjectProgress = subjects
        )
    }

    /**
     * Update loading and error states from both data sources.
     */
    private fun updateLoadingState() {

        val errors = listOfNotNull(
            chatError,
            quizError
        ).joinToString("\n").ifBlank {
            null
        }

        _state.value = _state.value.copy(
            isLoading = !chatLoaded || !quizLoaded,
            error = errors
        )
    }

    override fun onCleared() {
        chatJob?.cancel()
        quizJob?.cancel()
        super.onCleared()
    }
}
