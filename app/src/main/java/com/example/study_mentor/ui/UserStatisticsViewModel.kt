package com.example.study_mentor.ui


import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UserStatisticsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _dayStreak = MutableStateFlow(0)
    val dayStreak: StateFlow<Int> = _dayStreak.asStateFlow()

    private val _totalQuestions = MutableStateFlow(0)
    val totalQuestions: StateFlow<Int> = _totalQuestions.asStateFlow()

    // Lắng nghe dữ liệu real-time từ Firestore của user hiện tại
    fun observeUserStatistics(uid: String?) {
        if (uid == null) {
            _dayStreak.value = 0
            _totalQuestions.value = 0
            return
        }

        db.collection("users")
            .document(uid)
            .collection("statistics")
            .document("summary")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val streak = snapshot.getLong("dayStreak")?.toInt() ?: 0
                    val questions = snapshot.getLong("totalQuestionsAsked")?.toInt() ?: 0

                    _dayStreak.update { streak }
                    _totalQuestions.update { questions }
                }
            }
    }
}