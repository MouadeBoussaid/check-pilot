package com.mebiad.checkpilot.domain.session

import java.time.LocalDate

interface ChangeSessionDateUsecase {
    /** Saves [date] as the Session's date. Returns at once. */
    fun change(
        sessionId: String,
        date: LocalDate,
    )
}
