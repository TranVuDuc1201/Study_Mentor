package com.example.study_mentor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.study_mentor.data.QuizAiInsights
import com.example.study_mentor.data.QuizInsightsRepository
import com.example.study_mentor.ui.theme.BackgroundOffWhite
import com.example.study_mentor.ui.theme.PrimaryBlue
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException

/** Only this new feature's user-facing copy and AI output are in English. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizInsightsScreen(
    quizId: String,
    quizTitle: String,
    onNavigateBack: () -> Unit
) {
    var retryVersion by remember(quizId) { mutableIntStateOf(0) }
    var report by remember(quizId) { mutableStateOf<QuizAiInsights?>(null) }
    var loading by remember(quizId) { mutableStateOf(true) }
    var error by remember(quizId) { mutableStateOf<String?>(null) }

    LaunchedEffect(quizId, quizTitle, retryVersion) {
        loading = true
        error = null
        report = null
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw IllegalStateException("Please sign in to analyze your quiz.")
            val saved = QuizInsightsRepository.loadSavedReview(uid, quizId, quizTitle)
            report = QuizInsightsRepository.generateInsights(saved)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            error = exception.message ?: "Unable to generate insights. Please try again."
        } finally {
            loading = false
        }
    }

    Scaffold(
        containerColor = BackgroundOffWhite,
        topBar = {
            TopAppBar(
                title = { Text("AI Learning Insights", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to results")
                    }
                }
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue)
                Spacer(Modifier.width(10.dp))
                Text("Personalized Quiz Review", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
            }
            Text("Review of your completed quiz", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 65.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(color = PrimaryBlue)
                            Text("Analyzing your saved quiz answers...", color = Color.Gray)
                        }
                    }
                }
                error != null -> {
                    InsightsCard("Analysis unavailable") {
                        Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { retryVersion++ },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                            Text("Try Again")
                        }
                    }
                }
                report != null -> {
                    val insights = report!!
                    InsightsCard("Overall Feedback") {
                        Text(insights.summary, style = MaterialTheme.typography.bodyLarge)
                    }
                    InsightsListCard("What You Did Well", insights.strengths,
                        emptyText = "The available results do not identify any specific strengths yet.")
                    InsightsListCard("Areas to Improve", insights.areasToImprove,
                        emptyText = "No incorrect answers were recorded for this attempt.")

                    InsightsCard("Mistake Explanations") {
                        if (insights.totalMistakes == 0) {
                            Text("Great job! You answered every question correctly.")
                        } else if (insights.mistakeExplanations.isEmpty()) {
                            Text("Detailed explanations are not available for this attempt.")
                        } else {
                            insights.mistakeExplanations.forEach { note ->
                                Text("Question ${note.number}", fontWeight = FontWeight.SemiBold,
                                    color = PrimaryBlue)
                                Text(note.explanation)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }

                    InsightsListCard("Your Study Plan", insights.studyPlan,
                        emptyText = "Try another quiz to keep building your skills.")
                    Text(
                        "AI feedback may contain mistakes. Review important concepts using trusted course materials.",
                        style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Quiz Results")
            }
        }
    }
}

@Composable
private fun InsightsCard(
    heading: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(heading, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = PrimaryBlue)
            content()
        }
    }
}

@Composable
private fun InsightsListCard(heading: String, entries: List<String>, emptyText: String) {
    InsightsCard(heading) {
        if (entries.isEmpty()) Text(emptyText, color = Color.Gray)
        entries.forEachIndexed { index, entry ->
            Text("${index + 1}. $entry", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
