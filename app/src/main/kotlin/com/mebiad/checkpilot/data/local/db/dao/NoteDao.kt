package com.mebiad.checkpilot.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mebiad.checkpilot.data.local.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Query("UPDATE note SET text = :text WHERE id = :id")
    suspend fun updateText(
        id: String,
        text: String,
    )

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM note WHERE sessionId = :sessionId AND text = ''")
    suspend fun deleteEmpty(sessionId: String)

    @Query("SELECT MAX(sortIndex) FROM note WHERE sessionId = :sessionId AND competencyId = :competencyId")
    suspend fun highestSortIndex(
        sessionId: String,
        competencyId: Long,
    ): Long?

    @Query("SELECT * FROM note WHERE sessionId = :sessionId ORDER BY sortIndex, rowid")
    fun observe(sessionId: String): Flow<List<NoteEntity>>
}
