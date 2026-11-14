package com.example.study_mentor.ui

import android.content.Context
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
                    val context = LocalContext.current
                    val prefs = remember { context.getSharedPreferences("study_prefs", Context.MODE_PRIVATE) }
                    var reminderEnabled by remember { mutableStateOf(prefs.getBoolean("reminder_enabled", false)) }
                    var reminderHour by remember { mutableStateOf(prefs.getInt("reminder_hour", 8)) }
                    var reminderMinute by remember { mutableStateOf(prefs.getInt("reminder_minute", 0)) }
                    var showTimePicker by remember { mutableStateOf(false) }

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
                                reminderEnabled = enabled
                                prefs.edit().putBoolean("reminder_enabled", enabled).apply()
                                val timeStr = String.format("%02d:%02d", reminderHour, reminderMinute)
                                if (enabled) {
                                    StudyReminderManager.scheduleReminder(context, reminderHour, reminderMinute)
                                    scope.launch { snackbarHostState.showSnackbar("Study reminder set for $timeStr daily!") }
                                } else {
                                    StudyReminderManager.cancelReminder(context)
                                    scope.launch { snackbarHostState.showSnackbar("Study reminder turned off.") }
                                }
                            }
                        )
                    }

                    if (reminderEnabled) {
                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Change time", color = ProfileBlue, fontWeight = FontWeight.SemiBold)
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
            text = { Text("Are you sure you want to log out of your account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onNavigateToLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium,
        color = Color.Gray,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun ProfileDetail(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.Black)
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
