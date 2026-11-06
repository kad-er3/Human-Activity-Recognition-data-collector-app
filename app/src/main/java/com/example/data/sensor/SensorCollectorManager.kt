package com.example.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import com.example.data.db.MotionSessionEntity
import com.example.data.export.CsvExporter
import com.example.data.model.SensorReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.UUID
import kotlin.math.sin
import kotlin.random.Random

data class LiveSensorState(
  val accelX: Float = 0f,
  val accelY: Float = 9.8f,
  val accelZ: Float = 0f,
  val gyroX: Float = 0f,
  val gyroY: Float = 0f,
  val gyroZ: Float = 0f,
  val accelMagnitude: Float = 9.8f,
  val gyroMagnitude: Float = 0f,
  val isRecording: Boolean = false,
  val isPaused: Boolean = false,
  val elapsedSeconds: Double = 0.0,
  val sampleCount: Int = 0,
  val currentHz: Double = 0.0,
  val activeUser: String = "",
  val activeAction: String = "",
  val hasHardwareAccelerometer: Boolean = false,
  val hasHardwareGyroscope: Boolean = false,
  val isSimulationMode: Boolean = false
)

class SensorCollectorManager(private val context: Context) : SensorEventListener {

  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

  private val scope = CoroutineScope(Dispatchers.Default + Job())

  private val _liveState = MutableStateFlow(
    LiveSensorState(
      hasHardwareAccelerometer = accelerometer != null,
      hasHardwareGyroscope = gyroscope != null
    )
  )
  val liveState: StateFlow<LiveSensorState> = _liveState.asStateFlow()

  // Rolling buffers for live waveform visualization (last 100 points)
  private val _recentReadings = MutableStateFlow<List<SensorReading>>(emptyList())
  val recentReadings: StateFlow<List<SensorReading>> = _recentReadings.asStateFlow()

  // Current values
  private var latestAccelX = 0f
  private var latestAccelY = 9.81f
  private var latestAccelZ = 0f

  private var latestGyroX = 0f
  private var latestGyroY = 0f
  private var latestGyroZ = 0f

  // Recording session vars
  private var isRecording = false
  private var isPaused = false
  private var currentSessionId: String = ""
  private var currentUserName: String = ""
  private var currentActionLabel: String = ""
  private var sessionStartTimeMs: Long = 0L
  private var sessionStartElapsedRealtime: Long = 0L
  private var pausedDurationMs: Long = 0L
  private var pauseStartTime: Long = 0L

  private var sessionFile: File? = null
  private var fileWriter: BufferedWriter? = null
  private var recordedSampleCount = 0

  private var timerJob: Job? = null
  private var simulationJob: Job? = null
  private var isSimulationEnabled = false

  // Selected sampling delay in microseconds
  private var currentSamplingDelayUs = 20_000 // 50 Hz default

  init {
    // If no hardware sensors are available (e.g. running in basic emulator), default simulation enabled
    if (accelerometer == null || gyroscope == null) {
      isSimulationEnabled = true
      _liveState.value = _liveState.value.copy(isSimulationMode = true)
    }
    startListening()
  }

  fun setSimulationMode(enabled: Boolean) {
    isSimulationEnabled = enabled
    _liveState.value = _liveState.value.copy(isSimulationMode = enabled)
    if (enabled) {
      startSimulationLoop()
    } else {
      simulationJob?.cancel()
    }
  }

  fun setSamplingRate(hz: Int) {
    currentSamplingDelayUs = when (hz) {
      100 -> 10_000
      50 -> 20_000
      25 -> 40_000
      else -> SensorManager.SENSOR_DELAY_GAME
    }
    // Re-register listeners with new rate
    stopListening()
    startListening()
  }

  fun startListening() {
    sensorManager?.let { sm ->
      accelerometer?.let {
        sm.registerListener(this, it, currentSamplingDelayUs)
      }
      gyroscope?.let {
        sm.registerListener(this, it, currentSamplingDelayUs)
      }
    }
    if (isSimulationEnabled) {
      startSimulationLoop()
    }
  }

  fun stopListening() {
    sensorManager?.unregisterListener(this)
    simulationJob?.cancel()
  }

