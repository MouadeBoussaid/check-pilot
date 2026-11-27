package com.mebiad.checkpilot.domain.session

class DeleteSessionUsecaseFake : DeleteSessionUsecase {
    /** Each Session id deleted, in order. */
    val deleted = mutableListOf<String>()

    override fun delete(sessionId: String) {
        deleted += sessionId
    }

    /** How many times every Session was deleted. */
    var deleteAllCount: Int = 0

    override suspend fun deleteAll() {
        deleteAllCount++
    }

    fun reset() {
        deleted.clear()
        deleteAllCount = 0
    }
}
