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

    /** Each condition left null matches every Session. The prefixes must hold digits or letters only. */
    @Query(
        """
        SELECT * FROM session
        WHERE (:staffNumberPrefix IS NULL
                OR cm1StaffNumber LIKE :staffNumberPrefix || '%'
                OR cm2StaffNumber LIKE :staffNumberPrefix || '%')
            AND (:airportPrefix IS NULL
                OR departureCode LIKE :airportPrefix || '%'
                OR destinationCode LIKE :airportPrefix || '%')
            AND (:date IS NULL OR date = :date)
        ORDER BY date DESC, createdAt DESC, rowid DESC
        """
    )
    fun search(
        staffNumberPrefix: String?,
        airportPrefix: String?,
        date: LocalDate?,
    ): Flow<List<SessionEntity>>

    @Query("DELETE FROM session WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM session")
    suspend fun deleteAll()
}
