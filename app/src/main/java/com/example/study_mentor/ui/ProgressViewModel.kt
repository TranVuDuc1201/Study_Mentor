
package com.example.study_mentor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_mentor.data.ProgressRepository
import com.example.study_mentor.data.ProgressSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    private var chatSessions: List<ProgressSession> = emptyList()

    fun observeProgress(uid: String?) {
        chatJob?.cancel()
        chatSessions = emptyList()

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
                    _state.value = ProgressUiState(
                        isLoading = false,
                        error = exception.localizedMessage
                            ?: "Unable to load AI chat progress"
                    )
                }
                .collect { sessions ->
                    chatSessions = sessions
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

        _state.value = ProgressUiState(
            isLoading = false,
            studySessions = chatSessions.size,
            studyStreak = streak,
            weeklyActivity = activity,

            // Quiz statistics will be integrated
            // when the Backend Quiz API is available.
            quizzesDone = null,
            completion = null,
            subjectProgress = emptyList(),

            error = null
        )
    }

    override fun onCleared() {
        chatJob?.cancel()
        super.onCleared()
    }
}
