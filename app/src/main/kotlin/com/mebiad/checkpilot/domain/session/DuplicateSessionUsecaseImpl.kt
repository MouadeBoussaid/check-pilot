package com.mebiad.checkpilot.domain.session

import com.mebiad.checkpilot.core.clock.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class DuplicateSessionUsecaseImpl(
    private val repository: SessionRepository,
    private val clock: Clock,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DuplicateSessionUsecase {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    override fun duplicate(sessionId: String) {
        scope.launch {
            val original = repository.observe(sessionId).first() ?: return@launch
            repository.insert(
                original.copy(
                    id = newId(),
                    createdAt = clock.now(),
                    date = clock.today(),
                    departure = original.destination,
                    destination = original.departure
                )
            )
        }
    }
}
