package com.example.study_mentor.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.DisposableEffect
import com.google.firebase.firestore.FirebaseFirestore

private val ProfileBlue = Color(0xFF4263EB)
private val ProfilePurple = Color(0xFF7950F2)
private val ProfileBackground = Color(0xFFF6F7FC)

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val user = authState.currentUser
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var totalXp by remember(user?.uid) {
        mutableLongStateOf(0L)
    }

    var currentLevel by remember(user?.uid) {
        mutableIntStateOf(1)
    }

    DisposableEffect(user?.uid) {
        val uid = user?.uid

        val registration = if (uid != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("statistics")
                .document("summary")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        totalXp = snapshot.getLong("totalXp") ?: 0L
                        currentLevel = snapshot.getLong("level")?.toInt() ?: 1
                    }
                }
        } else {
            null
        }

        onDispose {
            registration?.remove()
        }
    }

    var displayName by rememberSaveable(user?.uid) {
        mutableStateOf(user?.displayName.orEmpty())
    }
    var editingName by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var savingName by remember { mutableStateOf(false) }
    var sendingReset by remember { mutableStateOf(false) }
    var sendingVerification by remember { mutableStateOf(false) }
    var verified by remember(user?.uid) { mutableStateOf(user?.isEmailVerified == true) }
    var shownName by remember(user?.uid) { mutableStateOf(user?.displayName.orEmpty()) }

    LaunchedEffect(user?.uid) {
        if (user == null) onNavigateToLogin()
    }

    val prefs = remember { context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE) }
    var reminderEnabled by remember { mutableStateOf(prefs.getBoolean("reminder_enabled", false)) }
    var reminderHour by remember { mutableStateOf(prefs.getInt("reminder_hour", 8)) }
    var reminderMinute by remember { mutableStateOf(prefs.getInt("reminder_minute", 0)) }
    var showTimePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            prefs.edit().putBoolean("reminder_enabled", true).apply()
            reminderEnabled = true
            StudyReminderManager.scheduleReminder(context, reminderHour, reminderMinute)
            val timeStr = String.format("%02d:%02d", reminderHour, reminderMinute)
            scope.launch { snackbarHostState.showSnackbar("Study reminder enabled for $timeStr daily!") }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Notification permission is required for reminders.") }
        }
    }

    Scaffold(
        containerColor = ProfileBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = Color.White, shadowElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text("My Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(ProfileBlue, ProfilePurple)))
                    .padding(vertical = 30.dp, horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(88.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile avatar",
                            tint = Color.White, modifier = Modifier.size(52.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(shownName.takeIf { it.isNotBlank() } ?: "Study Mentor User",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 23.sp,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(user?.email.orEmpty(), color = Color.White.copy(alpha = 0.90f),
                        fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }

            LevelXpCard(
                totalXp = totalXp,
                level = currentLevel
            )


            Spacer(Modifier.height(22.dp))
            ProfileSectionTitle("ACCOUNT INFORMATION")
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    ProfileDetail("Full name", shownName.ifBlank { "Not set" })
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    ProfileDetail("Email address", user?.email.orEmpty().ifBlank { "Not available" })
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    ProfileDetail("Email verification", if (verified) "Verified" else "Not verified")
                }
            }

            Spacer(Modifier.height(22.dp))
            ProfileSectionTitle("ACCOUNT SETTINGS")
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 5.dp)) {
                    ProfileAction(Icons.Default.Edit, "Edit display name", "Update your public name") {
                        displayName = shownName
                        editingName = true
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 18.dp))
                    ProfileAction(Icons.Default.Lock, "Change password", "Receive a password reset email",
                        enabled = !sendingReset && !user?.email.isNullOrBlank()) {
                        val email = user?.email ?: return@ProfileAction
                        scope.launch {
                            sendingReset = true
                            try {
                                user?.let { current ->
                                    current.reload().await()
                                    current.email?.let { address ->
                                        com.google.firebase.auth.FirebaseAuth.getInstance()
                                            .sendPasswordResetEmail(address).await()
                                    } ?: error("Email address unavailable")
                                } ?: error("Please sign in again")
                                snackbarHostState.showSnackbar("Password reset email requested. Check your inbox.")
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) {
                                snackbarHostState.showSnackbar("Could not send reset email. Please try again.")
                            } finally { sendingReset = false }
                        }
                    }
                    if (!verified) {
                        HorizontalDivider(Modifier.padding(horizontal = 18.dp))
                        ProfileAction(Icons.Default.Email, "Verify email", "Send a verification link",
                            enabled = !sendingVerification) {
                            scope.launch {
                                sendingVerification = true
                                try {
                                    val current = user ?: error("Please sign in again")
                                    current.reload().await()
                                    verified = current.isEmailVerified
                                    if (verified) {
                                        snackbarHostState.showSnackbar("Your email is already verified.")
                                    } else {
                                        current.sendEmailVerification().await()
                                        snackbarHostState.showSnackbar("Verification email sent. Check your inbox.")
                                    }
                                } catch (e: CancellationException) { throw e }
                                catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Could not send verification email.")
                                } finally { sendingVerification = false }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            ProfileSectionTitle("STUDY REMINDER")
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily Study Reminder", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(Modifier.height(2.dp))
                            val timeStr = String.format("%02d:%02d", reminderHour, reminderMinute)
                            Text("Notify at $timeStr daily", color = Color.Gray, fontSize = 13.sp)
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (!hasPermission) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            return@Switch
                                        }
                                    }
                                    reminderEnabled = true
                                    prefs.edit().putBoolean("reminder_enabled", true).apply()
                                    StudyReminderManager.scheduleReminder(context, reminderHour, reminderMinute)
                                    val timeStr = String.format("%02d:%02d", reminderHour, reminderMinute)
                                    scope.launch { snackbarHostState.showSnackbar("Study reminder set for $timeStr daily!") }
                                } else {
                                    reminderEnabled = false
                                    prefs.edit().putBoolean("reminder_enabled", false).apply()
                                    StudyReminderManager.cancelReminder(context)
                                    scope.launch { snackbarHostState.showSnackbar("Study reminder turned off.") }
                                }
                            }
                        )
                    }

                    if (reminderEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    StudyReminderManager.triggerTestNotification(context)
                                    scope.launch { snackbarHostState.showSnackbar("🔔 Test notification sent!") }
                                }
                            ) {
                                Text("🔔 Test Notification", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = { showTimePicker = true }
                            ) {
                                Text("Change time", color = ProfileBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (showTimePicker) {
                        LaunchedEffect(Unit) {
                            android.app.TimePickerDialog(
                                context,
                                { _, h, m ->
                                    reminderHour = h
                                    reminderMinute = m
                                    prefs.edit().putInt("reminder_hour", h).putInt("reminder_minute", m).apply()
                                    if (reminderEnabled) {
                                        StudyReminderManager.scheduleReminder(context, h, m)
                                    }
                                    showTimePicker = false
                                    val timeStr = String.format("%02d:%02d", h, m)
                                    scope.launch { snackbarHostState.showSnackbar("Reminder time updated to $timeStr") }
                                },
                                reminderHour,
                                reminderMinute,
                                true
                            ).apply {
                                setOnCancelListener { showTimePicker = false }
                            }.show()
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Log out", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(20.dp))
            Text("Study Mentor • Keep learning every day", color = Color.Gray,
                style = MaterialTheme.typography.bodySmall)
        }
    }

    if (editingName) {
        AlertDialog(
            onDismissRequest = { if (!savingName) editingName = false },
            title = { Text("Edit display name") },
            text = {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(60) },
                    label = { Text("Full name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !savingName && displayName.trim().length in 2..60,
                    onClick = {
                        scope.launch {
                            savingName = true
                            try {
                                val current = user ?: error("Please sign in again")
                                current.updateProfile(
                                    UserProfileChangeRequest.Builder()
                                        .setDisplayName(displayName.trim()).build()
                                ).await()
                                shownName = displayName.trim()
                                editingName = false
                                snackbarHostState.showSnackbar("Display name updated successfully")
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) {
                                snackbarHostState.showSnackbar("Could not update your name. Please try again.")
                            } finally { savingName = false }
                        }
                    }
                ) { Text(if (savingName) "Saving..." else "Save") }
            },
            dismissButton = {
                TextButton(enabled = !savingName, onClick = { editingName = false }) { Text("Cancel") }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out?") },
            text = { Text("You will need to sign in again to access your account.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    onNavigateToLogin()
                }) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProfileSectionTitle(value: String) {
    Text(value, modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
        color = ProfileBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}

@Composable
fun ProfileDetail(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ProfileAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = ProfileBlue, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = if (enabled) Color.Black else Color.Gray)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}
@Composable
private fun LevelXpCard(
    totalXp: Long,
    level: Int
) {
    // XP cần có để bắt đầu Level hiện tại
    val currentLevelXp =
        25L * (level - 1) * (level + 2)

    // XP cần có để đạt Level tiếp theo
    val nextLevel = level + 1
    val nextLevelXp =
        25L * (nextLevel - 1) * (nextLevel + 2)

    // XP đã tích lũy trong Level hiện tại
    val earnedXp = (totalXp - currentLevelXp)
        .coerceAtLeast(0L)

    val requiredXp = nextLevelXp - currentLevelXp

    val progress = if (requiredXp > 0) {
        (earnedXp.toFloat() / requiredXp.toFloat())
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LEVEL $level",
                        color = ProfileBlue,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Keep learning to level up!",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "⭐",
                    fontSize = 30.sp
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "XP Progress",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Text(
                    text = "$earnedXp / $requiredXp XP",
                    color = ProfileBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50.dp)),
                color = ProfileBlue,
                trackColor = Color(0xFFE8EAF6)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "${(progress * 100).toInt()}% completed • Next: Level $nextLevel",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Text(
                text = "${(requiredXp - earnedXp).coerceAtLeast(0L)} XP remaining",
                color = ProfilePurple,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}
