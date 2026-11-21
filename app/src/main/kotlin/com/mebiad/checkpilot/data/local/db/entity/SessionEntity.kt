package com.mebiad.checkpilot.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey val id: String,
    val createdAt: Instant,
    val date: LocalDate,
    val cm1StaffNumber: String?,
    val cm2StaffNumber: String?,
    val departureCode: String,
    val destinationCode: String,
)
