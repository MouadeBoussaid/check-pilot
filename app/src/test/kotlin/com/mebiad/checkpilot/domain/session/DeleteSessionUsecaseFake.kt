package com.mebiad.checkpilot.domain.session

class DeleteSessionUsecaseFake : DeleteSessionUsecase {
    /** Each Session id deleted, in order. */
    val deleted = mutableListOf<String>()

    override fun delete(sessionId: String) {
        deleted += sessionId
    }

    fun reset() {
        deleted.clear()
    }
}
