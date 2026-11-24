package com.mebiad.checkpilot.domain.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DeleteSessionUsecaseImpl(
    private val repository: SessionRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DeleteSessionUsecase {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    override fun delete(sessionId: String) {
        scope.launch { repository.delete(sessionId) }
    }
}
