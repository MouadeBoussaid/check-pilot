package com.mebiad.checkpilot.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.mebiad.checkpilot.data.local.db.entity.CompetencyEntity
import com.mebiad.checkpilot.data.local.db.entity.GradeEntity
import com.mebiad.checkpilot.data.local.db.entity.NoteEntity
import com.mebiad.checkpilot.data.local.db.entity.ObservableBehaviourEntity
import com.mebiad.checkpilot.data.local.db.entity.SessionEntity

@Database(
    entities = [
        SessionEntity::class,
        NoteEntity::class,
        GradeEntity::class,
        CompetencyEntity::class,
        ObservableBehaviourEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class CheckPilotDatabase : RoomDatabase() {
    companion object {
        const val FILE_NAME = "check-pilot.db"

        /**
         * Builds the database on [name], a file name in the app's database folder or an absolute path. The app
         * passes SQLCipher's [openHelperFactory]; JVM tests pass null for the framework's SQLite.
         */
        fun build(
            context: Context,
            name: String,
            openHelperFactory: SupportSQLiteOpenHelper.Factory?,
        ): CheckPilotDatabase =
            Room
                .databaseBuilder(context, CheckPilotDatabase::class.java, name)
                .openHelperFactory(openHelperFactory)
                .addCallback(CatalogueSeedCallback())
                .build()
    }
}
