package com.mebiad.checkpilot.domain.session

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface SearchSessionsUsecase {
    /**
     * Emits the Sessions matching the search [text] and, if given, dated [date], newest first, again whenever they or
     * their Grades change. See [SessionFilter.of] for how the text is read.
     */
    fun search(
        text: String,
        date: LocalDate?,
    ): Flow<List<SessionSummary>>
}
