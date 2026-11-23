package com.mebiad.checkpilot.domain.grade

import com.mebiad.checkpilot.domain.session.CrewMember
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetGradeUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = GradeRepositoryFake()
    private val usecase = SetGradeUsecaseImpl(repository = repository, dispatcher = dispatcher)

    @Test
    fun aGradeSetIsSavedForTheCrewMemberAndCompetency() =
        runTest(dispatcher) {
            // Given: no Grade saved

            // When
            usecase.set("session-1", competencyId = 9, crewMember = CrewMember.CM2, grade = Grade.EFFECTIVE)
            advanceUntilIdle()

            // Then
            assertEquals(
                listOf(CompetencyGrade(competencyId = 9, crewMember = CrewMember.CM2, grade = Grade.EFFECTIVE)),
                repository.gradesOf("session-1")
            )
        }
}
