package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    MotionSessionEntity::class,
    UserProfileEntity::class,
    CustomActionEntity::class,
    HiddenItemEntity::class
  ],
  version = 3,
  exportSchema = false
)
abstract class MotionDatabase : RoomDatabase() {
  abstract fun sessionDao(): MotionSessionDao
  abstract fun userDao(): UserProfileDao
  abstract fun customActionDao(): CustomActionDao
  abstract fun hiddenItemDao(): HiddenItemDao

  companion object {
    @Volatile
    private var INSTANCE: MotionDatabase? = null

    fun getDatabase(context: Context): MotionDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          MotionDatabase::class.java,
          "motion_collector.db"
        )
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
