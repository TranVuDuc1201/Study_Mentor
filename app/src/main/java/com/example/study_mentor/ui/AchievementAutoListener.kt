package com.example.study_mentor.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.study_mentor.data.UserStatisticsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun AchievementAutoListener() {

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val coroutineScope = rememberCoroutineScope()
    val statisticsRepository = remember {
        UserStatisticsRepository()
    }

    var uid by remember {
        mutableStateOf(auth.currentUser?.uid)
    }

    // Theo dõi trạng thái đăng nhập / đăng xuất.
    DisposableEffect(auth) {

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            uid = firebaseAuth.currentUser?.uid
        }

        auth.addAuthStateListener(authListener)

        onDispose {
            auth.removeAuthStateListener(authListener)
        }
    }

    // Mỗi tài khoản có một listener riêng.
    DisposableEffect(uid) {

        if (uid == null) {
            onDispose { }
        } else {

            val registration = db.collection("users")
                .document(uid!!)
                .collection("statistics")
                .document("summary")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        Log.e(
                            "AchievementListener",
                            "Failed to observe statistics",
                            error
                        )
                        return@addSnapshotListener
                    }

                    if (snapshot?.exists() == true) {

                        coroutineScope.launch {
                            try {
                                statisticsRepository.checkAchievements()

                                Log.d(
                                    "AchievementListener",
                                    "Achievement check completed"
                                )

                            } catch (e: Exception) {
                                Log.e(
                                    "AchievementListener",
                                    "Failed to check achievements",
                                    e
                                )
                            }
                        }
                    }
                }

            onDispose {
                registration.remove()
            }
        }
    }
}