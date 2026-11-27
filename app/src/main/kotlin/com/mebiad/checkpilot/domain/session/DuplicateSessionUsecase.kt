package com.mebiad.checkpilot.domain.session

interface DuplicateSessionUsecase {
    /**
     * Saves a new Session for the return Sector of the Session with [sessionId]: the same Staff Numbers, the route
     * swapped, dated today, and no Notes or Grades. It becomes the Last Session. Returns at once.
     */
    fun duplicate(sessionId: String)
}
