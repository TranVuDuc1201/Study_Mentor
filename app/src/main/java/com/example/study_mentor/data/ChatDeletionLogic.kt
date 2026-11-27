package com.example.study_mentor.data

internal suspend fun <T> deleteChatSessionInBatches(
    loadBatch: suspend () -> List<T>,
    deleteBatch: suspend (List<T>) -> Unit,
    deleteParent: suspend () -> Unit
) {
    while (true) {
        val messages = loadBatch()

        if (messages.isEmpty()) {
            break
        }

        deleteBatch(messages)
    }

    deleteParent()
}
