package com.mebiad.checkpilot.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** `autoGenerate` makes the id `AUTOINCREMENT`, so a deleted OBS BEH's id is never reused (ADR 0002). */
@Entity(
    tableName = "observable_behaviour",
    foreignKeys = [
        ForeignKey(
            entity = CompetencyEntity::class,
            parentColumns = ["id"],
            childColumns = ["competencyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("competencyId")]
)
data class ObservableBehaviourEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val competencyId: Long,
    val name: String,
    val sortIndex: Long,
)
