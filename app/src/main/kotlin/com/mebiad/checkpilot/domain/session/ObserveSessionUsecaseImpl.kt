package com.mebiad.checkpilot.domain.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class ObserveSessionUsecaseImpl(
    private val repository: SessionRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ObserveSessionUsecase {
    override fun session(id: String): Flow<Session?> = repository.observe(id).flowOn(dispatcher)
}
