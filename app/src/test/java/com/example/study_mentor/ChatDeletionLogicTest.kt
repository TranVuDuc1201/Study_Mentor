package com.example.study_mentor

import com.example.study_mentor.data.deleteChatSessionInBatches
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatDeletionLogicTest {

    @Test
    fun testDeleteAllMessages() = runBlocking {
        val messages = (1..805).toMutableList()
        val batchSizes = mutableListOf<Int>()
        var sessionDeleted = false

        deleteChatSessionInBatches(
            loadBatch = {
                messages.take(400)
            },
            deleteBatch = { batch ->
                assertFalse(sessionDeleted)
                batchSizes.add(batch.size)
                messages.removeAll(batch.toSet())
            },
            deleteParent = {
                assertTrue(messages.isEmpty())
                sessionDeleted = true
            }
        )

        assertEquals(listOf(400, 400, 5), batchSizes)
        assertTrue(messages.isEmpty())
        assertTrue(sessionDeleted)
    }

    @Test
    fun testDeleteFailure() = runBlocking {
        var sessionDeleted = false
        var exceptionCaught = false

        try {
            deleteChatSessionInBatches(
                loadBatch = {
                    listOf(1, 2, 3)
                },
                deleteBatch = {
                    throw IllegalStateException("Test failure")
                },
                deleteParent = {
                    sessionDeleted = true
                }
            )
        } catch (e: IllegalStateException) {
            exceptionCaught = true
        }

        assertTrue(exceptionCaught)
        assertFalse(sessionDeleted)
    }

    @Test
    fun testDeleteEmptySession() = runBlocking {
        var sessionDeleted = false
        var deleteBatchCalled = false

        deleteChatSessionInBatches(
            loadBatch = {
                emptyList<Int>()
            },
            deleteBatch = {
                deleteBatchCalled = true
            },
            deleteParent = {
                sessionDeleted = true
            }
        )

        assertTrue(sessionDeleted)
        assertFalse(deleteBatchCalled)
    }
}
