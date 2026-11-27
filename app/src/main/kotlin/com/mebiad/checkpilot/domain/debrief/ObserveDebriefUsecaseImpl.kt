package com.mebiad.checkpilot.domain.debrief

import com.mebiad.checkpilot.domain.catalogue.Competency
import com.mebiad.checkpilot.domain.catalogue.ObserveCatalogueUsecase
import com.mebiad.checkpilot.domain.grade.CompetencyGrade
import com.mebiad.checkpilot.domain.grade.ObserveGradesUsecase
import com.mebiad.checkpilot.domain.note.CmTag
import com.mebiad.checkpilot.domain.note.Note
import com.mebiad.checkpilot.domain.note.ObserveNotesUsecase
import com.mebiad.checkpilot.domain.session.CrewMember
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class ObserveDebriefUsecaseImpl(
    private val catalogue: ObserveCatalogueUsecase,
    private val notes: ObserveNotesUsecase,
    private val grades: ObserveGradesUsecase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ObserveDebriefUsecase {
    override fun debrief(
        sessionId: String,
        crewMember: CrewMember,
    ): Flow<Debrief> =
        combine(
            catalogue.competencies(),
            notes.notes(sessionId),
            grades.grades(sessionId)
        ) { competencies, notes, grades ->
            buildDebrief(crewMember, competencies, notes, grades)
        }.flowOn(dispatcher)
}

private fun buildDebrief(
    crewMember: CrewMember,
    competencies: List<Competency>,
    notes: List<Note>,
    grades: List<CompetencyGrade>,
): Debrief {
    val notesAbout = notes.filter { it.cmTag == CmTag.BOTH || it.cmTag.name == crewMember.name }
    val gradeOf = grades.filter { it.crewMember == crewMember }.associate { it.competencyId to it.grade }
    return Debrief(
        crewMember = crewMember,
        competencies =
            competencies
                .map { competency ->
                    DebriefCompetency(
                        competency = competency,
                        notes =
                            notesAbout
                                .filter { it.competencyId == competency.id }
                                .map { note ->
                                    DebriefNote(
                                        note = note,
                                        observableBehaviour =
                                            competency.observableBehaviours.find { it.id == note.observableBehaviourId }
                                    )
                                },
                        grade = gradeOf[competency.id]
                    )
                }.sortedWith(
                    compareByDescending<DebriefCompetency> { it.notes.size }
                        .thenByDescending { it.observableBehaviours.size }
                )
    )
}
