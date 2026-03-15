package com.example.health_app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Meritev::class], version = 2, exportSchema = false)
abstract class MeritevDatabase : RoomDatabase() {

    abstract fun meritevDao(): MeritevDao

    companion object {
        @Volatile
        private var INSTANCE: MeritevDatabase? = null

        fun getDatabase(context: Context): MeritevDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeritevDatabase::class.java,
                    "meritev_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

