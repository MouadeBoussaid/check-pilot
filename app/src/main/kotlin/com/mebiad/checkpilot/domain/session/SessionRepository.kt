package com.mebiad.checkpilot.domain.session

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun insert(session: Session)

    /** Emits the Session with [id], or null while there is none. */
    fun observe(id: String): Flow<Session?>

    /** Emits the Last Session: the latest created, the later insert winning a tie. Null while there is no Session. */
    fun observeLast(): Flow<Session?>
}
