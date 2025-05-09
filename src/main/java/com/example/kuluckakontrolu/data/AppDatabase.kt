package com.example.kuluckakontrolu.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.kuluckakontrolu.model.IncubationCycle
import com.example.kuluckakontrolu.model.IncubationNote
import com.example.kuluckakontrolu.model.IncubatorHistoryRecord

@Database(
    entities = [
        IncubatorHistoryRecord::class,
        IncubationCycle::class,
        IncubationNote::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun incubatorDao(): IncubatorDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "incubator_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}