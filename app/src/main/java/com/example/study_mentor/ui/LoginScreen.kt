package com.example.study_mentor.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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

private val AuthBackground = Color(0xFFF8FAFC)
private val AuthText = Color(0xFF0F172A)
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
        modifier = Modifier
            .fillMaxSize()
            .background(AuthBackground)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Modern Immersive Gradient Header with Floating Icon Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PrimaryBlue, Color(0xFF4F46E5), Color(0xFF6366F1))
                    )
                )
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 48.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, "Go back", tint = Color.White)
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AI Study Mentor", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = "Welcome Back! 👋",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Sign in to continue your intelligent learning journey.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }

        // Floating Card with inputs
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-28.dp))
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp), spotColor = Color(0x22000000)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Sign In",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuthText
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Enter your credentials to access your account",
                    color = AuthMuted,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(24.dp))

                state.errorMessage?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; viewModel.clearError() },
                    label = { Text("Email address") },
                    placeholder = { Text("you@example.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue) },
                    isError = emailError != null,
                    supportingText = { emailError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    singleLine = true,
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        disabledContainerColor = Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; viewModel.clearError() },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = AuthMuted
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    isError = passwordError != null,
                    supportingText = { passwordError?.let { Text(it) } },
                    singleLine = true,
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        disabledContainerColor = Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { viewModel.clearError(); showResetDialog = true },
                        enabled = !state.isLoading,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text("Forgot password?", color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(8.dp))

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
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
                    } else {
                        Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account? ", color = AuthMuted)
            Text(
                "Create account",
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = !state.isLoading) { onNavigateToRegister() }
            )
        }
    }

    if (showResetDialog) {
        var resetEmail by rememberSaveable { mutableStateOf(email) }
        var resetSubmitted by rememberSaveable { mutableStateOf(false) }
        val resetError = if (resetSubmitted) AuthValidation.emailError(resetEmail) else null
        AlertDialog(
            onDismissRequest = { if (!state.isLoading) showResetDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your account email address and we'll send you a password reset link.", color = AuthMuted)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email address") },
                        singleLine = true,
                        isError = resetError != null,
                        supportingText = { resetError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !state.isLoading,
                    onClick = {
                        resetSubmitted = true
                        if (AuthValidation.emailError(resetEmail) == null) {
                            viewModel.resetPassword(resetEmail)
                            showResetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send link")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isLoading,
                    onClick = { showResetDialog = false }
                ) {
                    Text("Cancel", color = AuthMuted)
                }
            }
        )
    }
}
