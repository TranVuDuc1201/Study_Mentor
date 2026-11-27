package com.example.study_mentor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_mentor.data.Quiz
import com.example.study_mentor.data.QuizChatSessionOption
import com.example.study_mentor.data.QuizGenerator
import com.example.study_mentor.data.QuizRepository
import com.example.study_mentor.data.PowerUpType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val error: String? = null,

    // Quiz Taking & Feedback State
    val currentQuestionIndex: Int = 0,
    val selectedAnswerIndex: Int = -1,
    val isAnswerEvaluated: Boolean = false,
    val feedbackMessage: String? = null,
    val showFeedback: Boolean = false,

    // Power-up State (Remove 1 Wrong Option)
    val removeWrongCount: Int = 0,
    val eliminatedOptionIndices: Set<Int> = emptySet(),
    val explodingOptionIndex: Int? = null
)

class QuizViewModel : ViewModel() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private var observeJob: Job? = null
    private var userPowerUpListener: ListenerRegistration? = null
    private var currentUid: String? = null

    override fun onCleared() {
        super.onCleared()
        userPowerUpListener?.remove()
    }

    fun observe(uid: String?) {
        observeJob?.cancel()
        userPowerUpListener?.remove()
        userPowerUpListener = null
        currentUid = uid

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

        userPowerUpListener = db.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val powerUpsMap = snapshot.get("powerUps") as? Map<*, *>
                val countFromMap = (powerUpsMap?.get(PowerUpType.REMOVE_ONE_WRONG.id) as? Number)?.toInt()
                val countFallback = (snapshot.get(PowerUpType.REMOVE_ONE_WRONG.id) as? Number)?.toInt()
                val resolvedCount = (countFromMap ?: countFallback ?: 0).coerceAtLeast(0)

                _state.update { it.copy(removeWrongCount = resolvedCount) }
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
    /** Creates a quiz from either a topic or one selected chat session. */
    fun generateQuiz(
        uid: String?,
        topic: String? = null,
        sessionId: String? = null,
        questionCount: Int = 5,
        allowedTypes: Set<String> = QuizGenerator.ALLOWED_QUESTION_TYPES,
        difficulty: String = "medium"
    ) {
        if (uid == null || _state.value.isGenerating) return

        val cleanTopic = topic?.trim().orEmpty()
        val cleanSessionId = sessionId?.trim().orEmpty()
        val cleanQuestionCount = questionCount.coerceIn(1, 15)
        val cleanTypes = allowedTypes
            .map { it.trim() }
            .filter { it in QuizGenerator.ALLOWED_QUESTION_TYPES }
            .toSet()
            .ifEmpty { QuizGenerator.ALLOWED_QUESTION_TYPES }

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
                    sessionId = cleanSessionId.takeIf { it.isNotBlank() },
                    questionCount = cleanQuestionCount,
                    allowedTypes = cleanTypes,
                    difficulty = difficulty
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

    // ============================================
    // QUIZ TAKING & FEEDBACK STATE MANAGEMENT
    // ============================================

    fun selectAnswer(index: Int) {
        val current = _state.value
        if (index in current.eliminatedOptionIndices || index == current.explodingOptionIndex) {
            return
        }
        _state.update {
            it.copy(selectedAnswerIndex = index)
        }
    }

    fun evaluateAnswer(isCorrect: Boolean, correctAnswer: String) {
        val message = if (isCorrect) {
            "Correct! 🎉"
        } else {
            if (correctAnswer.isNotBlank()) "Incorrect answer. Correct answer: $correctAnswer" else "Incorrect answer."
        }
        _state.update {
            it.copy(
                isAnswerEvaluated = true,
                feedbackMessage = message,
                showFeedback = true
            )
        }
    }

    /**
     * Removes one incorrect option for the current question, decrements the persistent
     * power-up count in Firestore, and plays a short bomb animation.
     */
    fun useRemoveOneWrongOption(options: List<String>, correctAnswer: String) {
        val uid = currentUid ?: return
        val current = _state.value

        if (current.isAnswerEvaluated || current.removeWrongCount <= 0 || current.explodingOptionIndex != null) {
            return
        }

        val correctIndex = options.indexOfFirst { it.trim().equals(correctAnswer.trim(), ignoreCase = true) }
        val remainingOptionsCount = options.size - current.eliminatedOptionIndices.size
        // Giữ lại ít nhất 1 đáp án sai để người dùng vẫn có lựa chọn nếu chưa chọn đúng
        if (remainingOptionsCount <= 2) return

        val candidateWrongIndices = options.indices.filter { idx ->
            idx != correctIndex && idx !in current.eliminatedOptionIndices
        }
        val targetIndex = candidateWrongIndices.randomOrNull() ?: return

        // Nếu người dùng đang chọn tạm đúng vào đáp án sắp bị xóa thì bỏ chọn
        val updatedSelectedIndex = if (current.selectedAnswerIndex == targetIndex) -1 else current.selectedAnswerIndex

        _state.update {
            it.copy(
                explodingOptionIndex = targetIndex,
                selectedAnswerIndex = updatedSelectedIndex
            )
        }

        db.collection("users")
            .document(uid)
            .update("powerUps.${PowerUpType.REMOVE_ONE_WRONG.id}", FieldValue.increment(-1L))
            .addOnFailureListener { e ->
                _state.update {
                    it.copy(
                        explodingOptionIndex = null,
                        error = e.localizedMessage ?: "Could not use power-up."
                    )
                }
            }

        viewModelScope.launch {
            delay(450L)
            _state.update { stateNow ->
                if (stateNow.explodingOptionIndex == targetIndex) {
                    stateNow.copy(
                        explodingOptionIndex = null,
                        eliminatedOptionIndices = stateNow.eliminatedOptionIndices + targetIndex
                    )
                } else {
                    stateNow
                }
            }
        }
    }

    /**
     * Advances to the next question and explicitly resets all feedback & question power-up states
     * so that the new question starts completely fresh.
     */
    fun nextQuestion(totalQuestions: Int) {
        _state.update { current ->
            val nextIndex = (current.currentQuestionIndex + 1).coerceAtMost(totalQuestions - 1)
            current.copy(
                currentQuestionIndex = nextIndex,
                selectedAnswerIndex = -1,
                isAnswerEvaluated = false,
                feedbackMessage = null,
                showFeedback = false,
                eliminatedOptionIndices = emptySet(),
                explodingOptionIndex = null
            )
        }
    }

    /**
     * Returns to the previous question and resets lingering feedback & question power-up states.
     */
    fun previousQuestion() {
        _state.update { current ->
            val prevIndex = (current.currentQuestionIndex - 1).coerceAtLeast(0)
            current.copy(
                currentQuestionIndex = prevIndex,
                selectedAnswerIndex = -1,
                isAnswerEvaluated = false,
                feedbackMessage = null,
                showFeedback = false,
                eliminatedOptionIndices = emptySet(),
                explodingOptionIndex = null
            )
        }
    }

    /**
     * Resets all quiz-taking and feedback state back to defaults.
     */
    fun resetQuizTakingState() {
        _state.update {
            it.copy(
                currentQuestionIndex = 0,
                selectedAnswerIndex = -1,
                isAnswerEvaluated = false,
                feedbackMessage = null,
                showFeedback = false,
                eliminatedOptionIndices = emptySet(),
                explodingOptionIndex = null
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}