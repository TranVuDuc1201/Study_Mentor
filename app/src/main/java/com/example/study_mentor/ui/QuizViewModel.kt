package com.example.study_mentor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_mentor.data.ChatSession
import com.example.study_mentor.data.Quiz
import com.example.study_mentor.data.QuizChatSessionOption
import com.example.study_mentor.data.QuizGenerator
import com.example.study_mentor.data.QuizRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizUiState(
    val quizzes: List<Quiz> = emptyList(),
    val chatSessions: List<QuizChatSessionOption> = emptyList(),
    val isLoadingSessions: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null
)

class QuizViewModel : ViewModel() {

    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private var observeJob: Job? = null

    fun observe(uid: String?) {
        observeJob?.cancel()

        if (uid == null) {
            _state.value = QuizUiState()
            return
        }

        observeJob = viewModelScope.launch {
            QuizRepository.observeQuizzes(uid)
                .catch { e ->
                    _state.update {
                        it.copy(error = e.localizedMessage ?: "Could not load quizzes.")
                    }
                }
                .collect { list ->
                    _state.update { it.copy(quizzes = list) }
                }
        }
    }

    /** Loads chat sessions to show in the Create Quiz dialog. */
    fun loadChatSessions(uid: String?) {
        if (uid == null || _state.value.isLoadingSessions) return

        viewModelScope.launch {
            _state.update {
                it.copy(isLoadingSessions = true, error = null)
            }

            try {
                val sessions = QuizGenerator.loadChatSessions(uid)
                _state.update { it.copy(chatSessions = sessions) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Could not load conversations.",
                        chatSessions = emptyList()
                    )
                }
            } finally {
                _state.update { it.copy(isLoadingSessions = false) }
            }
        }
    }

    /** Creates a quiz from either a topic or one selected chat session. */
    fun generateQuiz(
        uid: String?,
        topic: String? = null,
        sessionId: String? = null
    ) {
        if (uid == null || _state.value.isGenerating) return

        val cleanTopic = topic?.trim().orEmpty()
        val cleanSessionId = sessionId?.trim().orEmpty()

        if (cleanTopic.isBlank() && cleanSessionId.isBlank()) {
            _state.update {
                it.copy(error = "Enter a topic or select a conversation.")
            }
            return
        }

        if (cleanTopic.isNotBlank() && cleanSessionId.isNotBlank()) {
            _state.update {
                it.copy(error = "Please choose only one quiz source.")
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true, error = null) }

            try {
                val quiz = QuizGenerator.generate(
                    uid = uid,
                    topic = cleanTopic.takeIf { it.isNotBlank() },
                    sessionId = cleanSessionId.takeIf { it.isNotBlank() }
                )
                QuizRepository.saveQuiz(uid, quiz)
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message ?: "Could not generate a quiz.")
                }
            } finally {
                _state.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
