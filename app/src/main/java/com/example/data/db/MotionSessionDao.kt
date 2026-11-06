package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MotionSessionDao {
  @Query("SELECT * FROM motion_sessions ORDER BY startTimeEpochMs DESC")
  fun getAllSessions(): Flow<List<MotionSessionEntity>>

  @Query("SELECT * FROM motion_sessions WHERE id = :id LIMIT 1")
  suspend fun getSessionById(id: String): MotionSessionEntity?

  @Query("SELECT DISTINCT userName FROM motion_sessions ORDER BY userName ASC")
  fun getDistinctUsers(): Flow<List<String>>

  @Query("SELECT DISTINCT actionLabel FROM motion_sessions ORDER BY actionLabel ASC")
  fun getDistinctActions(): Flow<List<String>>

  @Query("SELECT COUNT(*) FROM motion_sessions")
  fun getTotalSessionCount(): Flow<Int>

  @Query("SELECT COUNT(DISTINCT actionLabel) FROM motion_sessions")
  fun getTotalDistinctActionsCount(): Flow<Int>

  @Query("SELECT COALESCE(SUM(sampleCount), 0) FROM motion_sessions")
  fun getTotalSamplesCount(): Flow<Long>

  @Query("SELECT COALESCE(SUM(durationSeconds), 0.0) FROM motion_sessions")
  fun getTotalDurationSeconds(): Flow<Double>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSession(session: MotionSessionEntity)

  @Delete
  suspend fun deleteSession(session: MotionSessionEntity)

  @Query("DELETE FROM motion_sessions WHERE id = :id")
  suspend fun deleteSessionById(id: String)

  @Query("DELETE FROM motion_sessions")
  suspend fun deleteAllSessions()
}
