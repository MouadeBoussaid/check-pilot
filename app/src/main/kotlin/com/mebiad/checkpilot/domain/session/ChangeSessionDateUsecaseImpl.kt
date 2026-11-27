package com.mebiad.checkpilot.domain.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class ChangeSessionDateUsecaseImpl(
    private val repository: SessionRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChangeSessionDateUsecase {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    override fun change(
        sessionId: String,
        date: LocalDate,
    ) {
        scope.launch { repository.updateDate(sessionId, date) }
    }
}
