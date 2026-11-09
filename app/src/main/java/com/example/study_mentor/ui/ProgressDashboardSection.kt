
package com.example.study_mentor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val DashboardBlue = Color(0xFF2563EB)
private val DashboardMuted = Color(0xFF64748B)
private val DashboardLightBlue = Color(0xFFEAF1FF)
private val DashboardDarkText = Color(0xFF17233B)

@Composable
fun ProgressDashboardSection(
    progressViewModel: ProgressViewModel = viewModel()
) {
    val state by progressViewModel.state.collectAsState()
    val auth = remember { FirebaseAuth.getInstance() }
    var uid by remember { mutableStateOf(auth.currentUser?.uid) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            uid = it.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(uid) {
        progressViewModel.observeProgress(uid)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Learning Progress",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = DashboardDarkText
        )

        if (uid == null) {
            Text(
                text = "Sign in to view your learning progress",
                style = MaterialTheme.typography.bodySmall,
                color = DashboardMuted
            )
        } else if (state.error != null) {
            Text(
                text = state.error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB45309)
            )
        }

        if (uid != null && state.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = DashboardBlue
            )
        }

        val signedIn = uid != null

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgressCard(
                "AI Chat Sessions",
                if (signedIn) state.studySessions?.toString() ?: "—" else "—",
                Modifier.weight(1f)
            )
            ProgressCard(
                "Quizzes Done",
                if (signedIn) state.quizzesDone?.toString() ?: "—" else "—",
                Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgressCard(
                "AI Chat Streak",
                if (signedIn) {
                    state.studyStreak?.let {
                        "$it ${if (it == 1) "day" else "days"}"
                    } ?: "—"
                } else "—",
                Modifier.weight(1f)
            )
            ProgressCard(
                "Quiz Accuracy",
                if (signedIn) state.completion?.let { "$it%" } ?: "—" else "—",
                Modifier.weight(1f)
            )
        }

        when {
            !signedIn -> ProgressMessageCard(
                "Weekly AI Chat Activity",
                "Sign in to view your weekly activity"
            )
            state.isLoading -> ProgressMessageCard(
                "Weekly AI Chat Activity",
                "Loading your activity..."
            )
            state.studySessions == null -> ProgressMessageCard(
                "Weekly AI Chat Activity",
                "Unable to load AI chat activity"
            )
            else -> WeeklyActivityChart(state.weeklyActivity)
        }

        SubjectProgressPanel(
            subjects = if (signedIn) state.subjectProgress else emptyList(),
            isLoading = signedIn && state.isLoading
        )
    }
}

@Composable
private fun ProgressCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardLightBlue)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = DashboardMuted
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DashboardDarkText
            )
        }
    }
}

@Composable
private fun WeeklyActivityChart(activity: List<Int>) {
    val today = LocalDate.now()
    val maxActivity = (activity.maxOrNull() ?: 0).coerceAtLeast(1)
    val days = (6 downTo 0).map { daysAgo ->
        today.minusDays(daysAgo.toLong()).dayOfWeek
            .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Weekly AI Chat Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DashboardDarkText
            )
            Text(
                "Your activity over the last 7 days",
                style = MaterialTheme.typography.bodySmall,
                color = DashboardMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth().height(154.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                activity.take(7).forEachIndexed { index, count ->
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            "$count",
                            style = MaterialTheme.typography.labelSmall,
                            color = DashboardMuted
                        )

                        Spacer(Modifier.height(6.dp))

                        val barHeight = if (count == 0) {
                            6.dp
                        } else {
                            (16 + 80 * count.toFloat() / maxActivity).dp
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(barHeight)
                                .background(
                                    if (count > 0) DashboardBlue
                                    else Color(0xFFE2E8F0),
                                    RoundedCornerShape(
                                        topStart = 6.dp,
                                        topEnd = 6.dp
                                    )
                                )
                        )

                        Spacer(Modifier.height(8.dp))
                        Text(
                            days[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = DashboardMuted
                        )
                    }
                }
            }

            Text(
                "AI chat sessions in the last 7 days",
                style = MaterialTheme.typography.bodySmall,
                color = DashboardMuted
            )
        }
    }
}

@Composable
private fun SubjectProgressPanel(
    subjects: List<SubjectProgress>,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Subject Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DashboardDarkText
            )

            if (subjects.isEmpty()) {
                Text(
                    if (isLoading) "Loading subject progress..."
                    else "No completed quiz results available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DashboardMuted
                )
            } else {
                subjects.forEach { subject ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                subject.subject,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${subject.percentage}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DashboardBlue
                            )
                        }

                        LinearProgressIndicator(
                            progress = { subject.percentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = DashboardBlue,
                            trackColor = DashboardLightBlue
                        )

                        Text(
                            "${subject.correctAnswers}/${subject.totalQuestions} correct answers",
                            style = MaterialTheme.typography.bodySmall,
                            color = DashboardMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressMessageCard(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DashboardDarkText
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = DashboardMuted
            )
        }
    }
}
