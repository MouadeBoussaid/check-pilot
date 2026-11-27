package com.mebiad.checkpilot.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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

    @Query(
        "INSERT INTO competency (name, sortIndex) SELECT :name, COALESCE(MAX(sortIndex), -1) + 1 FROM competency"
    )
    suspend fun insertLast(name: String)

    @Query("UPDATE competency SET name = :name WHERE id = :id")
    suspend fun rename(
        id: Long,
        name: String,
    )

    @Query(
        """
        INSERT INTO observable_behaviour (competencyId, name, sortIndex)
        SELECT :competencyId, :name, COALESCE(MAX(sortIndex), -1) + 1 FROM observable_behaviour
        WHERE competencyId = :competencyId
        """
    )
    suspend fun insertObservableBehaviourLast(
        competencyId: Long,
        name: String,
    )

    @Query("UPDATE observable_behaviour SET name = :name WHERE id = :id")
    suspend fun renameObservableBehaviour(
        id: Long,
        name: String,
    )

    @Query("UPDATE competency SET sortIndex = :sortIndex WHERE id = :id")
    suspend fun updateSortIndex(
        id: Long,
        sortIndex: Long,
    )

    @Query("UPDATE observable_behaviour SET sortIndex = :sortIndex WHERE id = :id")
    suspend fun updateObservableBehaviourSortIndex(
        id: Long,
        sortIndex: Long,
    )

    @Transaction
    suspend fun reorder(ids: List<Long>) {
        ids.forEachIndexed { index, id -> updateSortIndex(id = id, sortIndex = index.toLong()) }
    }

    @Transaction
    suspend fun reorderObservableBehaviours(ids: List<Long>) {
        ids.forEachIndexed { index, id -> updateObservableBehaviourSortIndex(id = id, sortIndex = index.toLong()) }
    }

    @Query("DELETE FROM competency WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM observable_behaviour WHERE id = :id")
    suspend fun deleteObservableBehaviour(id: Long)

    @Query(
        """
        SELECT competencyId AS id, COUNT(DISTINCT sessionId) AS sessions
        FROM (SELECT competencyId, sessionId FROM note UNION SELECT competencyId, sessionId FROM grade)
        GROUP BY competencyId
        """
    )
    fun observeCompetencyUsage(): Flow<List<UsageRow>>

    @Query(
        """
        SELECT observableBehaviourId AS id, COUNT(DISTINCT sessionId) AS sessions FROM note
        WHERE observableBehaviourId IS NOT NULL
        GROUP BY observableBehaviourId
        """
    )
    fun observeObservableBehaviourUsage(): Flow<List<UsageRow>>
}

/** How many Sessions use the Catalogue item [id]. */
data class UsageRow(
    val id: Long,
    val sessions: Int,
)
