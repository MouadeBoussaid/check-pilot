package com.mebiad.checkpilot.domain.session

class DuplicateSessionUsecaseFake : DuplicateSessionUsecase {
    /** Each Session id duplicated, in order. */
    val duplicated = mutableListOf<String>()

    override fun duplicate(sessionId: String) {
        duplicated += sessionId
    }

    fun reset() {
        duplicated.clear()
    }
}
