package com.example.study_mentor.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.study_mentor.OpenAIApiClient
import com.example.study_mentor.ui.theme.BackgroundOffWhite
import com.example.study_mentor.ui.theme.PrimaryBlue
import com.example.study_mentor.ui.theme.SecondaryGreen
import kotlinx.coroutines.launch

@Composable
fun QuizResultScreen(
    viewModel: QuizViewModel,
    onFinish: () -> Unit
) {
    val result by viewModel.quizResult.collectAsState()
    val quiz by viewModel.currentQuiz.collectAsState()
    val selectedAnswers by viewModel.selectedAnswers.collectAsState()

    val aiExplanations = remember { mutableStateMapOf<Int, String>() }
    val aiLoading = remember { mutableStateMapOf<Int, Boolean>() }
    val scope = rememberCoroutineScope()

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
            Spacer(modifier = Modifier.height(12.dp))

            // Trophy / Star Icon
            Surface(
                shape = CircleShape,
                color = Color(0xFFFEF3C7),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Trophy",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Text(
                text = "Quiz Completed!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = result?.quizTitle ?: "Practice Session",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )

            // Score Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Score", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${result?.score ?: 0}/${result?.totalQuestions ?: 0}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "XP Earned", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+${result?.xpEarned ?: 0}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryGreen
                        )
                    }
                }
            }

            // Review & AI Explanations Section
            if (quiz != null && quiz!!.questions.isNotEmpty()) {
                Text(
                    text = "Review Answers & AI Explanations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                quiz!!.questions.forEachIndexed { index, question ->
                    val userAnsIndex = selectedAnswers[index]
                    val isCorrect = userAnsIndex == question.correctOptionIndex
                    val userAnsText = userAnsIndex?.let { question.options.getOrNull(it) } ?: "Not answered"
                    val correctAnsText = question.options.getOrNull(question.correctOptionIndex) ?: ""

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Question ${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Icon(
                                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = if (isCorrect) "Correct" else "Incorrect",
                                    tint = if (isCorrect) SecondaryGreen else Color(0xFFEF4444)
                                )
                            }

                            Text(
                                text = question.questionText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Your answer: $userAnsText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCorrect) SecondaryGreen else Color(0xFFEF4444)
                            )

                            if (!isCorrect) {
                                Text(
                                    text = "Correct answer: $correctAnsText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SecondaryGreen,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val explanation = aiExplanations[index]
                                val isLoading = aiLoading[index] == true

                                if (explanation != null) {
                                    Surface(
                                        color = Color(0xFFEFF6FF),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.AutoAwesome, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("AI Explanation", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 13.sp)
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(explanation, fontSize = 14.sp, color = Color(0xFF1E3A8A))
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            aiLoading[index] = true
                                            scope.launch {
                                                try {
                                                    val prompt = """
                                                        Question: "${question.questionText}"
                                                        Options: ${question.options.joinToString(", ")}
                                                        Student's wrong answer: "$userAnsText"
                                                        Correct answer: "$correctAnsText"
                                                        Please explain concisely why the student's answer is incorrect and why the correct answer is right. Keep it clear, friendly, and helpful.
                                                    """.trimIndent()

                                                    var resultText = ""
                                                    OpenAIApiClient.askQuestion(prompt = prompt, maxTokens = 500).collect { chunk ->
                                                        resultText += chunk
                                                    }
                                                    aiExplanations[index] = resultText.trim()
                                                } catch (e: Exception) {
                                                    aiExplanations[index] = "Could not generate AI explanation at this time."
                                                } finally {
                                                    aiLoading[index] = false
                                                }
                                            }
                                        },
                                        enabled = !isLoading,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF2FF)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                                            Spacer(Modifier.width(8.dp))
                                            Text("AI is explaining...", color = PrimaryBlue, fontSize = 13.sp)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("✨ AI Giải thích tại sao sai", color = PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Finish Button
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "Back to Practice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
