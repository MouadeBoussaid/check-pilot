package com.mebiad.checkpilot.domain.session

class ChangeStaffNumbersUsecaseFake : ChangeStaffNumbersUsecase {
    /** Each Staff Number set, in order. */
    val sets = mutableListOf<Triple<String, CrewMember, StaffNumber>>()

    /** Each removal asked for, in order, with whether it deleted Grades. */
    val removals = mutableListOf<Triple<String, CrewMember, Boolean>>()

    /** What [remove] answers when asked to keep Grades. */
    var removalKeepingGrades: StaffNumberRemoval = StaffNumberRemoval.Removed

    /** What [remove] answers when allowed to delete Grades. */
    var removalDeletingGrades: StaffNumberRemoval = StaffNumberRemoval.Removed

    override suspend fun set(
        sessionId: String,
        crewMember: CrewMember,
        staffNumber: StaffNumber,
    ) {
        sets += Triple(sessionId, crewMember, staffNumber)
    }

    override suspend fun remove(
        sessionId: String,
        crewMember: CrewMember,
        deleteGrades: Boolean,
    ): StaffNumberRemoval {
        removals += Triple(sessionId, crewMember, deleteGrades)
        return if (deleteGrades) removalDeletingGrades else removalKeepingGrades
    }

    fun reset() {
        sets.clear()
        removals.clear()
        removalKeepingGrades = StaffNumberRemoval.Removed
        removalDeletingGrades = StaffNumberRemoval.Removed
    }
}
