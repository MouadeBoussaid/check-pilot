package com.mebiad.checkpilot.domain.catalogue

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class ObserveCatalogueUsecaseFake : ObserveCatalogueUsecase {
    val competencies = MutableStateFlow<List<Competency>>(emptyList())

    override fun competencies(): Flow<List<Competency>> = competencies
}
