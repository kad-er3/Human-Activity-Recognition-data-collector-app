package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "motion_sessions")
data class MotionSessionEntity(
  @PrimaryKey
  val id: String,
  val userName: String,
  val actionLabel: String,
  val startTimeEpochMs: Long,
  val endTimeEpochMs: Long,
  val durationSeconds: Double,
  val sampleCount: Int,
  val samplingRateHz: Double,
  val filePath: String,
  val fileName: String,
  val fileSizeBytes: Long,
  val notes: String = ""
)
