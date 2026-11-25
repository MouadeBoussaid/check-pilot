package com.mebiad.checkpilot.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mebiad.checkpilot.data.local.db.entity.GradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {
    @Upsert
    suspend fun upsert(grade: GradeEntity)

    @Query("SELECT * FROM grade WHERE sessionId = :sessionId")
    fun observe(sessionId: String): Flow<List<GradeEntity>>

    @Query("SELECT sessionId, crewMember, COUNT(*) AS count FROM grade GROUP BY sessionId, crewMember")
    fun observeGradedCounts(): Flow<List<GradedCountRow>>
}

data class GradedCountRow(
    val sessionId: String,
    val crewMember: String,
    val count: Int,
)
