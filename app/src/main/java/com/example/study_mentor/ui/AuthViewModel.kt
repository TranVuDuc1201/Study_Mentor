package com.example.study_mentor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUser: FirebaseUser? = null,
    val infoMessage: String? = null
)

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _authState = MutableStateFlow(AuthState(currentUser = auth.currentUser))
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _authState.value = _authState.value.copy(currentUser = firebaseAuth.currentUser)
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun login(email: String, password: String) {
        if (_authState.value.isLoading) return
        val emailError = AuthValidation.emailError(email)
        val passwordError = AuthValidation.loginPasswordError(password)
        if (emailError != null || passwordError != null) {
            setError(emailError ?: passwordError!!)
            return
        }
        viewModelScope.launch {
            setLoading()
            try {
                // 1. Đăng nhập qua Firebase Authentication
                val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
                val user = result.user

                _authState.value = _authState.value.copy(
                    isLoading = false, currentUser = user, errorMessage = null
                )

                // 2. TỰ ĐỘNG KIỂM TRA VÀ ĐỒNG BỘ DỮ LIỆU SANG FIRESTORE CHO CẢ USER CŨ LẪN MỚI
                if (user != null) {
                    val userDocRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.uid)

                    try {
                        val snapshot = userDocRef.get().await()
                        // Nếu document chưa tồn tại HOẶC chưa có trường "name"
                        val updates = mutableMapOf<String, Any>()

                        if (!snapshot.contains("name")) {
                            updates["name"] =
                                user.displayName?.takeIf { it.isNotBlank() }
                                    ?: user.email?.substringBefore("@")
                                            ?: "User"
                        }

                        if (!snapshot.contains("email")) {
                            updates["email"] = user.email ?: email.trim()
                        }

                        if (!snapshot.contains("totalXp")) {
                            updates["totalXp"] = 0L
                        }

                        if (!snapshot.contains("level")) {
                            updates["level"] = 1
                        }

                        if (updates.isNotEmpty()) {
                            userDocRef.set(
                                updates,
                                com.google.firebase.firestore.SetOptions.merge()
                            ).await()
                        }
                    } catch (firestoreError: Exception) {
                        // Bỏ qua lỗi ngầm của Firestore để không làm gián đoạn luồng đăng nhập chính của người dùng
                    }
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setError(friendlyError(e))
            }
        }
    }

    fun register(email: String, password: String, name: String) {
        if (_authState.value.isLoading) return
        val error = AuthValidation.nameError(name)
            ?: AuthValidation.emailError(email)
            ?: AuthValidation.registrationPasswordError(password)
        if (error != null) {
            setError(error)
            return
        }
        viewModelScope.launch {
            setLoading()
            try {
                // 1. Tạo tài khoản trên Firebase Authentication
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val user = result.user

                _authState.value = _authState.value.copy(
                    isLoading = false, currentUser = user, errorMessage = null
                )

                if (user != null) {
                    // 2. Cập nhật display name lên Firebase Auth profile
                    val profile = UserProfileChangeRequest.Builder()
                        .setDisplayName(name.trim()).build()

                    try {
                        user.updateProfile(profile).await()
                        user.sendEmailVerification().await()

                        // 3. QUAN TRỌNG: Lưu thông tin chi tiết (gồm name, email, level)
                        // vào Firestore tại collection "users/{uid}" để làm bảng xếp hạng và quản lý thông tin
                        val userDocData = hashMapOf(
                            "name" to name.trim(),
                            "email" to email.trim(),
                            "level" to 1,          // Khởi tạo level mặc định là 1
                            "totalXp" to 0L        // Khởi tạo XP mặc định là 0
                        )

                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.uid)
                            .set(userDocData, com.google.firebase.firestore.SetOptions.merge())
                            .await()

                        _authState.value = _authState.value.copy(
                            infoMessage = "Account created. Check your inbox to verify your email."
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Nếu lỗi phụ (như gửi mail xác thực), tài khoản vẫn tạo thành công
                        _authState.value = _authState.value.copy(
                            infoMessage = "Account created. You can verify your email later."
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setError(friendlyError(e))
            }
        }
    }

    fun resetPassword(email: String) {
        if (_authState.value.isLoading) return
        val error = AuthValidation.emailError(email)
        if (error != null) {
            setError(error)
            return
        }
        viewModelScope.launch {
            setLoading()
            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    infoMessage = "If an account exists, a reset email will be sent."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setError(friendlyError(e))
            }
        }
    }

    fun resendVerificationEmail() {
        if (_authState.value.isLoading) return
        val user = auth.currentUser ?: run {
            setError("Please sign in first")
            return
        }
        if (user.isEmailVerified) {
            _authState.value = _authState.value.copy(infoMessage = "Email already verified")
            return
        }
        viewModelScope.launch {
            setLoading()
            try {
                user.sendEmailVerification().await()
                _authState.value = _authState.value.copy(
                    isLoading = false, infoMessage = "Verification email sent"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setError(friendlyError(e))
            }
        }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState()
    }

    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null, infoMessage = null)
    }

    private fun setLoading() {
        _authState.value = _authState.value.copy(
            isLoading = true, errorMessage = null, infoMessage = null
        )
    }

    private fun setError(message: String) {
        _authState.value = _authState.value.copy(isLoading = false, errorMessage = message)
    }

    private fun friendlyError(error: Exception): String = when (error) {
        is FirebaseAuthUserCollisionException ->
            "This email is already registered"

        is FirebaseNetworkException ->
            "Network unavailable. Check your connection"

        is FirebaseAuthInvalidCredentialsException ->
            "Invalid email or password"

        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_TOO_MANY_REQUESTS" ->
                "Too many attempts. Please try again later"

            "ERROR_INVALID_CREDENTIAL",
            "ERROR_WRONG_PASSWORD",
            "ERROR_USER_NOT_FOUND" ->
                "Invalid email or password"

            "ERROR_WEAK_PASSWORD" ->
                "Choose a stronger password"

            else ->
                "Authentication failed. Please try again"
        }

        else ->
            "Something went wrong. Please try again"
    }
}