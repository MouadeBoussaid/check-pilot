package com.mebiad.checkpilot.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.mebiad.checkpilot.data.local.db.entity.CompetencyEntity
import com.mebiad.checkpilot.data.local.db.entity.ObservableBehaviourEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompetencyDao {
    @Query("SELECT * FROM competency ORDER BY sortIndex")
    fun observeAll(): Flow<List<CompetencyEntity>>

    @Query("SELECT * FROM observable_behaviour ORDER BY competencyId, sortIndex")
    fun observeObservableBehaviours(): Flow<List<ObservableBehaviourEntity>>

    @Query("SELECT * FROM observable_behaviour WHERE id = :id")
    suspend fun findObservableBehaviour(id: Long): ObservableBehaviourEntity?
}
