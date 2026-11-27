package com.example.study_mentor.ui

import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import com.example.study_mentor.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study_mentor.data.Quiz
import com.example.study_mentor.data.QuizItem
import com.example.study_mentor.data.QuizResultRepository
import com.example.study_mentor.data.UserStatisticsRepository
import com.example.study_mentor.ui.theme.BackgroundOffWhite
import com.example.study_mentor.ui.theme.PrimaryBlue
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material3.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTakingScreen(
    quiz: Quiz,
    onNavigateBack: () -> Unit,
    onNavigateToResult: (quizTitle: String, score: Int, totalQuestions: Int, xpEarned: Int) -> Unit,
    quizViewModel: QuizViewModel = viewModel()
) {
    val quizUiState by quizViewModel.state.collectAsState()
    val questions = quiz.items
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid }


    LaunchedEffect(quiz.quizId, currentUid) {
        quizViewModel.observe(currentUid)
        quizViewModel.resetQuizTakingState()
    }

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

    // ============================================
    // LO-FI MUSIC & AUDIO FEEDBACK IMPLEMENTATION
    // ============================================
    var musicEnabled by remember(quiz.quizId) { mutableStateOf(true) }

    // Initialize MediaPlayer with raw resource res/raw/quiz_lofi.mp3
    val musicPlayer = remember(context, quiz.quizId) {
        MediaPlayer.create(context, R.raw.quiz_lofi)?.apply {
            isLooping = true
            setVolume(0.35f, 0.35f)
        }
    }

    // Manage playback according to screen state & lifecycle
    DisposableEffect(musicPlayer, lifecycleOwner, musicEnabled) {
        fun updatePlayback() {
            runCatching {
                if (musicEnabled && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    if (musicPlayer?.isPlaying == false) {
                        musicPlayer.start()
                    }
                } else if (musicPlayer?.isPlaying == true) {
                    musicPlayer.pause()
                }
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_STOP) {
                updatePlayback()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        updatePlayback()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching {
                if (musicPlayer?.isPlaying == true) {
                    musicPlayer.pause()
                }
            }
        }
    }

    // Release resources when navigating away from Quiz taking screen
    DisposableEffect(musicPlayer) {
        onDispose {
            runCatching {
                musicPlayer?.release()
            }
        }
    }

    // ToneGenerator for option answer sound feedback
    val feedbackTone = remember(quiz.quizId) {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }.getOrNull()
    }

    DisposableEffect(feedbackTone) {
        onDispose {
            runCatching { feedbackTone?.release() }
        }
    }

    fun playAnswerFeedback(isCorrect: Boolean) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (musicEnabled) {
            runCatching {
                feedbackTone?.startTone(
                    if (isCorrect) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK,
                    180
                )
            }
        }
    }

    var currentIndex by remember(quiz.quizId) { mutableIntStateOf(0) }
    var isSubmitting by remember(quiz.quizId) { mutableStateOf(false) }
    var showCelebration by remember(quiz.quizId) { mutableStateOf(false) }

    // Answers locked after selection to display instant visual feedback
    val checkedAnswers = remember(quiz.quizId) {
        mutableStateListOf<Boolean>().apply { repeat(questions.size) { add(false) } }
    }

    // Each entry stores the user's answer.
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

    val listScrollState = rememberScrollState()
    val isFeedbackVisible = checkedAnswers[currentIndex] && quizUiState.isAnswerEvaluated

    LaunchedEffect(isFeedbackVisible, currentIndex) {
        if (isFeedbackVisible) {
            delay(120) // chờ AnimatedVisibility mở xong rồi mới cuộn
            listScrollState.animateScrollTo(listScrollState.maxValue)
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
                },
                actions = {
                    IconButton(onClick = { musicEnabled = !musicEnabled }) {
                        Icon(
                            imageVector = if (musicEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (musicEnabled) "Mute Music" else "Unmute Music",
                            tint = PrimaryBlue
                        )
                    }
                }
            )
        },
        containerColor = BackgroundOffWhite
    ) { innerPadding ->

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(listScrollState),
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

                    // Thanh Power-up cho câu trắc nghiệm nhiều lựa chọn
                    if (currentQuestion.type == "multiple_choice") {
                        val rightAnswerText = currentQuestion.options
                            .getOrNull(currentQuestion.correctIndex)
                            .orEmpty()
                        val remainingChoices = currentQuestion.options.size - quizUiState.eliminatedOptionIndices.size

                        RemoveWrongPowerUpChip(
                            remainingCount = quizUiState.removeWrongCount,
                            enabled = !checkedAnswers[currentIndex] &&
                                    !isSubmitting &&
                                    quizUiState.explodingOptionIndex == null &&
                                    remainingChoices > 2,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                quizViewModel.useRemoveOneWrongOption(
                                    options = currentQuestion.options,
                                    correctAnswer = rightAnswerText
                                )
                            }
                        )
                    }
                    when (currentQuestion.type) {
                        "multiple_choice" -> {
                            currentQuestion.options.forEachIndexed { optionIndex, optionText ->
                                val isEliminated = optionIndex in quizUiState.eliminatedOptionIndices
                                val isExploding = optionIndex == quizUiState.explodingOptionIndex
                                val selected =
                                    !isEliminated && answers[currentIndex]?.toIntOrNull() == optionIndex

                                OptionCard(
                                    optionText = optionText,
                                    isSelected = selected,
                                    isRevealed = checkedAnswers[currentIndex],
                                    isCorrectOption = optionIndex == currentQuestion.correctIndex,
                                    isEliminated = isEliminated,
                                    isExploding = isExploding,
                                    enabled = !checkedAnswers[currentIndex] &&
                                            !isSubmitting &&
                                            !isEliminated &&
                                            !isExploding,
                                    onClick = {
                                        if (!checkedAnswers[currentIndex] && !isEliminated && !isExploding) {
                                            answers[currentIndex] = optionIndex.toString()
                                            checkedAnswers[currentIndex] = true
                                            val isCorrect = optionIndex == currentQuestion.correctIndex
                                            val rightAnswer = currentQuestion.options.getOrNull(currentQuestion.correctIndex).orEmpty()
                                            quizViewModel.selectAnswer(optionIndex)
                                            quizViewModel.evaluateAnswer(isCorrect, rightAnswer)
                                            playAnswerFeedback(isCorrect)
                                        }
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
                                minLines = 3,
                                readOnly = checkedAnswers[currentIndex] || isSubmitting
                            )

                            if (!checkedAnswers[currentIndex]) {
                                Button(
                                    onClick = {
                                        saveCurrentAnswer()
                                        checkedAnswers[currentIndex] = true
                                        val isCorrect = isCorrectForFeedback(currentQuestion, answers[currentIndex].orEmpty())
                                        val rightAnswer = currentQuestion.answer
                                        quizViewModel.evaluateAnswer(isCorrect, rightAnswer)
                                        playAnswerFeedback(isCorrect)
                                    },
                                    enabled = textAnswer.isNotBlank() && !isSubmitting,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text("Check Answer")
                                }
                            }
                        }

                        else -> {
                            Text(
                                text = "Unsupported question type: ${currentQuestion.type}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Feedback visibility: Only shown if current question is evaluated AND isAnswerEvaluated is true in QuizViewModel
                    AnimatedVisibility(visible = checkedAnswers[currentIndex] && quizUiState.isAnswerEvaluated) {
                        val isCorrect = isCorrectForFeedback(
                            currentQuestion, answers[currentIndex].orEmpty()
                        )
                        val rightAnswer = when (currentQuestion.type) {
                            "multiple_choice" -> currentQuestion.options
                                .getOrNull(currentQuestion.correctIndex).orEmpty()
                            else -> currentQuestion.answer
                        }
                        AnswerFeedbackCard(
                            isCorrect = isCorrect,
                            correctAnswer = rightAnswer,
                            customMessage = quizUiState.feedbackMessage
                        )
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
                                quizViewModel.previousQuestion()
                                currentIndex--
                                loadCurrentTextAnswer()
                            },
                            enabled = !isSubmitting,
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
                            if (isLastQuestion) {
                                saveCurrentAnswer()
                                isSubmitting = true

                                val uid = FirebaseAuth
                                    .getInstance()
                                    .currentUser
                                    ?.uid

                                if (uid != null) {

                                    var score = 0

                                    val answerList = quiz.items.mapIndexed { index, item ->
                                        val userAnswer = answers[index] ?: ""

                                        val isCorrect = when (item.type) {
                                            "multiple_choice" -> {
                                                val selectedIndex =
                                                    answers[index]?.toIntOrNull()

                                                selectedIndex == item.correctIndex
                                            }

                                            "short_answer",
                                            "fill_blank" -> {
                                                userAnswer.trim()
                                                    .equals(
                                                        item.answer.trim(),
                                                        ignoreCase = true
                                                    )
                                            }

                                            else -> false
                                        }

                                        if (isCorrect) {
                                            score++
                                        }

                                        mapOf<String, Any?>(
                                            "questionIndex" to index,
                                            "question" to item.prompt,
                                            "userAnswer" to userAnswer,
                                            "correctAnswer" to when (item.type) {
                                                "multiple_choice" ->
                                                    item.options.getOrNull(item.correctIndex) ?: ""

                                                else ->
                                                    item.answer
                                            },
                                            "isCorrect" to isCorrect
                                        )
                                    }

                                    val totalQuestions = quiz.items.size
                                    val xpEarned = if (totalQuestions > 0) {
                                        (score.toDouble() / totalQuestions * 50.0).toInt()
                                    } else {
                                        0
                                    }

                                    coroutineScope.launch {
                                        try {
                                            QuizResultRepository.saveQuizResult(
                                                uid = uid,
                                                quizId = quiz.quizId,
                                                quizTitle = quiz.title,
                                                subject = quiz.subject,
                                                topic = quiz.topic,
                                                difficulty = quiz.difficulty,
                                                score = score,
                                                totalQuestions = totalQuestions,
                                                xpEarned = xpEarned,
                                                answers = answerList
                                            )

                                            val statsRepository = UserStatisticsRepository()
                                            statsRepository.recordQuizResult(
                                                correctAnswers = score,
                                                totalQuestions = totalQuestions
                                            )
                                            if (xpEarned > 0) {
                                                statsRepository.addXp(xpEarned.toLong())
                                            }

                                            if (totalQuestions > 0 && score.toFloat() / totalQuestions >= 0.8f) {
                                                showCelebration = true
                                                if (musicEnabled) {
                                                    runCatching { feedbackTone?.startTone(ToneGenerator.TONE_PROP_ACK, 240) }
                                                }
                                                delay(1300)
                                            }
                                            onNavigateToResult(
                                                quiz.title,
                                                score,
                                                totalQuestions,
                                                xpEarned
                                            )

                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            isSubmitting = false
                                            showCelebration = false
                                        }
                                    }

                                } else {
                                    onNavigateBack()
                                }

                            } else {
                                // Save current answer
                                saveCurrentAnswer()

                                // Advance to next question in ViewModel & reset feedback states
                                quizViewModel.nextQuestion(questions.size)

                                // Go to next question locally
                                currentIndex++

                                // Load answer of the new question
                                loadCurrentTextAnswer()
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        )
                    ) {
                        Text(
                            if (isSubmitting) {
                                "Submitting..."
                            } else if (isLastQuestion) {
                                "Submit Quiz"
                            } else {
                                "Next"
                            }
                        )
                    }
                }
            }

            if (showCelebration) {
                ConfettiOverlay(modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 9.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            "🎉 Excellent! Score >= 80%!",
                            modifier = Modifier.padding(20.dp),
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun RemoveWrongPowerUpChip(
    remainingCount: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val isActive = enabled && remainingCount > 0
    val contentTint = if (isActive) PrimaryBlue else Color(0xFF9CA3AF)
    val bgColor = if (isActive) Color.White else Color(0xFFF3F4F6)
    val badgeColor = if (isActive) PrimaryBlue else Color(0xFF9CA3AF)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onClick,
            enabled = isActive,
            shape = RoundedCornerShape(14.dp),
            color = bgColor,
            shadowElevation = if (isActive) 2.dp else 0.dp,
            modifier = Modifier
                .width(88.dp)
                .height(48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoFixHigh,
                    contentDescription = "Xóa 1 đáp án sai",
                    tint = contentTint,
                    modifier = Modifier.size(26.dp)
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = remainingCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
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

private fun isCorrectForFeedback(question: QuizItem, answer: String): Boolean =
    when (question.type) {
        "multiple_choice" -> answer.toIntOrNull() == question.correctIndex
        "short_answer", "fill_blank" ->
            answer.trim().equals(question.answer.trim(), ignoreCase = true)
        else -> false
    }

@Composable
private fun AnswerFeedbackCard(
    isCorrect: Boolean,
    correctAnswer: String,
    customMessage: String? = null
) {
    val tint = if (isCorrect) Color(0xFF166534) else Color(0xFFB91C1C)
    val background = if (isCorrect) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
    Card(
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = tint
            )
            Column {
                Text(
                    text = customMessage ?: if (isCorrect) "Correct! 🎉" else "Not quite, keep going!",
                    color = tint,
                    fontWeight = FontWeight.Bold
                )
                if (!isCorrect && correctAnswer.isNotBlank() && customMessage == null) {
                    Text(
                        text = "Correct answer: $correctAnswer",
                        color = tint,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionCard(
    optionText: String,
    isSelected: Boolean,
    isRevealed: Boolean,
    isCorrectOption: Boolean,
    isEliminated: Boolean = false,
    isExploding: Boolean = false,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val targetColor = when {
        isExploding -> Color(0xFFFFEDD5)
        isEliminated -> Color(0xFFF1F5F9)
        isRevealed && isCorrectOption -> Color(0xFFDCFCE7)
        isRevealed && isSelected -> Color(0xFFFEE2E2)
        isSelected -> Color(0xFFE0E7FF)
        else -> Color.White
    }
    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 280),
        label = "Answer background"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isExploding -> 1.06f
            isEliminated -> 0.96f
            isRevealed && isCorrectOption -> 1.015f
            else -> 1f
        },
        animationSpec = if (isExploding) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        } else {
            tween(durationMillis = 250)
        },
        label = "Answer pop"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = when {
            isExploding -> 0.85f
            isEliminated -> 0.45f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 250),
        label = "Answer alpha"
    )
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(isRevealed, isSelected, isCorrectOption, isExploding) {
        if (isExploding) {
            shakeX.animateTo(-10f, tween(50))
            shakeX.animateTo(10f, tween(60))
            shakeX.animateTo(-6f, tween(60))
            shakeX.animateTo(0f, tween(50))
        } else if (isRevealed && isSelected && !isCorrectOption) {
            shakeX.animateTo(-7f, tween(55))
            shakeX.animateTo(7f, tween(75))
            shakeX.animateTo(-3f, tween(75))
            shakeX.animateTo(0f, tween(65))
        } else {
            shakeX.snapTo(0f)
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = shakeX.value.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale, alpha = cardAlpha)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = optionText,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (isEliminated) TextDecoration.LineThrough else TextDecoration.None,
                color = when {
                    isEliminated -> Color.Gray
                    isRevealed && isCorrectOption -> Color(0xFF166534)
                    isRevealed && isSelected -> Color(0xFFB91C1C)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isSelected || (isRevealed && isCorrectOption))
                    FontWeight.Bold else FontWeight.Normal
            )
            if (isExploding) {
                Text(
                    text = "💥",
                    fontSize = 20.sp
                )
            } else if (isRevealed && (isCorrectOption || isSelected)) {
                Icon(
                    imageVector = if (isCorrectOption) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = if (isCorrectOption) "Correct" else "Incorrect",
                    tint = if (isCorrectOption) Color(0xFF16A34A) else Color(0xFFDC2626),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val fall = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        fall.animateTo(1f, animationSpec = tween(durationMillis = 1300))
    }
    val colors = remember {
        listOf(
            Color(0xFFF59E0B), Color(0xFF16A34A), Color(0xFF2563EB),
            Color(0xFFDB2777), Color(0xFF9333EA)
        )
    }
    Canvas(modifier = modifier) {
        for (i in 0 until 54) {
            val x = size.width * ((i * 47 % 101) / 101f)
            val speed = 0.65f + (i % 7) * 0.08f
            val y = (-45f - (i % 11) * 16f) + size.height * speed * fall.value * 1.6f
            drawRect(
                color = colors[i % colors.size],
                topLeft = Offset(x, y),
                size = Size(8f + i % 4, 12f + i % 5)
            )
        }
    }
}
