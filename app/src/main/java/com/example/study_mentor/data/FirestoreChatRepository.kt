package com.example.study_mentor.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Backend repository for chat history and bookmarked sessions.
 * Firestore: users/{uid}/chat_sessions/{sessionId}/messages/{messageId}
 */
class FirestoreChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Create a session and return its Firestore document ID. */
    suspend fun createNewSession(uid: String, title: String): String {
        val sessionDoc = firestore.collection("users")
            .document(uid)
            .collection("chat_sessions")
            .document()

        val session = ChatSession(
            id = sessionDoc.id,
            title = title,
            createdAt = System.currentTimeMillis()
        )
        sessionDoc.set(session).await()
        return sessionDoc.id
    }

    /** Save a message under its existing ID, or generate an ID if missing. */
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

        val toSave = if (message.id.isBlank()) {
            message.copy(id = messageDoc.id)
        } else {
            message
        }
        messageDoc.set(toSave).await()
    }

    /** Observe all sessions, newest first (existing UI-compatible API). */
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

    /** Observe a session's messages, ordered by timestamp ascending. */
    fun getMessagesForSession(uid: String, sessionId: String): Flow<List<ChatMessage>> =
        callbackFlow {
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

    /** Save or unsave an existing chat session in Personal Library. */
    suspend fun setBookmarked(uid: String, sessionId: String, bookmarked: Boolean) {
        firestore.collection("users")
            .document(uid)
            .collection("chat_sessions")
            .document(sessionId)
            .update("bookmarked", bookmarked)
            .await()
    }

    /** Observe bookmarked sessions; uses the existing chat history source. */
    fun getBookmarkedSessions(uid: String): Flow<List<ChatSession>> {
        return getChatSessions(uid).map { sessions ->
            sessions.filter { it.bookmarked }
        }
    }

    /**
     * H04: Load a page of chat sessions without downloading the entire history.
     * First call: lastDocument = null.
     * Next call: pass the DocumentSnapshot returned by the previous call.
     * Returns (sessions, nextCursor); a null cursor means no results in this page.
     * A short page (< pageSize) indicates the end of the current data set.
     */
    suspend fun getChatSessionsPage(
        uid: String,
        pageSize: Int = 20,
        lastDocument: DocumentSnapshot? = null
    ): Pair<List<ChatSession>, DocumentSnapshot?> {
        require(uid.isNotBlank()) { "UID cannot be empty" }
        require(pageSize in 1..50) { "pageSize must be between 1 and 50" }

        var query: Query = firestore.collection("users")
            .document(uid)
            .collection("chat_sessions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        val snapshot = query.get().await()
        val sessions = snapshot.documents.mapNotNull { doc ->
            doc.toObject(ChatSession::class.java)?.copy(id = doc.id)
        }

        return sessions to snapshot.documents.lastOrNull()
    }

    /**
     * H03: Delete a chat session and its message subcollection.
     *
     * Firestore does not delete subcollections automatically when their parent
     * document is deleted. Remove messages in batches, then delete the session.
     * A failed operation can be retried; Firestore Security Rules must enforce
     * that the caller is allowed to delete data belonging to this UID.
     */
    suspend fun deleteSession(uid: String, sessionId: String) {
        require(uid.isNotBlank()) { "UID cannot be empty" }
        require(sessionId.isNotBlank()) { "Session ID cannot be empty" }

        val sessionRef = firestore.collection("users")
            .document(uid)
            .collection("chat_sessions")
            .document(sessionId)

        val messagesRef = sessionRef.collection("messages")

        deleteChatSessionInBatches(
            loadBatch = {
                messagesRef.limit(400).get().await().documents
            },
            deleteBatch = { documents ->
                val batch = firestore.batch()

                documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                batch.commit().await()
            },
            deleteParent = {
                sessionRef.delete().await()
            }
        )
    }

}
