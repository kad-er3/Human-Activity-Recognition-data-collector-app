package com.example.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.AxisStats
import com.example.data.model.SessionFeatureSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.sqrt

object CsvExporter {

  fun getDatasetDirectory(context: Context): File {
    val dir = File(context.filesDir, "datasets")
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  fun createSessionFile(context: Context, userName: String, actionLabel: String): File {
    val dir = getDatasetDirectory(context)
    val sanitizedUser = userName.trim().replace(Regex("[^a-zA-Z0-9_]"), "_").ifEmpty { "user" }
    val sanitizedAction = actionLabel.trim().replace(Regex("[^a-zA-Z0-9_]"), "_").ifEmpty { "action" }
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val fileName = "motion_${sanitizedUser}_${sanitizedAction}_${timeStamp}.csv"
    return File(dir, fileName)
  }

  fun getShareableUri(context: Context, file: File): Uri {
    val authority = "${context.packageName}.fileprovider"
    return FileProvider.getUriForFile(context, authority, file)
  }

  fun shareSessionFile(
    context: Context,
    file: File,
    userName: String,
    actionLabel: String,
    sampleCount: Int,
    durationSeconds: Double
  ) {
    if (!file.exists()) return

    val uri = getShareableUri(context, file)
    val formattedDuration = String.format(Locale.US, "%.1f", durationSeconds)
    val subject = "MotionML Dataset: $actionLabel ($userName)"
    val messageBody = """
      MotionML Sensor Dataset Export
      ---------------------------------
      Participant / User: $userName
      Activity / Action: $actionLabel
      Samples Collected: $sampleCount
      Duration: ${formattedDuration}s
      File: ${file.name}
      Format: CSV (Accelerometer + Gyroscope time-series for ML models)
      
      This dataset is prepared for activity recognition models, training, or cloud analysis.
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/csv"
      putExtra(Intent.EXTRA_SUBJECT, subject)
      putExtra(Intent.EXTRA_TEXT, messageBody)
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(intent, "Export Dataset to Email or Cloud")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
  }

  suspend fun shareAllDatasetsZip(
    context: Context,
    files: List<File>,
    totalActions: Int,
    totalSessions: Int
  ): Boolean = withContext(Dispatchers.IO) {
    val validFiles = files.filter { it.exists() && it.isFile }
    if (validFiles.isEmpty()) return@withContext false

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val zipFile = File(context.cacheDir, "motionml_datasets_bundle_${timeStamp}.zip")

    ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
      val buffer = ByteArray(8192)
      for (file in validFiles) {
        val entry = ZipEntry(file.name)
        zos.putNextEntry(entry)
        FileInputStream(file).use { fis ->
          var count: Int
          while (fis.read(buffer).also { count = it } != -1) {
            zos.write(buffer, 0, count)
          }
        }
        zos.closeEntry()
      }
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "application/zip"
      putExtra(Intent.EXTRA_SUBJECT, "MotionML Complete Dataset Bundle ($totalSessions sessions)")
      putExtra(
        Intent.EXTRA_TEXT,
        "Attached is a complete bundle of $totalSessions recorded motion sessions covering $totalActions distinct activities ready for Machine Learning workflows."
      )
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(intent, "Export All Datasets (ZIP) to Cloud / Email")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
    true
  }

  suspend fun readCsvPreview(file: File, maxRows: Int = 20): Pair<List<String>, List<List<String>>> =
    withContext(Dispatchers.IO) {
      if (!file.exists()) return@withContext Pair(emptyList(), emptyList())
      val headers = mutableListOf<String>()
      val rows = mutableListOf<List<String>>()

      BufferedReader(FileReader(file)).use { reader ->
        val headerLine = reader.readLine()
        if (headerLine != null) {
          headers.addAll(headerLine.split(",").map { it.trim().trim('\"') })
        }
        var line: String?
        var count = 0
        while (reader.readLine().also { line = it } != null && count < maxRows) {
          val row = line?.split(",")?.map { it.trim().trim('\"') } ?: emptyList()
          if (row.isNotEmpty()) {
            rows.add(row)
            count++
          }
        }
      }
      Pair(headers, rows)
    }

  suspend fun computeSessionFeatureSummary(file: File): SessionFeatureSummary? =
    withContext(Dispatchers.IO) {
      if (!file.exists()) return@withContext null

      var sampleCount = 0
      var maxElapsed = 0.0

      val axList = mutableListOf<Double>()
      val ayList = mutableListOf<Double>()
      val azList = mutableListOf<Double>()
      val gxList = mutableListOf<Double>()
      val gyList = mutableListOf<Double>()
      val gzList = mutableListOf<Double>()
      val aMagList = mutableListOf<Double>()
      val gMagList = mutableListOf<Double>()

      BufferedReader(FileReader(file)).use { reader ->
        // skip header
        reader.readLine()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
          val parts = line!!.split(",")
          if (parts.size >= 10) {
            try {
              val elapsed = parts[1].toDoubleOrNull() ?: 0.0
              val ax = parts[2].toDoubleOrNull() ?: 0.0
              val ay = parts[3].toDoubleOrNull() ?: 0.0
              val az = parts[4].toDoubleOrNull() ?: 0.0
              val gx = parts[5].toDoubleOrNull() ?: 0.0
              val gy = parts[6].toDoubleOrNull() ?: 0.0
              val gz = parts[7].toDoubleOrNull() ?: 0.0
              val amag = parts[8].toDoubleOrNull() ?: sqrt(ax * ax + ay * ay + az * az)
              val gmag = parts[9].toDoubleOrNull() ?: sqrt(gx * gx + gy * gy + gz * gz)

              axList.add(ax)
              ayList.add(ay)
              azList.add(az)
              gxList.add(gx)
              gyList.add(gy)
              gzList.add(gz)
              aMagList.add(amag)
              gMagList.add(gmag)

              if (elapsed > maxElapsed) maxElapsed = elapsed
              sampleCount++
            } catch (_: Exception) {}
          }
        }
      }

      if (sampleCount == 0) return@withContext null

      fun calcStats(list: List<Double>): AxisStats {
        if (list.isEmpty()) return AxisStats(0.0, 0.0, 0.0, 0.0)
        val min = list.minOrNull() ?: 0.0
        val max = list.maxOrNull() ?: 0.0
        val mean = list.average()
        val variance = list.map { (it - mean) * (it - mean) }.average()
        val std = sqrt(variance)
        return AxisStats(min, max, mean, std)
      }

      val effectiveHz = if (maxElapsed > 0) sampleCount / maxElapsed else 0.0

      SessionFeatureSummary(
        sampleCount = sampleCount,
        durationSeconds = maxElapsed,
        effectiveHz = effectiveHz,
        accelX = calcStats(axList),
        accelY = calcStats(ayList),
        accelZ = calcStats(azList),
        gyroX = calcStats(gxList),
        gyroY = calcStats(gyList),
        gyroZ = calcStats(gzList),
        accelMag = calcStats(aMagList),
        gyroMag = calcStats(gMagList)
      )
    }

  fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val clampedGroup = digitGroups.coerceIn(0, units.size - 1)
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, clampedGroup.toDouble()), units[clampedGroup])
  }
}
