package com.example.study_mentor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study_mentor.data.ChatSession
import com.example.study_mentor.data.Quiz
import com.example.study_mentor.data.QuizChatSessionOption
import com.example.study_mentor.ui.theme.BackgroundOffWhite
import com.example.study_mentor.ui.theme.PrimaryBlue
import com.example.study_mentor.ui.theme.SecondaryGreen

@Composable
fun PracticeScreen(
    authViewModel: AuthViewModel = viewModel(),
    quizViewModel: QuizViewModel = viewModel(),
    onStartGeneratedQuiz: (Quiz) -> Unit = {},
    onStartQuiz: (String) -> Unit = {}
) {
    val authState by authViewModel.authState.collectAsState()
    val quizState by quizViewModel.state.collectAsState()
    val uid = authState.currentUser?.uid

    LaunchedEffect(uid) {
        quizViewModel.observe(uid)
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("topic") }
    var topic by remember { mutableStateOf("") }
    var selectedSessionId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showCreateDialog, uid) {
        if (showCreateDialog && uid != null) {
            quizViewModel.loadChatSessions(uid)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column {
            Text(
                text = "Daily Practice",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Test your knowledge",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                    ),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Streak",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "14-Day Streak! Complete today's quiz to keep it going",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
                    ),
                    shape = MaterialTheme.shapes.medium
                )
                .clickable(enabled = !quizState.isGenerating) {
                    topic = ""
                    selectedSessionId = null
                    selectedMode = "topic"
                    showCreateDialog = true
                }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (quizState.isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Quiz",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Tự tạo Quiz bằng AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (quizState.isGenerating) {
                            "Đang tạo quiz, vui lòng đợi..."
                        } else {
                            "Tạo Quiz theo chủ đề hoặc từ đoạn chat"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        quizState.error?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (uid != null && quizState.quizzes.isNotEmpty()) {
            Column {
                Text(
                    text = "Your Quizzes",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${quizState.quizzes.size} quizzes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                quizState.quizzes.forEach { quiz ->
                    QuizCard(
                        title = quiz.title,
                        subject = quiz.subject.pretty("General"),
                        difficulty = quiz.difficulty.pretty("Medium"),
                        subtitle = "${quiz.items.size} questions • +${quiz.xpReward} XP",
                        icon = Icons.Outlined.Create,
                        onStartClick = { onStartGeneratedQuiz(quiz) }
                    )
                }
            }
        }

//        Column {
//            Text(
//                text = "Today's Challenges",
//                style = MaterialTheme.typography.titleLarge,
//                color = MaterialTheme.colorScheme.onBackground
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//            Text(
//                text = "4 quizzes",
//                style = MaterialTheme.typography.bodyMedium,
//                color = Color.Gray
//            )
//        }
//
//        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
//            QuizCard(
//                title = "Quadratic Equations",
//                subject = "Math",
//                difficulty = "Medium",
//                subtitle = "3 questions • +150 XP",
//                icon = Icons.Outlined.Create,
//                onStartClick = { onStartQuiz("Quadratic Equations") }
//            )
//            QuizCard(
//                title = "Cellular Respiration",
//                subject = "Biology",
//                difficulty = "Hard",
//                subtitle = "3 questions • +200 XP",
//                icon = Icons.Outlined.Create,
//                onStartClick = { onStartQuiz("Cellular Respiration") }
//            )
//            QuizCard(
//                title = "Toán Lớp 2",
//                subject = "Tiểu học",
//                difficulty = "Easy",
//                subtitle = "3 questions • +100 XP",
//                icon = Icons.Outlined.Create,
//                onStartClick = { onStartQuiz("Toán Lớp 2") }
//            )
//            QuizCard(
//                title = "IQ Test",
//                subject = "Logic",
//                difficulty = "Medium",
//                subtitle = "3 questions • +200 XP",
//                icon = Icons.Outlined.Create,
//                onStartClick = { onStartQuiz("IQ Test") }
//            )
//        }
//
        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text("Tạo Quiz bằng AI", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Chọn cách bạn muốn tạo Quiz:")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = "topic" },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMode == "topic",
                            onClick = { selectedMode = "topic" }
                        )
                        Column {
                            Text("Theo chủ đề", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Nhập chủ đề muốn ôn tập.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    if (selectedMode == "topic") {
                        OutlinedTextField(
                            value = topic,
                            onValueChange = { topic = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Chủ đề") },
                            placeholder = { Text("Ví dụ: Kotlin, Android, Database") },
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = "chat" },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMode == "chat",
                            onClick = { selectedMode = "chat" }
                        )
                        Column {
                            Text("Từ đoạn chat với AI", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Chọn một cuộc trò chuyện đã có.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    if (selectedMode == "chat") {
                        when {
                            quizState.isLoadingSessions -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Đang tải cuộc trò chuyện...")
                                }
                            }

                            quizState.chatSessions.isEmpty() -> {
                                Text(
                                    "Không tìm thấy cuộc trò chuyện nào.",
                                    color = Color.Gray
                                )
                            }

                            else -> {
                                quizState.chatSessions.forEach { session ->
                                    ChatSessionRow(
                                        session = session,
                                        selected = selectedSessionId == session.id,
                                        onClick = { selectedSessionId = session.id }
                                    )
                                }
                            }
                        }
                    }

                    if (uid == null) {
                        Text(
                            "Vui lòng đăng nhập để sử dụng tính năng này.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = uid != null &&
                        !quizState.isGenerating &&
                        when (selectedMode) {
                            "topic" -> topic.isNotBlank()
                            else -> selectedSessionId != null
                        },
                    onClick = {
                        if (selectedMode == "topic") {
                            quizViewModel.generateQuiz(uid, topic = topic)
                        } else {
                            quizViewModel.generateQuiz(
                                uid = uid,
                                sessionId = selectedSessionId
                            )
                        }
                        showCreateDialog = false
                    }
                ) {
                    Text(if (quizState.isGenerating) "Đang tạo..." else "Tạo Quiz")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun ChatSessionRow(
    session: QuizChatSessionOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFE0E7FF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = session.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun QuizCard(
    title: String,
    subject: String,
    difficulty: String,
    subtitle: String,
    icon: ImageVector,
    onStartClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = BackgroundOffWhite,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TagPill(
                            text = subject,
                            backgroundColor = Color(0xFFE0E7FF),
                            textColor = PrimaryBlue
                        )
                        TagPill(
                            text = difficulty,
                            backgroundColor = Color(0xFFFEF3C7),
                            textColor = Color(0xFFD97706)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(SecondaryGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ready to start",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onStartClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Start Quiz", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TagPill(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun String.pretty(fallback: String): String =
    if (isBlank()) fallback
    else replace('_', ' ').replaceFirstChar { it.uppercase() }
