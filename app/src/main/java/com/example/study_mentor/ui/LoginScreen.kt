package com.example.study_mentor.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study_mentor.ui.theme.PrimaryBlue

private val AuthBackground = Color(0xFFF4F7FD)
private val AuthText = Color(0xFF172642)
private val AuthMuted = Color(0xFF64748B)

@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    val state by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val emailError = if (submitted) AuthValidation.emailError(email) else null
    val passwordError = if (submitted) AuthValidation.loginPasswordError(password) else null

    LaunchedEffect(state.currentUser) {
        if (state.currentUser != null) onAuthSuccess()
    }
    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(AuthBackground)
            .imePadding().verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(PrimaryBlue, Color(0xFF645BDA)))
            ).padding(start = 16.dp, end = 24.dp, top = 18.dp, bottom = 34.dp)
        ) {
            Column {
                IconButton(onClick = onNavigateBack, enabled = !state.isLoading) {
                    Icon(Icons.Default.ArrowBack, "Go back", tint = Color.White)
                }
                Spacer(Modifier.height(16.dp))
                Text("STUDY MENTOR", color = Color.White.copy(alpha = .85f),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text("Welcome back!", color = Color.White,
                    fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Sign in to pick up where you left off.",
                    color = Color.White.copy(alpha = .9f), fontSize = 15.sp)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                .offset(y = (-14).dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Text("Log in", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = AuthText)
                Spacer(Modifier.height(4.dp))
                Text("Enter your account details below", color = AuthMuted, fontSize = 14.sp)
                Spacer(Modifier.height(22.dp))
                state.errorMessage?.let {
                    Surface(color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp)) {
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.fillMaxWidth().padding(12.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; viewModel.clearError() },
                    label = { Text("Email address") },
                    placeholder = { Text("you@example.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    isError = emailError != null,
                    supportingText = { emailError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    singleLine = true, enabled = !state.isLoading,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; viewModel.clearError() },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password")
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    isError = passwordError != null,
                    supportingText = { passwordError?.let { Text(it) } },
                    singleLine = true, enabled = !state.isLoading,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { viewModel.clearError(); showResetDialog = true },
                    enabled = !state.isLoading, modifier = Modifier.align(Alignment.End)) {
                    Text("Forgot password?", color = PrimaryBlue)
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        submitted = true
                        if (AuthValidation.emailError(email) == null &&
                            AuthValidation.loginPasswordError(password) == null) {
                            viewModel.login(email, password)
                        }
                    },
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Log in", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("New to Study Mentor? ", color = AuthMuted)
            Text("Create account", color = PrimaryBlue, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = !state.isLoading) { onNavigateToRegister() })
        }
        Text("Learn smarter, every day.", color = AuthMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp))
    }

    if (showResetDialog) {
        var resetEmail by rememberSaveable { mutableStateOf(email) }
        var resetSubmitted by rememberSaveable { mutableStateOf(false) }
        val resetError = if (resetSubmitted) AuthValidation.emailError(resetEmail) else null
        AlertDialog(
            onDismissRequest = { if (!state.isLoading) showResetDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("Reset your password") },
            text = {
                Column {
                    Text("Enter your email address and we'll send a password reset link.")
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = resetEmail, onValueChange = { resetEmail = it },
                        label = { Text("Email address") }, singleLine = true,
                        isError = resetError != null,
                        supportingText = { resetError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = !state.isLoading, onClick = {
                    resetSubmitted = true
                    if (AuthValidation.emailError(resetEmail) == null) {
                        viewModel.resetPassword(resetEmail)
                        showResetDialog = false
                    }
                }) { Text("Send reset link") }
            },
            dismissButton = {
                TextButton(enabled = !state.isLoading,
                    onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}
