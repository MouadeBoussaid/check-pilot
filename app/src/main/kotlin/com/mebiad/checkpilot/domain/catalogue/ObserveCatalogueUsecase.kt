package com.mebiad.checkpilot.domain.catalogue

import kotlinx.coroutines.flow.Flow

interface ObserveCatalogueUsecase {
    /** Emits every Competency in Catalogue order, again whenever the Catalogue changes. */
    fun competencies(): Flow<List<Competency>>
}
