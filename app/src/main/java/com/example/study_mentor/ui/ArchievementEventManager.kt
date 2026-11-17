
package com.example.study_mentor.ui

import com.example.study_mentor.data.Achievement
import com.example.study_mentor.data.AchievementDefinitions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AchievementEventManager {

    private val _events = MutableSharedFlow<Achievement>(
        replay = 10,
        extraBufferCapacity = 10
    )

    val events = _events.asSharedFlow()

    fun notifyUnlocked(achievementIds: List<String>) {

        val definitions = AchievementDefinitions.achievements
            .associateBy { it.id }

        achievementIds.forEach { id ->

            definitions[id]?.let { achievement ->

                _events.tryEmit(achievement)

            }
        }
    }
}
