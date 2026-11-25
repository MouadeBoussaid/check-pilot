package com.mebiad.checkpilot.domain.session

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface SessionRepository {
    suspend fun insert(session: Session)

    /** Changes the date of the Session with [id]; the rest of it stays as it was. */
    suspend fun updateDate(
        id: String,
        date: LocalDate,
    )

    /** Emits the Session with [id], or null while there is none. */
    fun observe(id: String): Flow<Session?>

    /** Emits the Last Session: the latest created, the later insert winning a tie. Null while there is no Session. */
    fun observeLast(): Flow<Session?>

    /** Emits the Sessions [filter] matches, newest date first, then latest created, again whenever they change. */
    fun search(filter: SessionFilter): Flow<List<Session>>

    /** Deletes the Session with [id], its Notes and its Grades. */
    suspend fun delete(id: String)
}
