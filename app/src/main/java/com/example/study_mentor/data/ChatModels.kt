package com.example.study_mentor.data

import java.util.UUID

/**
 * Represents a chat conversation session stored in Firestore.
 * Firestore document path: users/{uid}/chat_sessions/{sessionId}
 *
 * @param id Unique ID of the session document.
 * @param title Human-readable title summarizing the conversation topic.
 * @param createdAt Epoch timestamp (ms) when the session was created.
 */
data class ChatSession(
    val id: String = "",
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val bookmarked: Boolean = false
)
/**
 * Represents an individual message in a chat session stored in Firestore.
 * Firestore document path: users/{uid}/chat_sessions/{sessionId}/messages/{messageId}
 *
 * @param id Unique ID of the message.
 * @param role Sender role: either "user" or "ai".
 * @param text The text body of the message.
 * @param timestamp Epoch timestamp (ms) when the message was created.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
