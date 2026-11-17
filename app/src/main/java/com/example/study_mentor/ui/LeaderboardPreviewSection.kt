
package com.example.study_mentor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val LeaderboardDark = Color(0xFF17233B)
private val LeaderboardMuted = Color(0xFF718096)
private val LeaderboardBlue = Color(0xFF356AE6)

@Composable
fun LeaderboardPreviewSection(
    onViewAll: () -> Unit,
    viewModel: LeaderboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFB938),
                    modifier = Modifier.size(23.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "Leaderboard",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = LeaderboardDark
                )
            }

            TextButton(onClick = onViewAll) {
                Text(
                    text = "View All",
                    color = LeaderboardBlue,
                    fontWeight = FontWeight.SemiBold
                )

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = LeaderboardBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = LeaderboardBlue
                            )
                        }
                    }

                    uiState.errorMessage != null -> {
                        Text(
                            text = "Unable to load leaderboard.",
                            color = LeaderboardMuted,
                            fontSize = 13.sp
                        )
                    }

                    uiState.users.isEmpty() -> {
                        Text(
                            text = "No leaderboard data yet.",
                            color = LeaderboardMuted,
                            fontSize = 13.sp
                        )
                    }

                    else -> {
                        uiState.users.take(3).forEach { user ->
                            LeaderboardPreviewRow(
                                rank = user.rank,
                                name = user.name,
                                level = user.level,
                                totalXp = user.totalXp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardPreviewRow(
    rank: Int,
    name: String,
    level: Int,
    totalXp: Long
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFB938)
        2 -> Color(0xFF9AA7B8)
        3 -> Color(0xFFCD8B55)
        else -> LeaderboardMuted
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = rankColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                fontWeight = FontWeight.Bold,
                color = rankColor,
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                color = LeaderboardDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1
            )

            Text(
                text = "Level $level",
                color = LeaderboardMuted,
                fontSize = 11.sp
            )
        }

        Text(
            text = "$totalXp XP",
            color = LeaderboardBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
