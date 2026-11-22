package com.mebiad.checkpilot.domain.session

import java.time.LocalDate

interface StartSessionUsecase {
    /** Validates [setup] and saves a new Session from it, or says why it can't. */
    suspend fun start(setup: SessionSetup): StartSessionResult
}

/** The setup screen's fields as the Assessor typed them. */
data class SessionSetup(
    val cm1StaffNumber: String,
    val cm2StaffNumber: String,
    val departure: String,
    val destination: String,
    val date: LocalDate,
)

sealed interface StartSessionResult {
    data class Started(
        val sessionId: String,
    ) : StartSessionResult

    data class Refused(
        val reason: SetupRefusal,
    ) : StartSessionResult
}

enum class SetupRefusal {
    NO_STAFF_NUMBER,
    STAFF_NUMBER_NOT_4_TO_6_DIGITS,
    AIRPORT_CODE_MISSING,
    AIRPORT_CODE_NOT_3_LETTERS,
}
