package com.mebiad.checkpilot.domain.session

import java.time.LocalDate

class ChangeSessionDateUsecaseFake : ChangeSessionDateUsecase {
    /** Each change asked for, in order. */
    val changes = mutableListOf<Pair<String, LocalDate>>()

    override fun change(
        sessionId: String,
        date: LocalDate,
    ) {
        changes += sessionId to date
    }

    fun reset() {
        changes.clear()
    }
}
