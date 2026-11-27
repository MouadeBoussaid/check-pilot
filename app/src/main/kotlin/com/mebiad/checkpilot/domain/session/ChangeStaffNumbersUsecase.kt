package com.mebiad.checkpilot.domain.session

interface ChangeStaffNumbersUsecase {
    /** Adds the Crew Member's Staff Number to the empty slot, or corrects the one it holds. */
    suspend fun set(
        sessionId: String,
        crewMember: CrewMember,
        staffNumber: StaffNumber,
    )

    /**
     * Removes the Crew Member from the Session unless [StaffNumberRemoval] says why not. A Crew Member with Grades is
     * removed, with their Grades, only when [deleteGrades] is set; otherwise nothing changes and the answer asks for
     * that confirmation.
     */
    suspend fun remove(
        sessionId: String,
        crewMember: CrewMember,
        deleteGrades: Boolean,
    ): StaffNumberRemoval
}

/** What came of removing a Crew Member's Staff Number. */
sealed interface StaffNumberRemoval {
    data object Removed : StaffNumberRemoval

    /** Refused: a Session keeps at least one Staff Number. */
    data object RefusedOnlyCrewMember : StaffNumberRemoval

    /** Refused: [count] Notes are tagged to the Crew Member and need retagging first. */
    data class RefusedNotesTagged(
        val count: Int,
    ) : StaffNumberRemoval

    /** Not yet removed: their [count] Grades would be deleted, so the Assessor confirms first. */
    data class ConfirmGradesDeletion(
        val count: Int,
    ) : StaffNumberRemoval
}
