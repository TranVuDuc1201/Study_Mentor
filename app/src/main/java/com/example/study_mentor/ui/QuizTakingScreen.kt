package com.example.study_mentor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.study_mentor.data.Quiz
import com.example.study_mentor.data.QuizItem
import com.example.study_mentor.ui.theme.BackgroundOffWhite
import com.example.study_mentor.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTakingScreen(
    quiz: Quiz,
    onNavigateBack: () -> Unit,
    onNavigateToResult: (quizTitle: String, score: Int, totalQuestions: Int, xpEarned: Int) -> Unit
) {
    val questions = quiz.items

    if (questions.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(quiz.title) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            },
            containerColor = BackgroundOffWhite
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No questions available.")
            }
        }
        return
    }

    var currentIndex by remember(quiz.quizId) { mutableIntStateOf(0) }

    // Each entry stores the user's answer.
    // For multiple choice: option index as String.
    // For short answer / fill blank: typed answer as String.
    val answers = remember(quiz.quizId) {
        mutableStateListOf<String?>().apply {
            repeat(questions.size) { add(null) }
        }
    }

    var textAnswer by remember(quiz.quizId) { mutableStateOf("") }

    val currentQuestion = questions[currentIndex]
    val progress = (currentIndex + 1).toFloat() / questions.size
    val isLastQuestion = currentIndex == questions.lastIndex

    // Keep the text field synchronized when moving between questions.
    fun loadCurrentTextAnswer() {
        textAnswer = answers[currentIndex].orEmpty()
    }

    fun saveCurrentAnswer() {
        if (currentQuestion.type == "short_answer" || currentQuestion.type == "fill_blank") {
            answers[currentIndex] = textAnswer.trim()
        }
    }

    fun calculateScore(): Int {
        return questions.countIndexed { index, item ->
            val userAnswer = answers[index]?.trim().orEmpty()

            when (item.type) {
                "multiple_choice" -> {
                    userAnswer.toIntOrNull() == item.correctIndex
                }

                "short_answer", "fill_blank" -> {
                    normalizeAnswer(userAnswer) == normalizeAnswer(item.answer)
                }

                else -> false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quiz.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        containerColor = BackgroundOffWhite
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Question ${currentIndex + 1} of ${questions.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = PrimaryBlue
                )

                QuestionCard(question = currentQuestion)

                when (currentQuestion.type) {
                    "multiple_choice" -> {
                        currentQuestion.options.forEachIndexed { optionIndex, optionText ->
                            val selected =
                                answers[currentIndex]?.toIntOrNull() == optionIndex

                            OptionCard(
                                optionText = optionText,
                                isSelected = selected,
                                onClick = {
                                    answers[currentIndex] = optionIndex.toString()
                                }
                            )
                        }
                    }

                    "short_answer", "fill_blank" -> {
                        OutlinedTextField(
                            value = textAnswer,
                            onValueChange = {
                                textAnswer = it
                                answers[currentIndex] = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    if (currentQuestion.type == "fill_blank")
                                        "Your answer"
                                    else
                                        "Short answer"
                                )
                            },
                            placeholder = {
                                Text("Enter your answer")
                            },
                            minLines = 3
                        )
                    }

                    else -> {
                        Text(
                            text = "Unsupported question type: ${currentQuestion.type}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                if (currentIndex > 0) {
                    Button(
                        onClick = {
                            saveCurrentAnswer()
                            currentIndex--
                            loadCurrentTextAnswer()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.LightGray
                        )
                    ) {
                        Text("Previous", color = Color.Black)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        saveCurrentAnswer()

                        if (isLastQuestion) {
                            val score = calculateScore()
                            val xpEarned = if (questions.isNotEmpty()) {
                                ((score.toFloat() / questions.size) * quiz.xpReward)
                                    .toInt()
                            } else {
                                0
                            }

                            onNavigateToResult(
                                quiz.title,
                                score,
                                questions.size,
                                xpEarned
                            )
                        } else {
                            currentIndex++
                            loadCurrentTextAnswer()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    )
                ) {
                    Text(if (isLastQuestion) "Submit Quiz" else "Next")
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(question: QuizItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = when (question.type) {
                    "multiple_choice" -> "Choose one answer"
                    "short_answer" -> "Write a short answer"
                    "fill_blank" -> "Fill in the blank"
                    else -> question.type
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun OptionCard(
    optionText: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor =
        if (isSelected) Color(0xFFE0E7FF) else Color.White

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

private fun normalizeAnswer(value: String): String =
    value.trim().lowercase().replace(Regex("\\s+"), " ")

private inline fun <T> List<T>.countIndexed(
    predicate: (index: Int, value: T) -> Boolean
): Int {
    var count = 0
    forEachIndexed { index, value ->
        if (predicate(index, value)) count++
    }
    return count
}
