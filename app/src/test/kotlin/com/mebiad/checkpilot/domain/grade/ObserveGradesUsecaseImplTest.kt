package com.mebiad.checkpilot.domain.grade

import app.cash.turbine.test
import com.mebiad.checkpilot.domain.session.CrewMember
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveGradesUsecaseImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = GradeRepositoryFake()
    private val usecase = ObserveGradesUsecaseImpl(repository = repository, dispatcher = dispatcher)

    @Test
    fun emitsTheSessionsGradesAgainWhenOneIsSet() =
        runTest(dispatcher) {
            // Given
            repository.set("session-2", competencyId = 1, crewMember = CrewMember.CM1, grade = Grade.EXEMPLARY)

            usecase.grades("session-1").test {
                assertEquals(emptyList<CompetencyGrade>(), awaitItem())

                // When
                repository.set("session-1", competencyId = 2, crewMember = CrewMember.CM1, grade = Grade.ADEQUATE)

                // Then
                assertEquals(
                    listOf(CompetencyGrade(competencyId = 2, crewMember = CrewMember.CM1, grade = Grade.ADEQUATE)),
                    awaitItem()
                )
            }
        }
}
