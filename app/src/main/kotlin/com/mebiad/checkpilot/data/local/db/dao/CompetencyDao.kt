package com.mebiad.checkpilot.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.mebiad.checkpilot.data.local.db.entity.CompetencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompetencyDao {
    @Query("SELECT * FROM competency ORDER BY sortIndex")
    fun observeAll(): Flow<List<CompetencyEntity>>
}