  private fun startSimulationLoop() {
    simulationJob?.cancel()
    simulationJob = scope.launch {
      var step = 0.0
      while (isActive) {
        val action = if (isRecording) currentActionLabel else "Walking"
        val freq = when (action.lowercase()) {
          "running" -> 2.8
          "jumping" -> 2.0
          "sitting" -> 0.25
          "standing" -> 0.1
          "cycling" -> 1.5
          else -> 1.8 // walking
        }
        val intensity = when (action.lowercase()) {
          "running" -> 14.0f
          "jumping" -> 18.0f
          "sitting" -> 0.3f
          "standing" -> 0.2f
          else -> 4.5f
        }

        step += 0.05
        val noiseX = (Random.nextFloat() - 0.5f) * 0.4f
        val noiseY = (Random.nextFloat() - 0.5f) * 0.4f
        val noiseZ = (Random.nextFloat() - 0.5f) * 0.4f

        latestAccelX = (sin(step * freq) * intensity * 0.4f).toFloat() + noiseX
        latestAccelY = 9.81f + (sin(step * freq * 2) * intensity * 0.7f).toFloat() + noiseY
        latestAccelZ = (sin(step * freq + 1.0) * intensity * 0.3f).toFloat() + noiseZ

        latestGyroX = (sin(step * freq * 1.5) * (intensity / 3.0f)).toFloat() + noiseX * 0.2f
        latestGyroY = (sin(step * freq) * (intensity / 4.0f)).toFloat() + noiseY * 0.2f
        latestGyroZ = (sin(step * freq * 2.0) * (intensity / 3.5f)).toFloat() + noiseZ * 0.2f

        processNewSample(System.currentTimeMillis())
        delay(20) // ~50Hz
      }
    }
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event == null || isSimulationEnabled) return

    val now = System.currentTimeMillis()
    when (event.sensor.type) {
      Sensor.TYPE_ACCELEROMETER -> {
        latestAccelX = event.values[0]
        latestAccelY = event.values[1]
        latestAccelZ = event.values[2]
        processNewSample(now)
      }
      Sensor.TYPE_GYROSCOPE -> {
        latestGyroX = event.values[0]
        latestGyroY = event.values[1]
        latestGyroZ = event.values[2]
      }
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    // No-op
  }

  private fun processNewSample(now: Long) {
    val aMag = kotlin.math.sqrt(latestAccelX * latestAccelX + latestAccelY * latestAccelY + latestAccelZ * latestAccelZ)
    val gMag = kotlin.math.sqrt(latestGyroX * latestGyroX + latestGyroY * latestGyroY + latestGyroZ * latestGyroZ)

    val elapsedSec = if (isRecording && !isPaused) {
      (SystemClock.elapsedRealtime() - sessionStartElapsedRealtime - pausedDurationMs) / 1000.0
    } else {
      0.0
    }

    val reading = SensorReading(
      timestampMs = now,
      elapsedSeconds = elapsedSec,
      accelX = latestAccelX,
      accelY = latestAccelY,
      accelZ = latestAccelZ,
      gyroX = latestGyroX,
      gyroY = latestGyroY,
      gyroZ = latestGyroZ,
      userName = if (isRecording) currentUserName else "",
      actionLabel = if (isRecording) currentActionLabel else "",
      sessionId = if (isRecording) currentSessionId else ""
    )

    // Update rolling waveform window
    val currentList = _recentReadings.value
    val updatedList = if (currentList.size >= 120) {
      currentList.drop(1) + reading
    } else {
      currentList + reading
    }
    _recentReadings.value = updatedList

    // If actively recording, write to CSV file
    if (isRecording && !isPaused) {
      recordedSampleCount++
      try {
        fileWriter?.write(reading.toCsvRow())
        fileWriter?.newLine()
      } catch (_: Exception) {}
    }

    // Update live state
    val hz = if (elapsedSec > 0.5) (recordedSampleCount / elapsedSec) else 0.0
    _liveState.value = _liveState.value.copy(
      accelX = latestAccelX,
      accelY = latestAccelY,
      accelZ = latestAccelZ,
      gyroX = latestGyroX,
      gyroY = latestGyroY,
      gyroZ = latestGyroZ,
      accelMagnitude = aMag,
      gyroMagnitude = gMag,
      elapsedSeconds = elapsedSec,
      sampleCount = recordedSampleCount,
      currentHz = hz
    )
  }

