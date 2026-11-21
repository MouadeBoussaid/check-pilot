package com.mebiad.checkpilot.data.local.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/** Instants are stored as epoch milliseconds, dates as ISO text (`2026-11-01`), so both sort in SQL. */
class Converters {
    @TypeConverter
    fun instantToMillis(instant: Instant): Long = instant.toEpochMilli()

    @TypeConverter
    fun millisToInstant(millis: Long): Instant = Instant.ofEpochMilli(millis)

    @TypeConverter
    fun dateToText(date: LocalDate): String = date.toString()

    @TypeConverter
    fun textToDate(text: String): LocalDate = LocalDate.parse(text)
}
