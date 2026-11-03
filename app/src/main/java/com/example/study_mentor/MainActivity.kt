package com.example.study_mentor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.example.study_mentor.ui.MainScreen
import com.example.study_mentor.ui.theme.Study_MentorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure system windows do not fit decor so Compose handles IME and navigation insets
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            Study_MentorTheme {
                MainScreen()
            }
        }
    }
}