package com.mebiad.checkpilot.domain.debrief

import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.catalogue.ObservableBehaviour
import com.mebiad.checkpilot.domain.catalogue.ObserveCatalogueUsecaseFake
import com.mebiad.checkpilot.domain.grade.CompetencyGrade
import com.mebiad.checkpilot.domain.grade.Grade
import com.mebiad.checkpilot.domain.grade.ObserveGradesUsecaseFake
import com.mebiad.checkpilot.domain.note.CmTag
import com.mebiad.checkpilot.domain.note.Note
import com.mebiad.checkpilot.domain.note.ObserveNotesUsecaseFake
import com.mebiad.checkpilot.domain.session.CrewMember
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveDebriefUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val catalogue = ObserveCatalogueUsecaseFake()
    private val notes = ObserveNotesUsecaseFake()
    private val grades = ObserveGradesUsecaseFake()
    private val usecase =
        ObserveDebriefUsecaseImpl(catalogue = catalogue, notes = notes, grades = grades, dispatcher = dispatcher)

    @Test
    fun aCrewMembersDebriefHoldsTheirOwnAndBothNotesWithTheirObsBeh() =
        runTest(dispatcher) {
            // Given
            catalogue.competencies.value = listOf(competency(1, obsBeh = listOf(11L, 12L)))
            val own = note("own", competencyId = 1, cmTag = CmTag.CM1, observableBehaviourId = 11)
            val both = note("both", competencyId = 1, cmTag = CmTag.BOTH)
            notes.notes.value = listOf(own, note("other", competencyId = 1, cmTag = CmTag.CM2), both)

            // When
            val debrief = usecase.debrief(SESSION, CrewMember.CM1).first()

            // Then
            assertEquals(
                listOf(DebriefNote(own, observableBehaviour(11, competencyId = 1)), DebriefNote(both, null)),
                debrief.competencies.single().notes
            )
        }

    @Test
    fun competenciesWithMoreNotesAboutTheCrewMemberAreListedHigher() =
        runTest(dispatcher) {
            // Given
            catalogue.competencies.value = listOf(competency(1), competency(2), competency(3))
            notes.notes.value =
                listOf(
                    note("a", competencyId = 1, cmTag = CmTag.CM1),
                    note("b", competencyId = 3, cmTag = CmTag.CM1),
                    note("c", competencyId = 3, cmTag = CmTag.BOTH),
                    note("d", competencyId = 2, cmTag = CmTag.CM2),
                    note("e", competencyId = 2, cmTag = CmTag.CM2)
                )

            // When
            val debrief = usecase.debrief(SESSION, CrewMember.CM1).first()

            // Then
            assertEquals(listOf(3L, 1L, 2L), debrief.competencies.map { it.competency.id })
        }

    @Test
    fun aTieInNotesIsBrokenByMoreDistinctObsBehAndAFullTieKeepsCatalogueOrder() =
        runTest(dispatcher) {
            // Given
            catalogue.competencies.value =
                listOf(
                    competency(1, obsBeh = listOf(11L)),
                    competency(2, obsBeh = listOf(21L)),
                    competency(3, obsBeh = listOf(31L, 32L)),
                    competency(4, obsBeh = listOf(41L, 42L))
                )
            notes.notes.value =
                listOf(
                    note("a", competencyId = 1, cmTag = CmTag.CM1, observableBehaviourId = 11),
                    note("b", competencyId = 1, cmTag = CmTag.CM1, observableBehaviourId = 11),
                    note("c", competencyId = 2, cmTag = CmTag.CM1, observableBehaviourId = 21),
                    note("d", competencyId = 2, cmTag = CmTag.CM1, observableBehaviourId = 21),
                    note("e", competencyId = 4, cmTag = CmTag.CM1, observableBehaviourId = 41),
                    note("f", competencyId = 4, cmTag = CmTag.BOTH, observableBehaviourId = 42),
                    note("g", competencyId = 3, cmTag = CmTag.BOTH, observableBehaviourId = 31),
                    note("h", competencyId = 3, cmTag = CmTag.CM1, observableBehaviourId = 32)
                )

            // When
            val debrief = usecase.debrief(SESSION, CrewMember.CM1).first()

            // Then
            assertEquals(listOf(3L, 4L, 1L, 2L), debrief.competencies.map { it.competency.id })
        }

    @Test
    fun eachCompetencyCarriesTheCrewMembersOwnGrade() =
        runTest(dispatcher) {
            // Given
            catalogue.competencies.value = listOf(competency(1), competency(2))
            grades.grades.value =
                mapOf(
                    SESSION to
                        listOf(
                            CompetencyGrade(competencyId = 1, crewMember = CrewMember.CM1, grade = Grade.ADEQUATE),
                            CompetencyGrade(competencyId = 2, crewMember = CrewMember.CM2, grade = Grade.EXEMPLARY)
                        ),
                    "other-session" to
                        listOf(CompetencyGrade(competencyId = 2, crewMember = CrewMember.CM1, grade = Grade.EFFECTIVE))
                )

            // When
            val debrief = usecase.debrief(SESSION, CrewMember.CM1).first()

            // Then
            assertEquals(
                listOf(1L to Grade.ADEQUATE, 2L to null),
                debrief.competencies.map { it.competency.id to it.grade }
            )
        }

    @Test
    fun theSummaryCountsTheCrewMembersNotesDistinctObsBehAndGrades() =
        runTest(dispatcher) {
            // Given
            catalogue.competencies.value =
                listOf(competency(1, obsBeh = listOf(11L, 12L)), competency(2, obsBeh = listOf(21L)), competency(3))
            notes.notes.value =
                listOf(
                    note("a", competencyId = 1, cmTag = CmTag.CM1, observableBehaviourId = 11),
                    note("b", competencyId = 1, cmTag = CmTag.BOTH, observableBehaviourId = 11),
                    note("c", competencyId = 1, cmTag = CmTag.CM2, observableBehaviourId = 12),
                    note("d", competencyId = 2, cmTag = CmTag.BOTH, observableBehaviourId = 21),
                    note("e", competencyId = 3, cmTag = CmTag.CM1)
                )
            grades.grades.value =
                mapOf(
                    SESSION to
                        listOf(
                            CompetencyGrade(competencyId = 1, crewMember = CrewMember.CM1, grade = Grade.NOT_OBSERVED),
                            CompetencyGrade(competencyId = 3, crewMember = CrewMember.CM1, grade = Grade.EFFECTIVE),
                            CompetencyGrade(competencyId = 2, crewMember = CrewMember.CM2, grade = Grade.EFFECTIVE),
                            CompetencyGrade(competencyId = 99, crewMember = CrewMember.CM1, grade = Grade.EFFECTIVE)
                        )
                )

            // When
            val debrief = usecase.debrief(SESSION, CrewMember.CM1).first()

            // Then
            assertEquals(4, debrief.noteCount)
            assertEquals(2, debrief.observableBehaviourCount)
            assertEquals(2, debrief.gradedCount)
            assertEquals(1, debrief.ungradedCount)
        }

    private fun competency(
        id: Long,
        obsBeh: List<Long> = emptyList(),
    ): Competency =
        Competency(
            id = id,
            name = "Competency $id",
            observableBehaviours = obsBeh.map { observableBehaviour(it, competencyId = id) }
        )

    private fun observableBehaviour(
        id: Long,
        competencyId: Long,
    ): ObservableBehaviour = ObservableBehaviour(id = id, competencyId = competencyId, name = "$competencyId-$id")

    private fun note(
        id: String,
        competencyId: Long,
        cmTag: CmTag,
        observableBehaviourId: Long? = null,
    ): Note =
        Note(
            id = id,
            sessionId = SESSION,
            competencyId = competencyId,
            text = "Note $id",
            cmTag = cmTag,
            observableBehaviourId = observableBehaviourId,
            sortIndex = nextSortIndex++
        )

    private var nextSortIndex = 0L

    private companion object {
        const val SESSION = "session-1"
    }
}
