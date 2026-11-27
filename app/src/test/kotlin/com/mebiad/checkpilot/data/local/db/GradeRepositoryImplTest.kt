package com.mebiad.checkpilot.data.local.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mebiad.checkpilot.domain.grade.CompetencyGrade
import com.mebiad.checkpilot.domain.grade.Grade
import com.mebiad.checkpilot.domain.grade.GradedCount
import com.mebiad.checkpilot.domain.session.AirportCode
import com.mebiad.checkpilot.domain.session.CrewMember
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.StaffNumber
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class GradeRepositoryImplTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val database by lazy {
        CheckPilotDatabase.build(
            context = ApplicationProvider.getApplicationContext(),
            name = folder.root.resolve(CheckPilotDatabase.FILE_NAME).absolutePath,
            openHelperFactory = null
        )
    }
    private val repository by lazy { GradeRepositoryImpl(dao = database.gradeDao()) }
    private val sessions by lazy { SessionRepositoryImpl(dao = database.sessionDao()) }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun aSetGradeIsObservedForItsSessionOnly() =
        runTest {
            // Given
            sessions.insert(session("session-1"))
            sessions.insert(session("session-2"))

            // When
            repository.set("session-1", competencyId = 2, crewMember = CrewMember.CM1, grade = Grade.ADEQUATE)
            repository.set("session-2", competencyId = 2, crewMember = CrewMember.CM1, grade = Grade.EXEMPLARY)

            // Then
            assertEquals(
                listOf(CompetencyGrade(competencyId = 2, crewMember = CrewMember.CM1, grade = Grade.ADEQUATE)),
                repository.observe("session-1").first()
            )
        }

    @Test
    fun settingAGradeAgainReplacesItAndLeavesTheOtherCrewMembersAlone() =
        runTest {
            // Given
            sessions.insert(session("session-1"))
            repository.set("session-1", competencyId = 2, crewMember = CrewMember.CM1, grade = Grade.ADEQUATE)
            repository.set("session-1", competencyId = 2, crewMember = CrewMember.CM2, grade = Grade.EFFECTIVE)

            // When
            repository.set("session-1", competencyId = 2, crewMember = CrewMember.CM1, grade = Grade.NOT_OBSERVED)

            // Then
            assertEquals(
                setOf(
                    CompetencyGrade(competencyId = 2, crewMember = CrewMember.CM1, grade = Grade.NOT_OBSERVED),
                    CompetencyGrade(competencyId = 2, crewMember = CrewMember.CM2, grade = Grade.EFFECTIVE)
                ),
                repository.observe("session-1").first().toSet()
            )
        }

    @Test
    fun deletingACrewMembersGradesLeavesTheOtherCrewMemberAndSessionsAlone() =
        runTest {
            // Given
            sessions.insert(session("session-1"))
            sessions.insert(session("session-2"))
            repository.set("session-1", competencyId = 1, crewMember = CrewMember.CM1, grade = Grade.ADEQUATE)
            repository.set("session-1", competencyId = 1, crewMember = CrewMember.CM2, grade = Grade.EFFECTIVE)
            repository.set("session-1", competencyId = 2, crewMember = CrewMember.CM2, grade = Grade.NOT_OBSERVED)
            repository.set("session-2", competencyId = 1, crewMember = CrewMember.CM2, grade = Grade.EXEMPLARY)

            // When
            repository.deleteOf(sessionId = "session-1", crewMember = CrewMember.CM2)

            // Then
            assertEquals(
                listOf(CompetencyGrade(competencyId = 1, crewMember = CrewMember.CM1, grade = Grade.ADEQUATE)),
                repository.observe("session-1").first()
            )
            assertEquals(
                listOf(CompetencyGrade(competencyId = 1, crewMember = CrewMember.CM2, grade = Grade.EXEMPLARY)),
                repository.observe("session-2").first()
            )
        }

    @Test
    fun theGradedCountsArePerSessionAndCrewMember() =
        runTest {
            // Given
            sessions.insert(session("session-1"))
            sessions.insert(session("session-2"))
            (1L..3L).forEach {
                repository.set("session-1", competencyId = it, crewMember = CrewMember.CM1, grade = Grade.ADEQUATE)
            }
            repository.set("session-1", competencyId = 1, crewMember = CrewMember.CM2, grade = Grade.NOT_OBSERVED)
            repository.set("session-2", competencyId = 4, crewMember = CrewMember.CM2, grade = Grade.EFFECTIVE)

            // When
            val counts = repository.observeGradedCounts().first()

            // Then
            assertEquals(
                setOf(
                    GradedCount(sessionId = "session-1", crewMember = CrewMember.CM1, count = 3),
                    GradedCount(sessionId = "session-1", crewMember = CrewMember.CM2, count = 1),
                    GradedCount(sessionId = "session-2", crewMember = CrewMember.CM2, count = 1)
                ),
                counts.toSet()
            )
        }

    private fun session(id: String): Session =
        Session(
            id = id,
            createdAt = Instant.parse("2026-11-22T09:30:00Z"),
            date = LocalDate.of(2026, 11, 1),
            cm1StaffNumber = StaffNumber.parse("79125"),
            cm2StaffNumber = StaffNumber.parse("04512"),
            departure = checkNotNull(AirportCode.parse("DOH")),
            destination = checkNotNull(AirportCode.parse("SLL"))
        )
}
