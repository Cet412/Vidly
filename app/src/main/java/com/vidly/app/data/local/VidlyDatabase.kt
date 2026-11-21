package com.vidly.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = false)
abstract class VidlyDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: VidlyDatabase? = null

        fun getInstance(context: Context): VidlyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VidlyDatabase::class.java,
                    "vidly_history.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
