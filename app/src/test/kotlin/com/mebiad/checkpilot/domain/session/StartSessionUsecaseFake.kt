package com.mebiad.checkpilot.domain.session

import kotlinx.coroutines.CompletableDeferred

class StartSessionUsecaseFake : StartSessionUsecase {
    var result: StartSessionResult = StartSessionResult.Started("session-1")
    val setups = mutableListOf<SessionSetup>()

    /** While set and not completed, every start waits on it, as a slow save would. */
    var hold: CompletableDeferred<Unit>? = null

    override suspend fun start(setup: SessionSetup): StartSessionResult {
        setups += setup
        hold?.await()
        return result
    }

    fun reset() {
        result = StartSessionResult.Started("session-1")
        setups.clear()
        hold = null
    }
}
