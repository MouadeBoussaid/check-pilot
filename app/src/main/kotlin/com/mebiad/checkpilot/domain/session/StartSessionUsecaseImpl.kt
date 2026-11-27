package com.mebiad.checkpilot.domain.session

import com.mebiad.checkpilot.core.clock.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class StartSessionUsecaseImpl(
    private val repository: SessionRepository,
    private val clock: Clock,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : StartSessionUsecase {
    override suspend fun start(setup: SessionSetup): StartSessionResult {
        refusalOf(setup)?.let { return StartSessionResult.Refused(it) }
        val session =
            Session(
                id = newId(),
                createdAt = clock.now(),
                date = setup.date,
                cm1StaffNumber = StaffNumber.parse(setup.cm1StaffNumber),
                cm2StaffNumber = StaffNumber.parse(setup.cm2StaffNumber),
                departure = checkNotNull(AirportCode.parse(setup.departure)),
                destination = checkNotNull(AirportCode.parse(setup.destination))
            )
        withContext(dispatcher) { repository.insert(session) }
        return StartSessionResult.Started(session.id)
    }

    private fun refusalOf(setup: SessionSetup): SetupRefusal? {
        val staffNumbers = listOf(setup.cm1StaffNumber, setup.cm2StaffNumber).filter { it.isNotEmpty() }
        val airportCodes = listOf(setup.departure, setup.destination)
        return when {
            staffNumbers.isEmpty() -> SetupRefusal.NO_STAFF_NUMBER
            staffNumbers.any { StaffNumber.parse(it) == null } -> SetupRefusal.STAFF_NUMBER_NOT_4_TO_6_DIGITS
            airportCodes.any { it.isEmpty() } -> SetupRefusal.AIRPORT_CODE_MISSING
            airportCodes.any { AirportCode.parse(it) == null } -> SetupRefusal.AIRPORT_CODE_NOT_3_LETTERS
            else -> null
        }
    }
}
