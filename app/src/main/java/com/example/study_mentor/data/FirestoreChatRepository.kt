
package com.example.study_mentor.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing chat sessions and message history in Cloud Firestore.
 * Structure: users/{uid}/chat_sessions/{sessionId}/messages/{messageId}
 */
class FirestoreChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Creates a new chat session document under users/{uid}/chat_sessions/
     * and returns the auto-generated session ID.
     */
    suspend fun createNewSession(uid: String, title: String): String {
        val sessionDoc = firestore.collection("users")
            .document(uid)
            .collection("chat_sessions")
            .document()

        val sessionId = sessionDoc.id
        val session = ChatSession(
            id = sessionId,
            title = title,
            createdAt = System.currentTimeMillis()
        )
        sessionDoc.set(session).await()
        return sessionId
    }

    /**
     * Saves a ChatMessage into the subcollection users/{uid}/chat_sessions/{sessionId}/messages/{messageId}.
     */
    suspend fun saveMessage(uid: String, sessionId: String, message: ChatMessage) {
        val messagesCol = firestore.collection("users")
            .document(uid)
            .collection("chat_sessions")
            .document(sessionId)
            .collection("messages")

        val messageDoc = if (message.id.isNotBlank()) {
            messagesCol.document(message.id)
        } else {
            messagesCol.document()
        }

        val toSave = if (message.id.isBlank()) message.copy(id = messageDoc.id) else message
        messageDoc.set(toSave).await()
    }

    /**
     * Retrieves all chat sessions for the specified user ordered by createdAt descending.
     */
    fun getChatSessions(uid: String): Flow<List<ChatSession>> = callbackFlow {
        val query = firestore.collection("users")
            .document(uid)
            .collection("chat_sessions")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val sessions = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ChatSession::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(sessions)
        }

        awaitClose { registration.remove() }
    }

    /**
     * Retrieves all messages for a given session ordered chronologically by timestamp ascending.
     */
    fun getMessagesForSession(uid: String, sessionId: String): Flow<List<ChatMessage>> = callbackFlow {
        val query = firestore.collection("users")
            .document(uid)
            .collection("chat_sessions")
            .document(sessionId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val messages = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(messages)
        }

        awaitClose { registration.remove() }
    }

    // PERSONAL LIBRARY BACKEND - NEW FUNCTIONS

    /**
     * Save or remove a chat session from Personal Library.
     */
    suspend fun setBookmarked(
        uid: String,
        sessionId: String,
        bookmarked: Boolean
    ) {
        firestore.collection("users")
            .document(uid)
            .collection("chat_sessions")
            .document(sessionId)
            .update("bookmarked", bookmarked)
            .await()
    }

    /**
     * Retrieve bookmarked sessions for the current user.
     */
    fun getBookmarkedSessions(
        uid: String
    ): Flow<List<ChatSession>> {
        return getChatSessions(uid).map { sessions ->
            sessions.filter { it.bookmarked }
        }
    }
}
