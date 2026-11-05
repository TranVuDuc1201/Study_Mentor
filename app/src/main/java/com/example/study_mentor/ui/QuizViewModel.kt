package com.example.study_mentor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_mentor.OpenAIApiClient
import com.example.study_mentor.data.Question
import com.example.study_mentor.data.QuizItem
import com.example.study_mentor.data.QuizResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class QuizViewModel : ViewModel() {

    private val sampleQuizzes = mapOf(
        "Quadratic Equations" to QuizItem(
            id = "1",
            title = "Quadratic Equations",
            subject = "Math",
            difficulty = "Medium",
            subtitle = "3 questions • +150 XP",
            questions = listOf(
                Question(
                    id = 1,
                    questionText = "What is the discriminant of the quadratic equation ax² + bx + c = 0?",
                    options = listOf("b² - 4ac", "b² + 4ac", "2ax + b", "-b / 2a"),
                    correctOptionIndex = 0
                ),
                Question(
                    id = 2,
                    questionText = "Find the roots of x² - 9 = 0",
                    options = listOf("3, -3", "9, -9", "0, 3", "No real roots"),
                    correctOptionIndex = 0
                ),
                Question(
                    id = 3,
                    questionText = "What is the vertex form of a quadratic equation?",
                    options = listOf("y = ax² + bx + c", "y = a(x - h)² + k", "y = mx + b", "ax + by = c"),
                    correctOptionIndex = 1
                )
            )
        ),
        "Cellular Respiration" to QuizItem(
            id = "2",
            title = "Cellular Respiration",
            subject = "Biology",
            difficulty = "Hard",
            subtitle = "3 questions • +200 XP",
            questions = listOf(
                Question(
                    id = 1,
                    questionText = "Where does glycolysis take place in the cell?",
                    options = listOf("Mitochondrial matrix", "Cytoplasm / Cytosol", "Nucleus", "Ribosome"),
                    correctOptionIndex = 1
                ),
                Question(
                    id = 2,
                    questionText = "What is the primary energy currency produced by cellular respiration?",
                    options = listOf("DNA", "ATP", "RNA", "NADH"),
                    correctOptionIndex = 1
                ),
                Question(
                    id = 3,
                    questionText = "Which molecule enters the Krebs cycle?",
                    options = listOf("Pyruvate", "Acetyl-CoA", "Glucose", "Lactic acid"),
                    correctOptionIndex = 1
                )
            )
        )
    )

    private val _currentQuiz = MutableStateFlow<QuizItem?>(null)
    val currentQuiz: StateFlow<QuizItem?> = _currentQuiz.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _selectedAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val selectedAnswers: StateFlow<Map<Int, Int>> = _selectedAnswers.asStateFlow()

    private val _quizResult = MutableStateFlow<QuizResult?>(null)
    val quizResult: StateFlow<QuizResult?> = _quizResult.asStateFlow()

    fun selectQuiz(title: String) {
        val fallbackQuiz = sampleQuizzes[title] ?: sampleQuizzes.values.first()
        _currentQuiz.value = fallbackQuiz
        _currentQuestionIndex.value = 0
        _selectedAnswers.value = emptyMap()
        _quizResult.value = null

        // Fetch AI generated questions in background
        fetchAiQuiz(title, fallbackQuiz)
    }

    private fun fetchAiQuiz(title: String, fallbackQuiz: QuizItem) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val prompt = """
                    Generate exactly 3 multiple-choice questions for the study topic: "$title".
                    You MUST respond ONLY with a raw JSON array of objects. No markdown formatting, no code blocks, no extra text.
                    Each object must have these exact keys:
                    "question": (string)
                    "options": (array of 4 strings)
                    "correctIndex": (integer between 0 and 3)
                """.trimIndent()

                var fullResponse = ""
                OpenAIApiClient.askQuestion(prompt = prompt, maxTokens = 1000).collect { chunk ->
                    fullResponse += chunk
                }

                val cleanedJson = fullResponse.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val jsonArray = JSONArray(cleanedJson)
                val aiQuestions = mutableListOf<Question>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val qText = obj.getString("question")
                    val optArray = obj.getJSONArray("options")
                    val options = mutableListOf<String>()
                    for (j in 0 until optArray.length()) {
                        options.add(optArray.getString(j))
                    }
                    val correctIndex = obj.getInt("correctIndex")

                    aiQuestions.add(
                        Question(
                            id = i + 1,
                            questionText = qText,
                            options = options,
                            correctOptionIndex = correctIndex
                        )
                    )
                }

                if (aiQuestions.isNotEmpty()) {
                    _currentQuiz.update { current ->
                        current?.copy(questions = aiQuestions)
                    }
                }
            } catch (e: Exception) {
                // Keep fallback quiz if AI generation or parsing fails
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        _selectedAnswers.update { it + (questionIndex to optionIndex) }
    }

    fun nextQuestion() {
        val quiz = _currentQuiz.value ?: return
        if (_currentQuestionIndex.value < quiz.questions.size - 1) {
            _currentQuestionIndex.value += 1
        }
    }

    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value -= 1
        }
    }

    fun submitQuiz(): QuizResult {
        val quiz = _currentQuiz.value
        val answers = _selectedAnswers.value
        var score = 0
        quiz?.questions?.forEachIndexed { index, question ->
            if (answers[index] == question.correctOptionIndex) {
                score++
            }
        }
        val total = quiz?.questions?.size ?: 0
        val xp = score * 50
        val result = QuizResult(
            quizTitle = quiz?.title ?: "Quiz",
            score = score,
            totalQuestions = total,
            xpEarned = xp
        )
        _quizResult.value = result
        return result
    }
}
