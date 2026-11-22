package com.mebiad.checkpilot.data.local.db

import com.mebiad.checkpilot.data.local.db.dao.CompetencyDao
import com.mebiad.checkpilot.domain.catalogue.CatalogueRepository
import com.mebiad.checkpilot.domain.catalogue.Competency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CatalogueRepositoryImpl(
    private val dao: CompetencyDao,
) : CatalogueRepository {
    override fun observeCompetencies(): Flow<List<Competency>> =
        dao.observeAll().map { rows -> rows.map { Competency(id = it.id, name = it.name) } }
}
