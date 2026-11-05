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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.study_mentor.ui.theme.BackgroundOffWhite
import com.example.study_mentor.ui.theme.PrimaryBlue
import com.example.study_mentor.ui.theme.SecondaryGreen

@Composable
fun PracticeScreen(
    onStartQuiz: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
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

        // Streak Card
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

        // Challenges Header
        Column {
            Text(
                text = "Today's Challenges",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "2 quizzes",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // Quiz Cards List
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            QuizCard(
                title = "Quadratic Equations",
                subject = "Math",
                difficulty = "Medium",
                subtitle = "3 questions • +150 XP",
                icon = Icons.Outlined.Create,
                onStartClick = { onStartQuiz("Quadratic Equations") }
            )
            QuizCard(
                title = "Cellular Respiration",
                subject = "Biology",
                difficulty = "Hard",
                subtitle = "3 questions • +200 XP",
                icon = Icons.Outlined.Create,
                onStartClick = { onStartQuiz("Cellular Respiration") }
            )
        }
        
        // Extra bottom padding for BottomAppBar spacing
        Spacer(modifier = Modifier.height(80.dp))
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
                // Icon on the left
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
                
                Column {
                    // Subject and Difficulty Tags
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TagPill(text = subject, backgroundColor = Color(0xFFE0E7FF), textColor = PrimaryBlue)
                        TagPill(text = difficulty, backgroundColor = Color(0xFFFEF3C7), textColor = Color(0xFFD97706))
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
            
            // Bottom Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Green dot and "Ready to start"
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
                
                // Start Quiz Button
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
fun TagPill(text: String, backgroundColor: Color, textColor: Color) {
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
