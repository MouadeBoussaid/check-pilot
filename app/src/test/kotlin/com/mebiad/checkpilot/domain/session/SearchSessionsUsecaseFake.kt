package com.mebiad.checkpilot.domain.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Finds what [setResults] set for a text and date, and no Session for any other search. */
class SearchSessionsUsecaseFake : SearchSessionsUsecase {
    private val resultsState = MutableStateFlow<Map<Pair<String, LocalDate?>, List<SessionSummary>>>(emptyMap())

    /** Each search asked for, in order. */
    val searches = mutableListOf<Pair<String, LocalDate?>>()

    fun setResults(
        text: String,
        date: LocalDate?,
        summaries: List<SessionSummary>,
    ) {
        resultsState.value += (text to date) to summaries
    }

    override fun search(
        text: String,
        date: LocalDate?,
    ): Flow<List<SessionSummary>> {
        searches += text to date
        return resultsState.map { it[text to date].orEmpty() }
    }

    fun reset() {
        resultsState.value = emptyMap()
        searches.clear()
    }
}
