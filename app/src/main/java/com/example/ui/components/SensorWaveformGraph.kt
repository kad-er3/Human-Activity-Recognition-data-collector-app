package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SensorReading
import com.example.ui.theme.AxisAccelX
import com.example.ui.theme.AxisAccelY
import com.example.ui.theme.AxisAccelZ
import com.example.ui.theme.AxisGyroX
import com.example.ui.theme.AxisGyroY
import com.example.ui.theme.AxisGyroZ
import java.util.Locale

enum class GraphSensorType {
  ACCELEROMETER,
  GYROSCOPE
}

@Composable
fun SensorWaveformGraph(
  readings: List<SensorReading>,
  modifier: Modifier = Modifier,
  latestReading: SensorReading? = null
) {
  var selectedType by remember { mutableStateOf(GraphSensorType.ACCELEROMETER) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
      .padding(14.dp)
      .testTag("sensor_waveform_container")
  ) {
    // Header & Toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = if (selectedType == GraphSensorType.ACCELEROMETER) "ACCELEROMETER (m/s²)" else "GYROSCOPE (rad/s)",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Real-time 3-Axis Waveform",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = selectedType == GraphSensorType.ACCELEROMETER,
          onClick = { selectedType = GraphSensorType.ACCELEROMETER },
          label = { Text("Accel", fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier.testTag("chip_accel_graph")
        )
        FilterChip(
          selected = selectedType == GraphSensorType.GYROSCOPE,
          onClick = { selectedType = GraphSensorType.GYROSCOPE },
          label = { Text("Gyro", fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier.testTag("chip_gyro_graph")
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Canvas Plot
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(150.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0xFF070B14))
        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
    ) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .padding(vertical = 8.dp)
      ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // Draw grid lines
        drawLine(
          color = Color(0xFF1E293B),
          start = Offset(0f, centerY),
          end = Offset(width, centerY),
          strokeWidth = 1.5f
        )
        drawLine(
          color = Color(0xFF151D2C),
          start = Offset(0f, height * 0.25f),
          end = Offset(width, height * 0.25f),
          strokeWidth = 1f
        )
        drawLine(
          color = Color(0xFF151D2C),
          start = Offset(0f, height * 0.75f),
          end = Offset(width, height * 0.75f),
          strokeWidth = 1f
        )

        if (readings.size < 2) return@Canvas

        val sampleCount = readings.size
        val stepX = width / (sampleCount - 1).coerceAtLeast(1)

        // Scaling range
        val maxVal = if (selectedType == GraphSensorType.ACCELEROMETER) 22f else 6f

        fun mapY(v: Float): Float {
          val clamped = v.coerceIn(-maxVal, maxVal)
          return centerY - (clamped / maxVal) * (height / 2f - 6f)
        }

        val pathX = Path()
        val pathY = Path()
        val pathZ = Path()

        readings.forEachIndexed { i, r ->
          val px = i * stepX
          val (valX, valY, valZ) = if (selectedType == GraphSensorType.ACCELEROMETER) {
            Triple(r.accelX, r.accelY, r.accelZ)
          } else {
            Triple(r.gyroX, r.gyroY, r.gyroZ)
          }

          val pyX = mapY(valX)
          val pyY = mapY(valY)
          val pyZ = mapY(valZ)

          if (i == 0) {
            pathX.moveTo(px, pyX)
            pathY.moveTo(px, pyY)
            pathZ.moveTo(px, pyZ)
          } else {
            pathX.lineTo(px, pyX)
            pathY.lineTo(px, pyY)
            pathZ.lineTo(px, pyZ)
          }
        }

        // Draw curves with axis colors
        val (colorX, colorY, colorZ) = if (selectedType == GraphSensorType.ACCELEROMETER) {
          Triple(AxisAccelX, AxisAccelY, AxisAccelZ)
        } else {
          Triple(AxisGyroX, AxisGyroY, AxisGyroZ)
        }

        drawPath(pathX, color = colorX, style = Stroke(width = 2.4f, cap = StrokeCap.Round))
        drawPath(pathY, color = colorY, style = Stroke(width = 2.4f, cap = StrokeCap.Round))
        drawPath(pathZ, color = colorZ, style = Stroke(width = 2.4f, cap = StrokeCap.Round))
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Real-time Legend & Current Values
    val (colorX, colorY, colorZ) = if (selectedType == GraphSensorType.ACCELEROMETER) {
      Triple(AxisAccelX, AxisAccelY, AxisAccelZ)
    } else {
      Triple(AxisGyroX, AxisGyroY, AxisGyroZ)
    }

    val (currX, currY, currZ) = latestReading?.let {
      if (selectedType == GraphSensorType.ACCELEROMETER) {
        Triple(it.accelX, it.accelY, it.accelZ)
      } else {
        Triple(it.gyroX, it.gyroY, it.gyroZ)
      }
    } ?: Triple(0f, 0f, 0f)

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      AxisValueBadge(label = "X", value = currX, color = colorX)
      AxisValueBadge(label = "Y", value = currY, color = colorY)
      AxisValueBadge(label = "Z", value = currZ, color = colorZ)

      val mag = latestReading?.let {
        if (selectedType == GraphSensorType.ACCELEROMETER) it.accelMagnitude else it.gyroMagnitude
      } ?: 0f
      AxisValueBadge(label = "MAG", value = mag, color = Color.White)
    }
  }
}

@Composable
private fun AxisValueBadge(
  label: String,
  value: Float,
  color: Color
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(Color(0xFF0F172A))
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color)
    )
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = color
    )
    Spacer(modifier = Modifier.width(2.dp))
    Text(
      text = String.format(Locale.US, "%+05.2f", value),
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Medium,
      color = Color.White
    )
  }
}
