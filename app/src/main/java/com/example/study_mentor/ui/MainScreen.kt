package com.example.study_mentor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study_mentor.ui.theme.PrimaryBlue

@Composable
fun MainScreen(
    authViewModel: AuthViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isAskAi = currentRoute == "ask_ai"
    val chatUiState by chatViewModel.uiState.collectAsState()

    val hideBottomBarRoutes = listOf("login", "register")
    val shouldShowBottomBar = currentRoute !in hideBottomBarRoutes

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                BottomBarWithFab(
                    navController = navController,
                    chatViewModel = chatViewModel
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {

            composable("home") {
                HomeScreen(
                    onNavigateToLogin = {
                        navController.navigate("login")
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    },
                    onNavigateToAskAI = {
                        navController.navigate("ask_ai")
                    },
                    onQuestionSelected = { question ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("suggested_question", question)

                        navController.navigate("ask_ai")
                    }
                )
            }
            composable("practice") { PracticeScreen() }

            composable("ask_ai") { backStackEntry ->
                val previousEntry = navController.previousBackStackEntry

                val suggestedQuestion =
                    previousEntry?.savedStateHandle
                        ?.get<String>("suggested_question") ?: ""

                CameraScannerScreen(
                    onBackClick = { navController.popBackStack() },
                    chatViewModel = chatViewModel,
                    initialQuestion = suggestedQuestion
                )
            }
            composable("library") { 
                LibraryScreen(
                    authViewModel = authViewModel,
                    libraryViewModel = libraryViewModel,
                    onSessionClick = { sessionId ->
                        chatViewModel.loadSession(sessionId)
                        navController.navigate("ask_ai") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate("login")
                    }
                ) 
            }
            composable("profile") {
                ProfileScreen(
                    onNavigateBack = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate("login") {
                            popUpTo("profile") {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    viewModel = authViewModel
                )
            }
            composable("login") {
                LoginScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRegister = { 
                        navController.navigate("register") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onAuthSuccess = {
                        navController.navigate("profile") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    viewModel = authViewModel
                )
            }
            composable("register") {
                RegisterScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = { 
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onAuthSuccess = {
                        navController.navigate("profile") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    viewModel = authViewModel
                )
            }
        }
    }
}

@Composable
fun BottomBarWithFab(
    navController: NavHostController,
    chatViewModel: ChatViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isAskAi = currentRoute == "ask_ai"
    val chatUiState by chatViewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // The actual Bottom Navigation Bar
        NavigationBar(
            containerColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .shadow(
                    elevation = 16.dp, 
                    spotColor = Color.Black.copy(alpha = 0.1f), 
                    ambientColor = Color.Black.copy(alpha = 0.1f)
                ),
            tonalElevation = 0.dp // Ensure it's pure white without surface tint
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home", fontWeight = FontWeight.Medium) },
                selected = currentRoute == "home",
                onClick = {
                    if (currentRoute != "home") {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Create, contentDescription = "Practice") },
                label = { Text("Practice", fontWeight = FontWeight.Medium) },
                selected = currentRoute == "practice",
                onClick = {
                    if (currentRoute != "practice") {
                        navController.navigate("practice") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
            // Center empty space for FAB and Label
            NavigationBarItem(
                icon = { Box(modifier = Modifier.height(24.dp)) }, // Placeholder space for the floating icon above it
                label = { 
                    Text(
                        text = if (isAskAi) "Send" else "Ask AI", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Medium
                    ) 
                },
                selected = isAskAi,
                onClick = {
                    if (isAskAi) {
                        if (chatUiState.messages.isEmpty() && chatUiState.selectedImageUri == null) {
                            chatViewModel.triggerCapture()
                        } else {
                            chatViewModel.sendMessage()
                        }
                    } else {
                        navController.navigate("ask_ai") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    unselectedIconColor = Color.Transparent, // Hidden behind FAB
                    unselectedTextColor = PrimaryBlue, // Keep it blue to indicate the primary action
                    indicatorColor = Color.Transparent
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Library") },
                label = { Text("Library", fontWeight = FontWeight.Medium) },
                selected = currentRoute == "library",
                onClick = {
                    if (currentRoute != "library") {
                        navController.navigate("library") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile", fontWeight = FontWeight.Medium) },
                selected = currentRoute == "profile",
                onClick = {
                    if (currentRoute != "profile") {
                        navController.navigate("profile") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }

        // The overlapping FAB
        FloatingActionButton(
            onClick = {
                if (isAskAi) {
                    if (chatUiState.messages.isEmpty() && chatUiState.selectedImageUri == null) {
                        chatViewModel.triggerCapture()
                    } else {
                        chatViewModel.sendMessage()
                    }
                } else {
                    navController.navigate("ask_ai") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            },
            containerColor = PrimaryBlue,
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp) // Adjust to overlap the top edge of the NavigationBar
                .size(64.dp)
        ) {
            Icon(
                imageVector = if (isAskAi) Icons.AutoMirrored.Filled.Send else Icons.Default.AutoAwesome,
                contentDescription = if (isAskAi) "Send / Solve" else "Ask AI",
                modifier = Modifier.size(if (isAskAi) 24.dp else 32.dp)
            )
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
