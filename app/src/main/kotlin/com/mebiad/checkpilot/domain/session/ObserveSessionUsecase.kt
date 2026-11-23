package com.mebiad.checkpilot.domain.session

import kotlinx.coroutines.flow.Flow

interface ObserveSessionUsecase {
    /** Emits the Session with [id] whenever it changes, or null while there is none. */
    fun session(id: String): Flow<Session?>

    /** Emits the Last Session whenever it changes, or null while there is no Session. */
    fun lastSession(): Flow<Session?>
}
