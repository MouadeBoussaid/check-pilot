package com.mebiad.checkpilot.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** `autoGenerate` makes the id `AUTOINCREMENT`, so a deleted Competency's id is never reused (ADR 0002). */
@Entity(tableName = "competency")
data class CompetencyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortIndex: Long,
)
