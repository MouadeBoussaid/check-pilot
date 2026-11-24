package com.mebiad.checkpilot.testutils

import com.mebiad.checkpilot.data.local.db.CheckPilotDatabase
import com.mebiad.checkpilot.domain.session.AirportCode
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.SessionRepository
import com.mebiad.checkpilot.domain.session.StaffNumber
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Writes the Given state of acceptance tests into the app's database: Sessions through the real [SessionRepository],
 * Notes and Grades straight into their rows. Each Note goes below the Notes already seeded under its Competency.
 */
class SessionSeeds(
    private val harness: AppHarness,
) {
    private val nextSortIndex = mutableMapOf<Pair<String, Long>, Long>()

    fun session(
        id: String,
        cm1: String? = "79125",
        cm2: String? = "04512",
        departure: String = "DOH",
        destination: String = "SLL",
        date: LocalDate = LocalDate.of(2026, 11, 1),
        createdAt: Instant = Instant.parse("2026-11-01T08:00:00Z"),
    ) {
        val session =
            Session(
                id = id,
                createdAt = createdAt,
                date = date,
                cm1StaffNumber = cm1?.let { checkNotNull(StaffNumber.parse(it)) },
                cm2StaffNumber = cm2?.let { checkNotNull(StaffNumber.parse(it)) },
                departure = checkNotNull(AirportCode.parse(departure)),
                destination = checkNotNull(AirportCode.parse(destination))
            )
        runBlocking { harness.get<SessionRepository>().insert(session) }
    }

    /** [cmTag] is the stored code `CM1`, `CM2` or `BOTH`; [observableBehaviour] is an OBS BEH name such as "3c". */
    fun note(
        sessionId: String,
        competencyId: Long,
        text: String,
        cmTag: String = "BOTH",
        observableBehaviour: String? = null,
        id: String = UUID.randomUUID().toString(),
    ) {
        val key = sessionId to competencyId
        val sortIndex = nextSortIndex.getOrDefault(key, 0L)
        nextSortIndex[key] = sortIndex + 1
        database().execSQL(
            "INSERT INTO note (id, sessionId, competencyId, text, cmTag, observableBehaviourId, sortIndex) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                id,
                sessionId,
                competencyId,
                text,
                cmTag,
                observableBehaviour?.let(::observableBehaviourId),
                sortIndex
            )
        )
    }

    /** [crewMember] is `CM1` or `CM2`; [value] is the stored code, such as `ADEQUATE`. */
    fun grade(
        sessionId: String,
        competencyId: Long,
        crewMember: String,
        value: String,
    ) {
        database().execSQL(
            "INSERT INTO grade (sessionId, competencyId, crewMember, value) VALUES (?, ?, ?, ?)",
            arrayOf<Any?>(sessionId, competencyId, crewMember, value)
        )
    }

    fun observableBehaviourId(name: String): Long =
        database().query("SELECT id FROM observable_behaviour WHERE name = ?", arrayOf(name)).use { cursor ->
            check(cursor.moveToFirst()) { "No OBS BEH named $name" }
            cursor.getLong(0)
        }

    private fun database() = harness.get<CheckPilotDatabase>().openHelper.writableDatabase
}
