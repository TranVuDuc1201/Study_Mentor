
package com.example.study_mentor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.study_mentor.data.Achievement

@Composable
fun AchievementUnlockListener() {

    val pendingAchievements = remember {
        mutableStateListOf<Achievement>()
    }

    LaunchedEffect(Unit) {

        AchievementEventManager.events.collect { achievement ->

            pendingAchievements.add(achievement)

        }
    }

    val currentAchievement = pendingAchievements.firstOrNull()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        if (currentAchievement != null) {

            AchievementBanner(
                achievement = currentAchievement,
                onDismiss = {

                    if (pendingAchievements.isNotEmpty()) {
                        pendingAchievements.removeAt(0)
                    }

                }
            )
        }
    }
}