  fun startRecording(userName: String, actionLabel: String): Boolean {
    if (isRecording) return false

    val trimmedUser = userName.trim().ifEmpty { "Participant_1" }
    val trimmedAction = actionLabel.trim().ifEmpty { "Activity" }

    currentSessionId = UUID.randomUUID().toString()
    currentUserName = trimmedUser
    currentActionLabel = trimmedAction
    sessionStartTimeMs = System.currentTimeMillis()
    sessionStartElapsedRealtime = SystemClock.elapsedRealtime()
    pausedDurationMs = 0L
    recordedSampleCount = 0

    val file = CsvExporter.createSessionFile(context, trimmedUser, trimmedAction)
    sessionFile = file

    try {
      val writer = BufferedWriter(FileWriter(file, false))
      writer.write(SensorReading.CSV_HEADER)
      writer.newLine()
      fileWriter = writer
    } catch (e: Exception) {
      e.printStackTrace()
      return false
    }

    isRecording = true
    isPaused = false

    _liveState.value = _liveState.value.copy(
      isRecording = true,
      isPaused = false,
      elapsedSeconds = 0.0,
      sampleCount = 0,
      activeUser = trimmedUser,
      activeAction = trimmedAction
    )

    // Start timer update tick for smooth elapsed UI
    timerJob?.cancel()
    timerJob = scope.launch {
      while (isActive && isRecording) {
        if (!isPaused) {
          val elapsed = (SystemClock.elapsedRealtime() - sessionStartElapsedRealtime - pausedDurationMs) / 1000.0
          val hz = if (elapsed > 0.5) (recordedSampleCount / elapsed) else 0.0
          _liveState.value = _liveState.value.copy(
            elapsedSeconds = elapsed,
            currentHz = hz
          )
        }
        delay(100)
      }
    }

    return true
  }

  fun togglePause(): Boolean {
    if (!isRecording) return false
    isPaused = !isPaused
    if (isPaused) {
      pauseStartTime = SystemClock.elapsedRealtime()
    } else {
      pausedDurationMs += (SystemClock.elapsedRealtime() - pauseStartTime)
    }
    _liveState.value = _liveState.value.copy(isPaused = isPaused)
    return isPaused
  }

  suspend fun stopAndSaveRecording(): MotionSessionEntity? = withContext(Dispatchers.IO) {
    if (!isRecording) return@withContext null

    isRecording = false
    isPaused = false
    timerJob?.cancel()

    val endTimeMs = System.currentTimeMillis()
    val totalElapsedSec = (SystemClock.elapsedRealtime() - sessionStartElapsedRealtime - pausedDurationMs) / 1000.0

    try {
      fileWriter?.flush()
      fileWriter?.close()
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      fileWriter = null
    }

    val file = sessionFile
    val entity = if (file != null && file.exists() && recordedSampleCount > 0) {
      val rate = if (totalElapsedSec > 0) recordedSampleCount / totalElapsedSec else 0.0
      MotionSessionEntity(
        id = currentSessionId,
        userName = currentUserName,
        actionLabel = currentActionLabel,
        startTimeEpochMs = sessionStartTimeMs,
        endTimeEpochMs = endTimeMs,
        durationSeconds = totalElapsedSec,
        sampleCount = recordedSampleCount,
        samplingRateHz = rate,
        filePath = file.absolutePath,
        fileName = file.name,
        fileSizeBytes = file.length(),
        notes = "Recorded at ${String.format(java.util.Locale.US, "%.1f", rate)} Hz"
      )
    } else {
      file?.delete()
      null
    }

    _liveState.value = _liveState.value.copy(
      isRecording = false,
      isPaused = false,
      elapsedSeconds = 0.0,
      sampleCount = 0,
      currentHz = 0.0
    )

    entity
  }

  fun cancelAndDiscardRecording() {
    if (!isRecording) return
    isRecording = false
    isPaused = false
    timerJob?.cancel()

    try {
      fileWriter?.close()
    } catch (_: Exception) {}
    fileWriter = null

    sessionFile?.delete()
    sessionFile = null

    _liveState.value = _liveState.value.copy(
      isRecording = false,
      isPaused = false,
      elapsedSeconds = 0.0,
      sampleCount = 0,
      currentHz = 0.0
    )
  }
}
