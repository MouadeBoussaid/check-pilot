package com.mebiad.checkpilot.domain.session

import app.cash.turbine.test
import com.mebiad.checkpilot.domain.catalogue.CatalogueRepositoryFake
import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.grade.Grade
import com.mebiad.checkpilot.domain.grade.GradeRepositoryFake
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SearchSessionsUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val sessions = SessionRepositoryFake()
    private val grades = GradeRepositoryFake()
    private val catalogue = CatalogueRepositoryFake()
    private val usecase =
        SearchSessionsUsecaseImpl(sessions = sessions, grades = grades, catalogue = catalogue, dispatcher = dispatcher)

    @Test
    fun eachFoundSessionComesWithHowManyCompetenciesEachCrewMemberHasAGradeFor() =
        runTest(dispatcher) {
            // Given
            catalogue.competencies.value = (1L..9L).map { Competency(id = it, name = "Competency $it") }
            sessions.insert(session(id = "ungraded", cm1 = "79125", cm2 = null))
            sessions.insert(session(id = "graded", cm1 = "79125", cm2 = "04512"))
            (1L..3L).forEach { grades.set("graded", it, CrewMember.CM1, Grade.ADEQUATE) }
            grades.set("graded", 5, CrewMember.CM2, Grade.NOT_OBSERVED)

            // When
            val found = usecase.search(text = "79125", date = null).first()

            // Then
            assertEquals(
                listOf(
                    SessionSummary(
                        session = session(id = "graded", cm1 = "79125", cm2 = "04512"),
                        gradedCounts = mapOf(CrewMember.CM1 to 3, CrewMember.CM2 to 1),
                        competencyCount = 9
                    ),
                    SessionSummary(
                        session = session(id = "ungraded", cm1 = "79125", cm2 = null),
                        gradedCounts = emptyMap(),
                        competencyCount = 9
                    )
                ),
                found
            )
        }

    @Test
    fun theTextAndTheDateNarrowTheSessions() =
        runTest(dispatcher) {
            // Given
            val eighth = LocalDate.of(2026, 11, 8)
            sessions.insert(session(id = "to-sll", destination = "SLL", date = eighth))
            sessions.insert(session(id = "to-sll-later", destination = "SLL", date = eighth.plusDays(1)))
            sessions.insert(session(id = "to-lhr", destination = "LHR", date = eighth))

            // When
            val found = usecase.search(text = "sll", date = eighth).first()

            // Then
            assertEquals(listOf("to-sll"), found.map { it.session.id })
        }

    @Test
    fun textThatCanMatchNoSessionFindsNone() =
        runTest(dispatcher) {
            // Given
            sessions.insert(session(id = "any"))

            // When
            val found = usecase.search(text = "DOH%", date = null).first()

            // Then
            assertEquals(emptyList<SessionSummary>(), found)
        }

    @Test
    fun theFoundSessionsFollowChangesToSessionsAndGrades() =
        runTest(dispatcher) {
            // Given
            catalogue.competencies.value = (1L..9L).map { Competency(id = it, name = "Competency $it") }
            sessions.insert(session(id = "first"))

            usecase.search(text = "", date = null).test {
                assertEquals(listOf(emptyMap<CrewMember, Int>()), awaitItem().map { it.gradedCounts })

                // When / Then: a Grade is set
                grades.set("first", 1, CrewMember.CM1, Grade.EFFECTIVE)
                assertEquals(listOf(mapOf(CrewMember.CM1 to 1)), awaitItem().map { it.gradedCounts })

                // When / Then: the Session is deleted
                sessions.delete("first")
                assertEquals(emptyList<SessionSummary>(), awaitItem())
            }
        }

    private fun session(
        id: String,
        cm1: String? = "79125",
        cm2: String? = null,
        destination: String = "SLL",
        date: LocalDate = LocalDate.of(2026, 11, 1),
    ): Session =
        Session(
            id = id,
            createdAt = Instant.parse("2026-11-01T08:00:00Z"),
            date = date,
            cm1StaffNumber = cm1?.let { checkNotNull(StaffNumber.parse(it)) },
            cm2StaffNumber = cm2?.let { checkNotNull(StaffNumber.parse(it)) },
            departure = checkNotNull(AirportCode.parse("DOH")),
            destination = checkNotNull(AirportCode.parse(destination))
        )
}
