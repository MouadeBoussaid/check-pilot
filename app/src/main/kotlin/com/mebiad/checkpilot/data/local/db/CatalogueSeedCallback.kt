package com.mebiad.checkpilot.data.local.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Seeds the placeholder Catalogue: Competency 1 to 9, each with OBS BEH a to g, named like "3c". It runs only when the
 * database file is created, so an app update never overwrites the Assessor's Catalogue. DAOs don't exist yet inside
 * this callback, hence the SQL.
 */
class CatalogueSeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        for (number in 1..COMPETENCIES) {
            db.execSQL(
                "INSERT INTO competency (id, name, sortIndex) VALUES (?, ?, ?)",
                arrayOf<Any>(number, "Competency $number", number - 1)
            )
            BEHAVIOUR_LETTERS.forEachIndexed { index, letter ->
                db.execSQL(
                    "INSERT INTO observable_behaviour (competencyId, name, sortIndex) VALUES (?, ?, ?)",
                    arrayOf<Any>(number, "$number$letter", index)
                )
            }
        }
    }

    private companion object {
        const val COMPETENCIES = 9
        const val BEHAVIOUR_LETTERS = "abcdefg"
    }
}
