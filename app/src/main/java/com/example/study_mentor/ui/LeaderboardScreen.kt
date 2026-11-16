package com.example.study_mentor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study_mentor.data.LeaderboardUser
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

// Bảng màu đồng bộ trực tiếp với màn hình Home
private val BrandBlue = Color(0xFF1D61E7)      // Màu xanh dương chính của App
private val SoftBlueBg = Color(0xFFEFF6FF)     // Màu nền thẻ xanh siêu nhạt (giống card Learning Progress)
private val NeutralCardBg = Color(0xFFF8FAFC)  // Màu nền thẻ phụ xám nhạt
private val BorderBlue = Color(0xFFBFDBFE)     // Viền xanh nhạt tạo điểm nhấn cho thẻ của bạn
private val TextPrimary = Color(0xFF0F172A)    // Chữ đen đậm dễ đọc
private val TextSecondary = Color(0xFF64748B)  // Chữ xám phụ

@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val isStreakTab = selectedTab == 1

    val users = if (isStreakTab) uiState.streakUsers else uiState.users
    val currentUser = users.find { it.uid == currentUid }
    val isLoading = if (isStreakTab) uiState.isStreakLoading else uiState.isLoading
    val errorMessage = if (isStreakTab) uiState.streakErrorMessage else uiState.errorMessage

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = SoftBlueBg
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = BrandBlue
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Leaderboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isStreakTab) "Stay consistent & build your streak! 🔥" else "Compete with peers & earn XP 🚀",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Pill Tab Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SoftBlueBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton(
                    text = "XP Ranking",
                    selected = selectedTab == 0,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = 0 }

                TabButton(
                    text = "🔥 Streak Ranking",
                    selected = selectedTab == 1,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = 1 }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandBlue)
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadLeaderboard() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                        ) {
                            Text("Try again", color = Color.White)
                        }
                    }
                }

                users.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isStreakTab) "No students have a learning streak yet." else "No students on the leaderboard yet.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                else -> {
                    val topThree = users.take(3)
                    val remainingUsers = users.drop(3)

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        // Top 3 Podium section
                        if (topThree.isNotEmpty()) {
                            item {
                                Text(
                                    text = if (isStreakTab) "TOP STREAK HEROES" else "TOP XP CHAMPIONS",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // 2nd Place
                                    topThree.getOrNull(1)?.let { user ->
                                        TopUserCard(
                                            user = user,
                                            currentUid = currentUid,
                                            isStreak = isStreakTab,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    // 1st Place (Taller)
                                    topThree.getOrNull(0)?.let { user ->
                                        TopUserCard(
                                            user = user,
                                            currentUid = currentUid,
                                            isStreak = isStreakTab,
                                            modifier = Modifier.weight(1.15f)
                                        )
                                    }
                                    // 3rd Place
                                    topThree.getOrNull(2)?.let { user ->
                                        TopUserCard(
                                            user = user,
                                            currentUid = currentUid,
                                            isStreak = isStreakTab,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        if (remainingUsers.isNotEmpty()) {
                            item {
                                Text(
                                    text = "ALL RANKINGS",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            items(
                                items = remainingUsers,
                                key = { user -> user.uid }
                            ) { user ->
                                LeaderboardRow(
                                    user = user,
                                    isCurrentUser = user.uid == currentUid,
                                    isStreak = isStreakTab
                                )
                            }
                        }

                        val currentUserInTop100 = users.any { it.uid == currentUid }
                        if (!currentUserInTop100 && currentUid.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = NeutralCardBg),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Text(
                                        text = if (isStreakTab) "Your account is not in the Top 100 streak ranking yet." else "Your account is not in the Top 100 yet.",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Current User's Sticky Bottom Card (Nền xanh nhạt chữ đậm cực kỳ rõ nét)
            if (!isLoading && errorMessage == null && currentUid.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SoftBlueBg
                    ),
                    border = BorderStroke(1.5.dp, BorderBlue),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "YOUR POSITION",
                                style = MaterialTheme.typography.labelMedium,
                                color = BrandBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentUser?.name ?: "You",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentUser?.let { "Level ${it.level}" } ?: "Not in Top 100",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = currentUser?.let { "#${it.rank}" } ?: "--",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                            Text(
                                text = currentUser?.let {
                                    if (isStreakTab) "🔥 ${it.dayStreak} days" else "${it.totalXp} XP"
                                } ?: "Outside Top 100",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) BrandBlue else Color.Transparent
    val contentColor = if (selected) Color.White else TextSecondary

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

@Composable
private fun TopUserCard(
    user: LeaderboardUser,
    currentUid: String,
    isStreak: Boolean,
    modifier: Modifier = Modifier
) {
    val isFirst = user.rank == 1
    val isCurrent = user.uid == currentUid
    val medal = when (user.rank) {
        1 -> "🥇"
        2 -> "🥈"
        else -> "🥉"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            // Thẻ của bạn dùng màu xanh nhạt giống card ở Home, người khác dùng xám nhạt
            containerColor = if (isCurrent) SoftBlueBg else NeutralCardBg
        ),
        border = if (isCurrent) BorderStroke(1.5.dp, BorderBlue) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFirst) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (isFirst) 18.dp else 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = medal,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            UserAvatar(
                name = user.name,
                size = if (isFirst) 50.dp else 40.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isCurrent) {
                Text(
                    text = "You",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Lvl ${user.level}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isStreak) "🔥 ${user.dayStreak}" else "${user.totalXp} XP",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = BrandBlue
            )
        }
    }
}

@Composable
private fun LeaderboardRow(
    user: LeaderboardUser,
    isCurrentUser: Boolean,
    isStreak: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) SoftBlueBg else NeutralCardBg
        ),
        border = if (isCurrentUser) BorderStroke(1.dp, BorderBlue) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${user.rank}",
                modifier = Modifier.width(40.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentUser) BrandBlue else TextPrimary
            )

            UserAvatar(
                name = user.name,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isCurrentUser) "${user.name} (You)" else user.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Level ${user.level}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Text(
                text = if (isStreak) "🔥 ${user.dayStreak} days" else "${user.totalXp} XP",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = BrandBlue
            )
        }
    }
}

@Composable
private fun UserAvatar(
    name: String,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(BrandBlue),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "U",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}