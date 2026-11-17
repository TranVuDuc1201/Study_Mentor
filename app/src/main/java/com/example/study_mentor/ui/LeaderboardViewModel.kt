
package com.example.study_mentor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_mentor.data.LeaderboardRepository
import com.example.study_mentor.data.LeaderboardUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    // XP leaderboard
    val isLoading: Boolean = true,
    val users: List<LeaderboardUser> = emptyList(),
    val errorMessage: String? = null,

    // Streak leaderboard
    val isStreakLoading: Boolean = true,
    val streakUsers: List<LeaderboardUser> = emptyList(),
    val streakErrorMessage: String? = null
)

class LeaderboardViewModel(
    private val repository: LeaderboardRepository =
        LeaderboardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())

    val uiState: StateFlow<LeaderboardUiState> =
        _uiState.asStateFlow()

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard() {

        // Load XP leaderboard
        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            repository.observeLeaderboard()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage
                            ?: "Unable to load XP leaderboard"
                    )
                }
                .collect { users ->

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        users = users,
                        errorMessage = null
                    )
                }
        }

        // Load Streak leaderboard
        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isStreakLoading = true,
                streakErrorMessage = null
            )

            repository.observeStreakLeaderboard()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isStreakLoading = false,
                        streakErrorMessage = error.localizedMessage
                            ?: "Unable to load streak leaderboard"
                    )
                }
                .collect { users ->

                    _uiState.value = _uiState.value.copy(
                        isStreakLoading = false,
                        streakUsers = users,
                        streakErrorMessage = null
                    )
                }
        }
    }
}
