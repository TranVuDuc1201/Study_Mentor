package com.example.study_mentor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.study_mentor.data.Quiz
import com.example.study_mentor.data.QuizChatSessionOption
import com.example.study_mentor.data.QuizGenerator
import com.example.study_mentor.ui.theme.BackgroundOffWhite
import com.example.study_mentor.ui.theme.PrimaryBlue
import com.example.study_mentor.ui.theme.SecondaryGreen

private val QUESTION_TYPE_OPTIONS = listOf(
    "multiple_choice" to "Multiple Choice",
    "short_answer" to "Short Answer",
    "fill_blank" to "Fill in the Blank"
)
private val DIFFICULTY_OPTIONS = listOf(
    "easy" to "Easy",
    "medium" to "Medium",
    "hard" to "Hard"
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    authViewModel: AuthViewModel = viewModel(),
    quizViewModel: QuizViewModel = viewModel(),
    statsViewModel: UserStatisticsViewModel = viewModel(),
    onStartGeneratedQuiz: (Quiz) -> Unit = {},
    onStartQuiz: (String) -> Unit = {}
) {
    val authState by authViewModel.authState.collectAsState()
    val quizState by quizViewModel.state.collectAsState()
    val uid = authState.currentUser?.uid

    var selectedDifficulty by remember { mutableStateOf("medium") }
    val currentStreak by statsViewModel.dayStreak.collectAsState()

    LaunchedEffect(uid) {
        quizViewModel.observe(uid)
        statsViewModel.observeUserStatistics(uid)
    }

    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("topic") }
    var topic by remember { mutableStateOf("") }
    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    var selectedQuestionCount by remember { mutableIntStateOf(5) }
    var selectedTypes by remember { mutableStateOf(QuizGenerator.ALLOWED_QUESTION_TYPES) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)


    LaunchedEffect(showCreateSheet, uid) {
        if (showCreateSheet && uid != null) {
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
                    text = "$currentStreak-Day Streak! Complete today's quiz to keep it going",
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
                        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                    ),
                    shape = MaterialTheme.shapes.medium
                )
                .clickable(enabled = !quizState.isGenerating) {
                    topic = ""
                    selectedSessionId = null
                    selectedMode = "topic"
                    selectedQuestionCount = 5
                    selectedTypes = QuizGenerator.ALLOWED_QUESTION_TYPES
                    showCreateSheet = true
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
                        text = "Create a Quiz with AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (quizState.isGenerating) {
                            "Generating quiz, please wait..."
                        } else {
                            "Create a quiz from a topic or a chat conversation"
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

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showCreateSheet) {
        val canSubmit = uid != null &&
                !quizState.isGenerating &&
                selectedTypes.isNotEmpty() &&
                when (selectedMode) {
                    "topic" -> topic.isNotBlank()
                    else -> selectedSessionId != null
                }

        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState,
            containerColor = BackgroundOffWhite,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header gradient đồng bộ banner chính
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Create a Quiz with AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "$selectedQuestionCount questions • ${selectedTypes.size} type(s) selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }
                }

                // Nội dung cuộn bên trong sheet
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Nguồn tạo quiz
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Quiz Source",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151)
                            )

                            SheetSourceCard(
                                title = "By Topic",
                                subtitle = "Type a topic you want to review",
                                selected = selectedMode == "topic",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = if (selectedMode == "topic") PrimaryBlue else Color.Gray
                                    )
                                },
                                onClick = { selectedMode = "topic" }
                            )

                            if (selectedMode == "topic") {
                                OutlinedTextField(
                                    value = topic,
                                    onValueChange = { topic = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Topic") },
                                    placeholder = { Text("e.g., Kotlin, Android, Database") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            SheetSourceCard(
                                title = "From an AI Chat Conversation",
                                subtitle = "Pick from your recent chat sessions",
                                selected = selectedMode == "chat",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = if (selectedMode == "chat") PrimaryBlue else Color.Gray
                                    )
                                },
                                onClick = { selectedMode = "chat" }
                            )

                            if (selectedMode == "chat") {
                                when {
                                    quizState.isLoadingSessions -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Loading conversations...")
                                        }
                                    }

                                    quizState.chatSessions.isEmpty() -> {
                                        Text(
                                            text = "No conversations found.",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    else -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            }
                        }
                    }

                    // 2. Ô tăng giảm số lượng câu hỏi (- / +)
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Number of questions",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF374151)
                                )
                                Text(
                                    text = "1 – 15 questions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalIconButton(
                                    onClick = { if (selectedQuestionCount > 1) selectedQuestionCount-- },
                                    enabled = selectedQuestionCount > 1,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease question count"
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PrimaryBlue.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = selectedQuestionCount.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = { if (selectedQuestionCount < 15) selectedQuestionCount++ },
                                    enabled = selectedQuestionCount < 15,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase question count"
                                    )
                                }
                            }
                        }
                    }
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Difficulty",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                DIFFICULTY_OPTIONS.forEach { (key, label) ->
                                    FilterChip(
                                        selected = selectedDifficulty == key,
                                        onClick = { selectedDifficulty = key },
                                        label = { Text(label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryBlue.copy(alpha = 0.14f),
                                            selectedLabelColor = PrimaryBlue
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 3. Loại câu hỏi
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Question types",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                QUESTION_TYPE_OPTIONS.forEach { (typeKey, typeLabel) ->
                                    val isSelected = typeKey in selectedTypes
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedTypes = if (isSelected) {
                                                if (selectedTypes.size > 1) selectedTypes - typeKey
                                                else selectedTypes
                                            } else {
                                                selectedTypes + typeKey
                                            }
                                        },
                                        label = { Text(typeLabel) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryBlue.copy(alpha = 0.14f),
                                            selectedLabelColor = PrimaryBlue
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (uid == null) {
                        Text(
                            text = "Please log in to use this feature.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Nút tạo quiz cố định ở chân sheet
                Button(
                    enabled = canSubmit,
                    onClick = {
                        if (selectedMode == "topic") {
                            quizViewModel.generateQuiz(
                                uid = uid,
                                topic = topic,
                                questionCount = selectedQuestionCount,
                                allowedTypes = selectedTypes,
                                difficulty = selectedDifficulty
                            )
                        } else {
                            quizViewModel.generateQuiz(
                                uid = uid,
                                sessionId = selectedSessionId,
                                questionCount = selectedQuestionCount,
                                allowedTypes = selectedTypes,
                                difficulty = selectedDifficulty
                            )
                        }
                        showCreateSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (quizState.isGenerating) "Generating..." else "Create Quiz",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetSourceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) PrimaryBlue.copy(alpha = 0.08f) else BackgroundOffWhite,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) PrimaryBlue else Color(0xFFE5E7EB)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
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
            containerColor = if (selected) Color(0xFFE0E7FF) else BackgroundOffWhite
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) PrimaryBlue else Color(0xFFE5E7EB)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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