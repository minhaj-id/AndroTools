package com.bimantara.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bimantara.data.model.NoteEntity
import com.bimantara.data.model.PlannerItemEntity
import com.bimantara.data.model.ScannedDocEntity

@Database(
    entities = [NoteEntity::class, PlannerItemEntity::class, ScannedDocEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun plannerDao(): PlannerDao
    abstract fun docDao(): DocDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "multi_tools_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
