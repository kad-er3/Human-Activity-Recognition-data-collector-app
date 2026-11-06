package com.example.data.model

import java.util.Locale
import kotlin.math.sqrt

data class SensorReading(
  val timestampMs: Long,
  val elapsedSeconds: Double,
  val accelX: Float,
  val accelY: Float,
  val accelZ: Float,
  val gyroX: Float,
  val gyroY: Float,
  val gyroZ: Float,
  val userName: String,
  val actionLabel: String,
  val sessionId: String
) {
  val accelMagnitude: Float
    get() = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)

  val gyroMagnitude: Float
    get() = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)

  fun toCsvRow(): String {
    return String.format(
      Locale.US,
      "%d,%.4f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,\"%s\",\"%s\",\"%s\"",
      timestampMs,
      elapsedSeconds,
      accelX,
      accelY,
      accelZ,
      gyroX,
      gyroY,
      gyroZ,
      accelMagnitude,
      gyroMagnitude,
      userName.replace("\"", "\"\""),
      actionLabel.replace("\"", "\"\""),
      sessionId
    )
  }

  companion object {
    val CSV_HEADER = "timestamp_ms,elapsed_seconds,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z,accel_magnitude,gyro_magnitude,user_name,action_label,session_id"
  }
}

data class ActionCountStat(
  val actionLabel: String,
  val count: Int,
  val totalSamples: Int,
  val totalDurationSeconds: Double
)

data class UserStat(
  val userName: String,
  val sessionCount: Int,
  val actionCount: Int,
  val totalDurationSeconds: Double
)

data class AxisStats(
  val min: Double,
  val max: Double,
  val mean: Double,
  val std: Double
)

data class SessionFeatureSummary(
  val sampleCount: Int,
  val durationSeconds: Double,
  val effectiveHz: Double,
  val accelX: AxisStats,
  val accelY: AxisStats,
  val accelZ: AxisStats,
  val gyroX: AxisStats,
  val gyroY: AxisStats,
  val gyroZ: AxisStats,
  val accelMag: AxisStats,
  val gyroMag: AxisStats
)
