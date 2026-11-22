package com.mebiad.checkpilot.domain.catalogue

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class ObserveCatalogueUsecaseImpl(
    private val repository: CatalogueRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ObserveCatalogueUsecase {
    override fun competencies(): Flow<List<Competency>> = repository.observeCompetencies().flowOn(dispatcher)
}
