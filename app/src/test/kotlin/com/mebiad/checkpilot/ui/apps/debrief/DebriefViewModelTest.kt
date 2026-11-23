package com.mebiad.checkpilot.ui.apps.debrief

import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.debrief.Debrief
import com.mebiad.checkpilot.domain.debrief.DebriefCompetency
import com.mebiad.checkpilot.domain.debrief.ObserveDebriefUsecaseFake
import com.mebiad.checkpilot.domain.grade.CompetencyGrade
import com.mebiad.checkpilot.domain.grade.Grade
import com.mebiad.checkpilot.domain.grade.SetGradeUsecaseFake
import com.mebiad.checkpilot.domain.session.AirportCode
import com.mebiad.checkpilot.domain.session.ChangeSessionDateUsecaseFake
import com.mebiad.checkpilot.domain.session.CrewMember
import com.mebiad.checkpilot.domain.session.ObserveSessionUsecaseFake
import com.mebiad.checkpilot.domain.session.Session
import com.mebiad.checkpilot.domain.session.StaffNumber
import com.mebiad.checkpilot.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DebriefViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeSession = ObserveSessionUsecaseFake()
    private val observeDebrief = ObserveDebriefUsecaseFake()
    private val setGrade = SetGradeUsecaseFake()
    private val changeSessionDate = ChangeSessionDateUsecaseFake()

    private val session =
        Session(
            id = SESSION_ID,
            createdAt = Instant.parse("2026-11-22T06:00:00Z"),
            date = LocalDate.of(2026, 11, 22),
            cm1StaffNumber = StaffNumber.parse("79125"),
            cm2StaffNumber = StaffNumber.parse("04512"),
            departure = checkNotNull(AirportCode.parse("DOH")),
            destination = checkNotNull(AirportCode.parse("SLL"))
        )

    @Test
    fun itShowsTheSessionAndTheDebriefOfTheCrewMemberItWasOpenedFor() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeDebrief.debriefs.value = mapOf(cm1Debrief, cm2Debrief)

            // When
            val viewModel = subscribedViewModel(crewMember = CrewMember.CM2)

            // Then
            val state = viewModel.state.value
            assertEquals(session, state.session)
            assertEquals(CrewMember.CM2, state.selected)
            assertEquals(cm2Debrief.second, state.debrief)
        }

    @Test
    fun selectingTheOtherCrewMemberShowsTheirDebrief() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeDebrief.debriefs.value = mapOf(cm1Debrief, cm2Debrief)
            val viewModel = subscribedViewModel(crewMember = CrewMember.CM2)

            // When
            viewModel.onSelect(CrewMember.CM1)
            advanceUntilIdle()

            // Then
            assertEquals(CrewMember.CM1, viewModel.state.value.selected)
            assertEquals(cm1Debrief.second, viewModel.state.value.debrief)
        }

    @Test
    fun aGradeIsSetForTheSelectedCrewMember() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeDebrief.debriefs.value = mapOf(cm1Debrief, cm2Debrief)
            val viewModel = subscribedViewModel(crewMember = CrewMember.CM1)
            viewModel.onSelect(CrewMember.CM2)

            // When
            viewModel.onGradeSelect(competencyId = 4, grade = Grade.EXEMPLARY)

            // Then
            assertEquals(
                listOf(
                    SESSION_ID to
                        CompetencyGrade(competencyId = 4, crewMember = CrewMember.CM2, grade = Grade.EXEMPLARY)
                ),
                setGrade.sets
            )
        }

    @Test
    fun aDatePickedIsSavedOnTheSession() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            val viewModel = subscribedViewModel(crewMember = CrewMember.CM1)

            // When
            viewModel.onDateChange(LocalDate.of(2026, 11, 5))

            // Then
            assertEquals(listOf(SESSION_ID to LocalDate.of(2026, 11, 5)), changeSessionDate.changes)
        }

    @Test
    fun backWithUngradedCompetenciesAsksAndStayKeepsTheDebrief() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeDebrief.debriefs.value = mapOf(cm1Debrief, cm2Debrief)
            val viewModel = subscribedViewModel(crewMember = CrewMember.CM2)
            val events = collectedEvents(viewModel)

            // When / Then: back
            viewModel.onBack()
            advanceUntilIdle()
            assertEquals(1, viewModel.state.value.ungradedWarning)

            // When / Then: Stay
            viewModel.onStay()
            advanceUntilIdle()
            assertNull(viewModel.state.value.ungradedWarning)
            assertEquals(emptyList<DebriefEvent>(), events)
        }

    @Test
    fun leaveOnTheMessageLeaves() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeDebrief.debriefs.value = mapOf(cm1Debrief, cm2Debrief)
            val viewModel = subscribedViewModel(crewMember = CrewMember.CM2)
            val events = collectedEvents(viewModel)
            viewModel.onBack()
            advanceUntilIdle()

            // When
            viewModel.onLeave()
            advanceUntilIdle()

            // Then
            assertNull(viewModel.state.value.ungradedWarning)
            assertEquals(listOf(DebriefEvent.Leave), events)
        }

    @Test
    fun backWithEveryCompetencyGradedLeavesAtOnce() =
        runTest {
            // Given
            observeSession.sessions.value = listOf(session)
            observeDebrief.debriefs.value = mapOf(cm1Debrief, cm2Debrief)
            val viewModel = subscribedViewModel(crewMember = CrewMember.CM1)
            val events = collectedEvents(viewModel)

            // When
            viewModel.onBack()
            advanceUntilIdle()

            // Then
            assertNull(viewModel.state.value.ungradedWarning)
            assertEquals(listOf(DebriefEvent.Leave), events)
        }

    private val cm1Debrief = debriefOf(CrewMember.CM1, grade = Grade.ADEQUATE)
    private val cm2Debrief = debriefOf(CrewMember.CM2, grade = null)

    private fun debriefOf(
        crewMember: CrewMember,
        grade: Grade?,
    ): Pair<Pair<String, CrewMember>, Debrief> =
        (SESSION_ID to crewMember) to
            Debrief(
                crewMember = crewMember,
                competencies =
                    listOf(
                        DebriefCompetency(
                            competency = Competency(id = 1, name = "Competency 1"),
                            notes = emptyList(),
                            grade = grade
                        )
                    )
            )

    private fun TestScope.subscribedViewModel(crewMember: CrewMember): DebriefViewModel {
        val viewModel =
            DebriefViewModel(
                sessionId = SESSION_ID,
                crewMember = crewMember,
                observeSession = observeSession,
                observeDebrief = observeDebrief,
                setGrade = setGrade,
                changeSessionDate = changeSessionDate
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()
        return viewModel
    }

    private fun TestScope.collectedEvents(viewModel: DebriefViewModel): List<DebriefEvent> {
        val events = mutableListOf<DebriefEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        return events
    }

    private companion object {
        const val SESSION_ID = "session-1"
    }
}
