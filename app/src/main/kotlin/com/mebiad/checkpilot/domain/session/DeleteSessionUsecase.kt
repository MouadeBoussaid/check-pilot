package com.mebiad.checkpilot.domain.session

interface DeleteSessionUsecase {
    /** Deletes the Session with [sessionId], with its Notes and Grades. Returns at once. */
    fun delete(sessionId: String)
}
