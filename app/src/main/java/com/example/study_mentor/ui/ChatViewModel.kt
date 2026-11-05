package com.example.study_mentor.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_mentor.OpenAIApiClient
import com.example.study_mentor.data.ChatMessage
import com.example.study_mentor.data.ChatSession
import com.example.study_mentor.data.FirestoreChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

// Expose typealias for backward compatibility if referenced from UI package
typealias ChatMessage = com.example.study_mentor.data.ChatMessage

// ─── UI State ───────────────────────────────────────────────────────────────

/**
 * The complete UI state for the chat screen.
 * @param messages            Ordered list of chat messages (oldest first).
 * @param isLoading           True while awaiting the initial AI response token.
 * @param errorMessage        Non-null when the last API call encountered an error.
 * @param selectedImageUri    URI of the image chosen from gallery for preview.
 * @param selectedImageBase64 Compressed Base64 representation of the selected image.
 * @param currentSessionId    ID of the active Firestore chat session, or null if new.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedImageUri: Uri? = null,
    val selectedImageBase64: String? = null,
    val currentSessionId: String? = null,
    val inputText: String = ""
)

// ─── ViewModel ───────────────────────────────────────────────────────────────

class ChatViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val repository: FirestoreChatRepository = FirestoreChatRepository()
) : ViewModel() {

    companion object {
        const val DEFAULT_IMAGE_PROMPT = "Solve this math problem step-by-step and explain the concepts."
    }

    private val _captureRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val captureRequest: SharedFlow<Unit> = _captureRequest.asSharedFlow()

    /** Triggers CameraX ImageCapture in the scanner screen from external controls (like the center FAB). */
    fun triggerCapture() {
        _captureRequest.tryEmit(Unit)
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var isStreaming = false
    private var compressionJob: kotlinx.coroutines.Job? = null

    /**
     * Updates the draft text typed by the user in the input field.
     */
    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * Loads an existing chat session's messages from Firestore.
     */
    fun loadSession(sessionId: String) {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(currentSessionId = sessionId, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.getMessagesForSession(uid, sessionId).collect { messages ->
                _uiState.update { it.copy(messages = messages, isLoading = false) }
            }
        }
    }

    /**
     * Resets the active session so the next message initiates a fresh session.
     */
    fun startNewChat() {
        _uiState.update {
            it.copy(
                currentSessionId = null,
                messages = emptyList(),
                errorMessage = null,
                selectedImageUri = null,
                selectedImageBase64 = null,
                inputText = ""
            )
        }
    }

    /**
     * Handles gallery image selection. Decodes the image, downscales it to max 1024x1024,
     * compresses it as JPEG at 80% quality, and stores its Base64 string on Dispatchers.IO.
     */
    fun onImageSelected(uri: Uri?, context: Context) {
        if (uri == null) {
            clearSelectedImage()
            return
        }

        // Show thumbnail immediately in UI while compression runs in background
        _uiState.update { it.copy(selectedImageUri = uri, selectedImageBase64 = null) }

        compressionJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val base64 = processAndCompressImage(context, uri)
                _uiState.update { it.copy(selectedImageBase64 = base64) }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to compress and encode image", e)
            }
        }
    }

    /** Resets the selected image URI and Base64 string. */
    fun clearSelectedImage() {
        _uiState.update { it.copy(selectedImageUri = null, selectedImageBase64 = null) }
    }

    /**
     * Safely reads, downsamples, scales (max 1024x1024), and compresses an image to JPEG 80%.
     */
    fun processAndCompressImage(context: Context, uri: Uri): String? {
        val resolver = context.contentResolver

        // 1. Decode bounds first to calculate inSampleSize and prevent OutOfMemory (OOM)
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) return null

        val maxDim = 1024
        var inSampleSize = 1
        if (rawHeight > maxDim || rawWidth > maxDim) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while ((halfHeight / inSampleSize) >= maxDim && (halfWidth / inSampleSize) >= maxDim) {
                inSampleSize *= 2
            }
        }

        // 2. Decode the downsampled bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val sampledBitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        // 3. Scale proportionally so max(width, height) <= 1024
        val currentWidth = sampledBitmap.width
        val currentHeight = sampledBitmap.height
        val longestSide = maxOf(currentWidth, currentHeight)

        val finalBitmap = if (longestSide > maxDim) {
            val scale = maxDim.toFloat() / longestSide
            val targetWidth = (currentWidth * scale).toInt()
            val targetHeight = (currentHeight * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(sampledBitmap, targetWidth, targetHeight, true)
            if (scaled != sampledBitmap) {
                sampledBitmap.recycle()
            }
            scaled
        } else {
            sampledBitmap
        }

        // 4. Compress to JPEG at 80% quality
        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        finalBitmap.recycle()

        // 5. Encode byte array to Base64 without line wraps
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Sends a user prompt to the AI and streams the reply token-by-token
     * into the UI state in real time, persisting messages to Cloud Firestore.
     *
     * @param prompt The question or instruction typed by the user. If left blank or defaulted,
     *               uses the current input text or applies the default prompt if an image is present.
     */
    fun sendMessage(prompt: String = _uiState.value.inputText) {
        val hasImage = _uiState.value.selectedImageUri != null || _uiState.value.selectedImageBase64 != null
        val cachedBase64 = _uiState.value.selectedImageBase64
        val pendingCompression = compressionJob
        var trimmedPrompt = prompt.trim()

        // "Lazy User" Default Prompt Logic:
        // If an image is captured/selected but prompt text is blank, assign the default prompt
        if (trimmedPrompt.isBlank() && hasImage) {
            trimmedPrompt = DEFAULT_IMAGE_PROMPT
        }

        if ((trimmedPrompt.isBlank() && !hasImage) || isStreaming) return

        // Clear thumbnail and active input text immediately from UI
        clearSelectedImage()
        setInputText("")

        // Optional UI tweak: Indicate in chat history that an image was attached
        val bubbleText = if (hasImage) {
            if (trimmedPrompt.isNotBlank()) "[Image attached]\n$trimmedPrompt" else "[Image attached]"
        } else {
            trimmedPrompt
        }

        // 1. Immediately append the user message and start the thinking indicator
        val userMessage = ChatMessage(
            role = "user",
            text = bubbleText,
            timestamp = System.currentTimeMillis()
        )
        _uiState.update { current ->
            current.copy(
                messages = current.messages + userMessage,
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            isStreaming = true
            var isFirstChunk = true
            val responseBuilder = StringBuilder()
            val uid = auth.currentUser?.uid

            // Await any pending background compression to guarantee Base64 is ready
            pendingCompression?.join()
            val base64ToSend = cachedBase64 ?: _uiState.value.selectedImageBase64
            if (_uiState.value.selectedImageBase64 != null) {
                _uiState.update { it.copy(selectedImageBase64 = null) }
            }
            Log.d("ChatViewModel", "sendMessage: hasImage=$hasImage, base64ToSend=${base64ToSend != null}")

            // Auto-create a session in Firestore if currentSessionId is null
            var activeSessionId = _uiState.value.currentSessionId
            if (activeSessionId == null && uid != null) {
                try {
                    val sessionTitle = if (trimmedPrompt.isNotBlank()) {
                        trimmedPrompt.split("\\s+".toRegex()).take(5).joinToString(" ")
                    } else {
                        "New Image Chat"
                    }
                    activeSessionId = repository.createNewSession(uid, sessionTitle)
                    _uiState.update { it.copy(currentSessionId = activeSessionId) }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Failed to create session in Firestore", e)
                }
            }

            // Save user message to Firestore
            if (uid != null && activeSessionId != null) {
                try {
                    repository.saveMessage(uid, activeSessionId, userMessage)
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Failed to save user message to Firestore", e)
                }
            }

            try {
                // 2. Collect streaming text chunks directly as Flow<String> (with optional image)
                OpenAIApiClient.askQuestion(trimmedPrompt, base64ToSend)
                    .collect { chunkText ->
                        // Strip raw Markdown headers (#), asterisks (*), and LaTeX delimiters (\(, \), \[, \])
                        val cleanDelta = chunkText.replace(Regex("[#*\\[\\]]|\\\\\\(|\\\\\\)|\\\\\\[|\\\\\\]"), "")
                        if (cleanDelta.isEmpty()) return@collect

                        responseBuilder.append(cleanDelta)
                        val accumulatedText = responseBuilder.toString()

                        if (isFirstChunk) {
                            isFirstChunk = false
                            // First token received: dismiss the thinking spinner and append the AI bubble
                            val aiMessage = ChatMessage(
                                role = "ai",
                                text = accumulatedText,
                                timestamp = System.currentTimeMillis()
                            )
                            _uiState.update { current ->
                                current.copy(
                                    messages = current.messages + aiMessage,
                                    isLoading = false
                                )
                            }
                        } else {
                            // Subsequent chunks: update the AI bubble text character-by-character
                            _uiState.update { current ->
                                if (current.messages.isEmpty()) return@update current
                                val updatedList = current.messages.toMutableList()
                                val lastIndex = updatedList.lastIndex
                                updatedList[lastIndex] = updatedList[lastIndex].copy(text = accumulatedText)
                                current.copy(messages = updatedList)
                            }
                        }
                    }

                // If the stream finished without emitting any tokens
                if (isFirstChunk) {
                    val emptyNotice = ChatMessage(
                        role = "ai",
                        text = "Sorry, I received an empty response. Please try again.",
                        timestamp = System.currentTimeMillis()
                    )
                    _uiState.update { current ->
                        current.copy(
                            messages = current.messages + emptyNotice,
                            isLoading = false
                        )
                    }
                } else {
                    // 3. Save final AI message to Firestore
                    val finalAiText = responseBuilder.toString()
                    if (finalAiText.isNotBlank() && uid != null && activeSessionId != null) {
                        try {
                            val finalAiMessage = ChatMessage(
                                role = "ai",
                                text = finalAiText,
                                timestamp = System.currentTimeMillis()
                            )
                            repository.saveMessage(uid, activeSessionId, finalAiMessage)
                            incrementQuestionCount(uid)
                        } catch (e: Exception) {
                            Log.e("ChatViewModel", "Failed to save AI message to Firestore", e)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("OpenAI_Debug", "Streaming Error", e)
                val errorText = "Debug Error: ${e.message ?: "Connection failed"}"
                _uiState.update { current ->
                    if (isFirstChunk) {
                        current.copy(
                            messages = current.messages + ChatMessage(role = "ai", text = errorText),
                            isLoading = false,
                            errorMessage = errorText
                        )
                    } else {
                        current.copy(
                            isLoading = false,
                            errorMessage = errorText
                        )
                    }
                }
            } finally {
                isStreaming = false
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * One-Tap Capture & Dispatch: Immediately sends a captured/cropped image along with an
     * evaluated prompt to the AI and streams the response. If prompt is blank, automatically
     * uses DEFAULT_IMAGE_PROMPT ("Solve this math problem step-by-step and explain the concepts.").
     */
    fun sendImageAndPrompt(uri: Uri, context: Context, prompt: String? = null) {
        val effectivePrompt = when {
            !prompt.isNullOrBlank() -> prompt.trim()
            _uiState.value.inputText.isNotBlank() -> _uiState.value.inputText.trim()
            else -> DEFAULT_IMAGE_PROMPT
        }

        if (isStreaming) return

        clearSelectedImage()
        setInputText("")

        val bubbleText = "[Image attached]\n$effectivePrompt"
        val userMessage = ChatMessage(
            role = "user",
            text = bubbleText,
            timestamp = System.currentTimeMillis()
        )
        _uiState.update { current ->
            current.copy(
                messages = current.messages + userMessage,
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            isStreaming = true
            var isFirstChunk = true
            val responseBuilder = StringBuilder()
            val uid = auth.currentUser?.uid

            // Compress the cropped image on IO dispatcher
            val base64ToSend = withContext(Dispatchers.IO) {
                try {
                    processAndCompressImage(context, uri)
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Failed to compress cropped image", e)
                    null
                }
            }
            Log.d("ChatViewModel", "sendImageAndPrompt: base64ToSend ready? ${base64ToSend != null}, length=${base64ToSend?.length ?: 0}")

            var activeSessionId = _uiState.value.currentSessionId
            if (activeSessionId == null && uid != null) {
                try {
                    val sessionTitle = effectivePrompt.split("\\s+".toRegex()).take(5).joinToString(" ")
                    activeSessionId = repository.createNewSession(uid, sessionTitle)
                    _uiState.update { it.copy(currentSessionId = activeSessionId) }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Failed to create session in Firestore", e)
                }
            }

            if (uid != null && activeSessionId != null) {
                try {
                    repository.saveMessage(uid, activeSessionId, userMessage)
                    incrementQuestionCount(uid)
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Failed to save user message to Firestore", e)
                }
            }

            try {
                OpenAIApiClient.askQuestion(effectivePrompt, base64ToSend)
                    .collect { chunkText ->
                        // Strip raw Markdown headers (#), asterisks (*), and LaTeX delimiters (\(, \), \[, \])
                        val cleanDelta = chunkText.replace(Regex("[#*\\[\\]]|\\\\\\(|\\\\\\)|\\\\\\[|\\\\\\]"), "")
                        if (cleanDelta.isEmpty()) return@collect

                        responseBuilder.append(cleanDelta)
                        val accumulatedText = responseBuilder.toString()

                        if (isFirstChunk) {
                            isFirstChunk = false
                            val aiMessage = ChatMessage(
                                role = "ai",
                                text = accumulatedText,
                                timestamp = System.currentTimeMillis()
                            )
                            _uiState.update { current ->
                                current.copy(
                                    messages = current.messages + aiMessage,
                                    isLoading = false
                                )
                            }
                        } else {
                            _uiState.update { current ->
                                if (current.messages.isEmpty()) return@update current
                                val updatedList = current.messages.toMutableList()
                                val lastIndex = updatedList.lastIndex
                                updatedList[lastIndex] = updatedList[lastIndex].copy(text = accumulatedText)
                                current.copy(messages = updatedList)
                            }
                        }
                    }

                if (isFirstChunk) {
                    val emptyNotice = ChatMessage(
                        role = "ai",
                        text = "Sorry, I received an empty response. Please try again.",
                        timestamp = System.currentTimeMillis()
                    )
                    _uiState.update { current ->
                        current.copy(
                            messages = current.messages + emptyNotice,
                            isLoading = false
                        )
                    }
                } else {
                    val finalAiText = responseBuilder.toString()
                    if (finalAiText.isNotBlank() && uid != null && activeSessionId != null) {
                        try {
                            val finalAiMessage = ChatMessage(
                                role = "ai",
                                text = finalAiText,
                                timestamp = System.currentTimeMillis()
                            )
                            repository.saveMessage(uid, activeSessionId, finalAiMessage)
                            incrementQuestionCount(uid)
                        } catch (e: Exception) {
                            Log.e("ChatViewModel", "Failed to save AI message to Firestore", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("OpenAI_Debug", "Streaming Error", e)
                val errorText = "Debug Error: ${e.message ?: "Connection failed"}"
                _uiState.update { current ->
                    if (isFirstChunk) {
                        current.copy(
                            messages = current.messages + ChatMessage(role = "ai", text = errorText),
                            isLoading = false,
                            errorMessage = errorText
                        )
                    } else {
                        current.copy(
                            isLoading = false,
                            errorMessage = errorText
                        )
                    }
                }
            } finally {
                isStreaming = false
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Clears any displayed error message. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Increments the question counter shown on the Profile screen. */
    private fun incrementQuestionCount(uid: String) {
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("stats").document("summary")
            .set(mapOf("totalQuestionsAsked" to FieldValue.increment(1)), SetOptions.merge())
    }
}
