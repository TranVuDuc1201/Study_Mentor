
package com.example.study_mentor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.study_mentor.ui.theme.BackgroundOffWhite
import com.example.study_mentor.ui.theme.PrimaryBlue
import com.google.firebase.auth.FirebaseAuth

private val DarkText = Color(0xFF17233B)
private val MutedText = Color(0xFF718096)
private val SoftBlue = Color(0xFFEAF1FF)

@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAskAI: () -> Unit,
    onQuestionSelected: (String) -> Unit
) {

    // ============================================
    // FIREBASE AUTHENTICATION STATE
    // ============================================

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    var currentUserUid by remember {
        mutableStateOf(auth.currentUser?.uid)
    }

    DisposableEffect(auth) {

        val authListener = FirebaseAuth.AuthStateListener {
                firebaseAuth ->

            currentUserUid = firebaseAuth.currentUser?.uid
        }

        auth.addAuthStateListener(authListener)

        onDispose {
            auth.removeAuthStateListener(authListener)
        }
    }

    val signedIn = currentUserUid != null

    // ============================================
    // HOME SCREEN
    // ============================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 20.dp,
                vertical = 18.dp
            ),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {

        // ========================================
        // HEADER
        // ========================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Hello, Learner! 👋",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "Ready to learn something new?",
                    fontSize = 13.sp,
                    color = MutedText
                )
            }

            // ====================================
            // SIGN IN OR PROFILE AVATAR
            // ====================================

            if (signedIn) {

                IconButton(
                    onClick = onNavigateToProfile
                ) {

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = SoftBlue,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Open Profile",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

            } else {

                OutlinedButton(
                    onClick = onNavigateToLogin,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
                ) {

                    Text(
                        text = "Sign In",
                        fontSize = 12.sp
                    )
                }
            }
        }

        // ========================================
        // WELCOME BANNER
        // ========================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = PrimaryBlue
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50.dp)
                ) {

                    Text(
                        text = "✦ YOUR AI STUDY PARTNER",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        ),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Learn smarter,\nnot harder.",
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Save answers, track progress and unlock more learning opportunities.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )

                // =================================
                // GET STARTED FREE
                // ONLY WHEN USER IS NOT SIGNED IN
                // =================================

                if (!signedIn) {

                    Button(
                        onClick = onNavigateToRegister,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = PrimaryBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Text(
                            text = "Get Started Free",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        // ========================================
        // LEARNING PROGRESS DASHBOARD
        // ========================================

        // Keep the existing Firebase dashboard.
        // It handles its own login state and UID.
        ProgressDashboardSection()

        // ========================================
        // ASK AI SECTION
        // ========================================

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "What would you like to learn?",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigateToAskAI()
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 3.dp
                )
            ) {

                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Text(
                        text = "Ask anything about your homework...",
                        modifier = Modifier.weight(1f),
                        color = MutedText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Button(
                        onClick = onNavigateToAskAI,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 15.dp,
                            vertical = 8.dp
                        )
                    ) {

                        Text(
                            text = "Ask",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // ========================================
        // SUGGESTED QUESTIONS
        // ========================================

        Column(
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {

            Text(
                text = "Explore popular questions",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Text(
                text = "Tap a question to start learning with AI",
                fontSize = 12.sp,
                color = MutedText
            )

            QuestionCard(
                question = "How do I solve quadratic equations?",
                icon = Icons.Outlined.Create,
                tag = "Mathematics",
                onClick = {
                    onQuestionSelected(
                        "How do I solve quadratic equations?"
                    )
                }
            )

            QuestionCard(
                question = "What is the mitochondria?",
                icon = Icons.Outlined.Info,
                tag = "Science",
                onClick = {
                    onQuestionSelected(
                        "What is the mitochondria?"
                    )
                }
            )

            QuestionCard(
                question = "Explain the French Revolution",
                icon = Icons.Outlined.Search,
                tag = "History",
                onClick = {
                    onQuestionSelected(
                        "Explain the French Revolution"
                    )
                }
            )
        }

        // ========================================
        // PERSONAL AI TUTOR CARD
        // ========================================

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onNavigateToAskAI()
                },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = SoftBlue
            )
        ) {

            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(30.dp)
                )

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Your personal AI tutor",
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "Get explanations and step-by-step help.",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Open Ask AI",
                    tint = MutedText
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

// ============================================
// SUGGESTED QUESTION CARD
// ============================================

@Composable
fun QuestionCard(
    question: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = SoftBlue,
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(13.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {

                Text(
                    text = question,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText
                )

                Surface(
                    color = SoftBlue,
                    shape = CircleShape
                ) {

                    Text(
                        text = tag,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        ),
                        fontSize = 10.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
