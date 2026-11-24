package com.mebiad.checkpilot.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mebiad.checkpilot.data.local.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity)

    @Query("UPDATE session SET date = :date WHERE id = :id")
    suspend fun updateDate(
        id: String,
        date: LocalDate,
    )

    @Query("SELECT * FROM session WHERE id = :id")
    fun observe(id: String): Flow<SessionEntity?>

    @Query("SELECT * FROM session ORDER BY createdAt DESC, rowid DESC LIMIT 1")
    fun observeLast(): Flow<SessionEntity?>
}
