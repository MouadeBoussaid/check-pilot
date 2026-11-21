package com.mebiad.checkpilot.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * [crewMember] is `CM1` or `CM2`. [value] is a stable code such as `EFFECTIVE`, never an ordinal, so reordering the
 * Grades can't change saved Sessions. No row means no Grade.
 */
@Entity(
    tableName = "grade",
    primaryKeys = ["sessionId", "competencyId", "crewMember"],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CompetencyEntity::class,
            parentColumns = ["id"],
            childColumns = ["competencyId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("competencyId")]
)
data class GradeEntity(
    val sessionId: String,
    val competencyId: Long,
    val crewMember: String,
    val value: String,
)
