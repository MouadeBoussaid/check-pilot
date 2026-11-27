package com.mebiad.checkpilot.domain.session

interface DeleteSessionUsecase {
    /** Deletes the Session with [sessionId], with its Notes and Grades. Returns at once. */
    fun delete(sessionId: String)

    /** Deletes every Session, with their Notes and Grades. Returns once they are gone. */
    suspend fun deleteAll()
}
