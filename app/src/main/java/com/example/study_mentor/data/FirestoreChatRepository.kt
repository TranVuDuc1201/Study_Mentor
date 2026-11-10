
package com.example.study_mentor.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repository for Question History & Personal Library.
 *
 * Firestore structure:
 * users/{uid}/chat_sessions/{sessionId}
 * users/{uid}/chat_sessions/{sessionId}/messages/{messageId}
 */
class FirestoreChatRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    // 1. Tao cuoc tro chuyen moi
    suspend fun createNewSession(
        uid: String,
        title: String
    ): String {

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

    // 2. Luu cau hoi va cau tra loi AI
    suspend fun saveMessage(
        uid: String,
        sessionId: String,
        message: ChatMessage
    ) {

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

        val toSave = if (message.id.isBlank()) {
            message.copy(id = messageDoc.id)
        } else {
            message
        }

        messageDoc.set(toSave).await()
    }

    // 3. Lay toan bo lich su cau hoi
    fun getChatSessions(
        uid: String
    ): Flow<List<ChatSession>> = callbackFlow {

        val query = firestore.collection("users")
            .document(uid)
            .collection("chat_sessions")
            .orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )

        val registration = query.addSnapshotListener {
                snapshot, error ->

            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val sessions = snapshot?.documents?.mapNotNull {
                doc ->
                doc.toObject(ChatSession::class.java)
                    ?.copy(id = doc.id)
            } ?: emptyList()

            trySend(sessions)
        }

        awaitClose {
            registration.remove()
        }
    }

    // 4. Lay tin nhan trong mot cuoc tro chuyen
    fun getMessagesForSession(
        uid: String,
        sessionId: String
    ): Flow<List<ChatMessage>> = callbackFlow {

        val query = firestore.collection("users")
            .document(uid)
            .collection("chat_sessions")
            .document(sessionId)
            .collection("messages")
            .orderBy(
                "timestamp",
                Query.Direction.ASCENDING
            )

        val registration = query.addSnapshotListener {
                snapshot, error ->

            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val messages = snapshot?.documents?.mapNotNull {
                doc ->
                doc.toObject(ChatMessage::class.java)
                    ?.copy(id = doc.id)
            } ?: emptyList()

            trySend(messages)
        }

        awaitClose {
            registration.remove()
        }
    }

    // 5. Luu hoac bo luu cuoc tro chuyen
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

    // 6. Lay danh sach cuoc tro chuyen da luu
    fun getBookmarkedSessions(
        uid: String
    ): Flow<List<ChatSession>> {

        return getChatSessions(uid).map { sessions ->

            sessions.filter { session ->
                session.bookmarked
            }
        }
    }
}
