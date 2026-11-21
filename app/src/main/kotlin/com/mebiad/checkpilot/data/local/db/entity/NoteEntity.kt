package com.mebiad.checkpilot.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** [cmTag] is the code `CM1`, `CM2` or `BOTH`. */
@Entity(
    tableName = "note",
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
        ),
        ForeignKey(
            entity = ObservableBehaviourEntity::class,
            parentColumns = ["id"],
            childColumns = ["observableBehaviourId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("sessionId"), Index("competencyId"), Index("observableBehaviourId")]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val competencyId: Long,
    val text: String,
    @ColumnInfo(defaultValue = "BOTH") val cmTag: String = "BOTH",
    val observableBehaviourId: Long?,
    val sortIndex: Long,
)
