package com.example.study_mentor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.study_mentor.OpenAIApiClient
import com.example.study_mentor.data.Quiz
import com.example.study_mentor.ui.theme.BackgroundOffWhite
import com.example.study_mentor.ui.theme.PrimaryBlue
import com.example.study_mentor.ui.theme.SecondaryGreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun QuizResultScreen(
    quizTitle: String,
    score: Int,
    totalQuestions: Int,
    xpEarned: Int,
    quiz: Quiz?,
    selectedAnswers: List<String?>,
    onFinish: () -> Unit
) {
    val percentage = if (totalQuestions > 0) {
        (score.toFloat() / totalQuestions * 100).toInt()
    } else {
        0
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundOffWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Quiz completed",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Text(
                    text = "Quiz Completed!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = quizTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {

                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            ResultValue(
                                label = "Score",
                                value = "$score/$totalQuestions",
                                valueColor = PrimaryBlue
                            )

                            ResultValue(
                                label = "XP Earned",
                                value = "+$xpEarned",
                                valueColor = SecondaryGreen
                            )
                        }
                    }
                }
            }

            if (quiz != null && quiz.items.isNotEmpty()) {
                if (selectedAnswers.size == quiz.items.size) {
                    InlineQuizAIReview(quiz = quiz, selectedAnswers = selectedAnswers)
                } else {
                    Text(
                        text = "Không có dữ liệu đáp án để xem lại.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Back to Practice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ResultValue(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

// Review uses the same answer encoding as QuizTakingScreen:
// choice = option index as String; short/blank = typed text.
@Composable
private fun InlineQuizAIReview(
    quiz: Quiz,
    selectedAnswers: List<String?>
) {
    val scope = rememberCoroutineScope()
    val aiExplanations = remember(quiz.quizId) { mutableStateMapOf<Int, String>() }
    val aiLoading = remember(quiz.quizId) { mutableStateMapOf<Int, Boolean>() }
    val aiErrors = remember(quiz.quizId) { mutableStateMapOf<Int, String>() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Xem lại đáp án và AI giải thích",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        quiz.items.forEachIndexed { index, question ->
            val rawAnswer = selectedAnswers.getOrNull(index)?.trim().orEmpty()
            val isCorrect = when (question.type) {
                "multiple_choice" -> rawAnswer.toIntOrNull() == question.correctIndex
                "short_answer", "fill_blank" ->
                    rawAnswer.equals(question.answer.trim(), ignoreCase = true)
                else -> false
            }
            val userAnswerText = when (question.type) {
                "multiple_choice" -> rawAnswer.toIntOrNull()
                    ?.let { question.options.getOrNull(it) }.orEmpty()
                else -> rawAnswer
            }.ifBlank { "Chưa trả lời" }
            val correctAnswerText = when (question.type) {
                "multiple_choice" -> question.options.getOrNull(question.correctIndex).orEmpty()
                else -> question.answer
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Câu ${index + 1}", fontWeight = FontWeight.Bold)
                        Icon(
                            imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = if (isCorrect) "Đúng" else "Sai",
                            tint = if (isCorrect) SecondaryGreen else Color(0xFFDC2626)
                        )
                    }
                    Text(question.prompt, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Bạn trả lời: $userAnswerText",
                        color = if (isCorrect) SecondaryGreen else Color(0xFFDC2626)
                    )

                    if (isCorrect) {
                        Text("Chính xác!", color = SecondaryGreen)
                    } else {
                        Text(
                            text = "Đáp án đúng: $correctAnswerText",
                            color = SecondaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (question.explanation.isNotBlank()) {
                            Text("Gợi ý có sẵn: ${question.explanation}")
                        }

                        aiExplanations[index]?.let { answer ->
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "AI giải thích",
                                            color = PrimaryBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(answer)
                                }
                            }
                        }
                        aiErrors[index]?.let { error ->
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }
                        if (aiExplanations[index] == null) {
                            val isLoading = aiLoading[index] == true
                            Button(
                                onClick = {
                                    aiErrors.remove(index)
                                    aiLoading[index] = true
                                    scope.launch {
                                        try {
                                            val prompt = """
                                                Bạn là gia sư Study Mentor. Hãy giải thích bằng tiếng Việt,
                                                dễ hiểu cho học sinh, không dùng Markdown.
                                                Loại câu hỏi: ${question.type}
                                                Câu hỏi: ${question.prompt}
                                                Lựa chọn: ${question.options.joinToString("; ")}
                                                Học sinh trả lời sai: $userAnswerText
                                                Đáp án đúng: $correctAnswerText
                                                Giải thích có sẵn: ${question.explanation}
                                                Giải thích vì sao học sinh sai, vì sao đáp án đúng,
                                                và một mẹo ghi nhớ. Giới hạn trong 5 câu.
                                            """.trimIndent()
                                            val response = StringBuilder()
                                            OpenAIApiClient.askQuestion(
                                                prompt = prompt,
                                                systemPrompt = "Bạn là gia sư, giải thích câu trả lời sai bằng tiếng Việt, ngắn gọn, chính xác.",
                                                maxTokens = 500
                                            ).collect { chunk ->
                                                response.append(chunk)
                                            }
                                            val answer = response.toString().trim()
                                            if (answer.isNotBlank()) {
                                                aiExplanations[index] = answer
                                            } else {
                                                aiErrors[index] = "AI chưa trả lời. Bạn có thể thử lại."
                                            }
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            aiErrors[index] = "Không gọi được AI. Kiểm tra mạng hoặc API rồi thử lại."
                                        } finally {
                                            aiLoading[index] = false
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEEF2FF),
                                    contentColor = PrimaryBlue
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = PrimaryBlue,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("AI đang giải thích...")
                                } else {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("AI giải thích tại sao sai")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
