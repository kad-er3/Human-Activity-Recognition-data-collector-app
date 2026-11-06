package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.CsvExporter
import com.example.data.model.AxisStats
import com.example.ui.viewmodel.PreviewSessionState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvPreviewBottomSheet(
  previewState: PreviewSessionState,
  onDismiss: () -> Unit,
  onShare: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedTab by remember { mutableStateOf(0) } // 0: ML Features, 1: Raw Table

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("csv_preview_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = previewState.session.fileName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${previewState.session.userName} • ${previewState.session.actionLabel} • ${CsvExporter.formatFileSize(previewState.session.fileSizeBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Tab switcher
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          label = { Text("ML Statistics / Features") },
          modifier = Modifier.testTag("tab_ml_features")
        )
        FilterChip(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          label = { Text("Raw CSV Rows (Preview)") },
          modifier = Modifier.testTag("tab_raw_rows")
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (previewState.isLoading) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator()
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp)
        ) {
          if (selectedTab == 0) {
            // ML Feature Table
            val stats = previewState.stats
            if (stats != null) {
              LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                item {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.primaryContainer)
                      .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = "Total Samples: ${stats.sampleCount}",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                      text = "Freq: ${String.format(Locale.US, "%.1f", stats.effectiveHz)} Hz",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                      text = "Duration: ${String.format(Locale.US, "%.1f", stats.durationSeconds)}s",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                  }
                }

                item {
                  StatTableHeader()
                }

                item { StatTableRow("Accel X", stats.accelX, "m/s²") }
                item { StatTableRow("Accel Y", stats.accelY, "m/s²") }
                item { StatTableRow("Accel Z", stats.accelZ, "m/s²") }
                item { StatTableRow("Accel Mag", stats.accelMag, "m/s²", isHighlighted = true) }
                item { StatTableRow("Gyro X", stats.gyroX, "rad/s") }
                item { StatTableRow("Gyro Y", stats.gyroY, "rad/s") }
                item { StatTableRow("Gyro Z", stats.gyroZ, "rad/s") }
                item { StatTableRow("Gyro Mag", stats.gyroMag, "rad/s", isHighlighted = true) }
              }
            } else {
              Text(
                text = "Could not compute stats from file.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            // Raw CSV Table
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
            ) {
              // Table Header
              Row(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .padding(8.dp)
              ) {
                previewState.headers.forEach { h ->
                  Text(
                    text = h,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(110.dp)
                  )
                }
              }

              LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                items(previewState.previewRows) { row ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 4.dp, horizontal = 8.dp)
                  ) {
                    row.forEach { cell ->
                      Text(
                        text = cell,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(110.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Bottom Actions
      Button(
        onClick = onShare,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("btn_export_from_preview")
      ) {
        Icon(imageVector = Icons.Default.IosShare, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Export Dataset (Email / Drive / Cloud)")
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun StatTableHeader() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(6.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .padding(horizontal = 10.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text("Axis / Metric", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
    Text("Mean", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    Text("Std Dev", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    Text("Min", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    Text("Max", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
  }
}

@Composable
private fun StatTableRow(
  label: String,
  stats: AxisStats,
  unit: String,
  isHighlighted: Boolean = false
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(6.dp))
      .border(
        width = if (isHighlighted) 1.dp else 0.5.dp,
        color = if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        shape = RoundedCornerShape(6.dp)
      ),
    color = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
        color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1.3f)
      )
      Text(
        text = String.format(Locale.US, "%.2f", stats.mean),
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = String.format(Locale.US, "%.2f", stats.std),
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = String.format(Locale.US, "%.2f", stats.min),
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = String.format(Locale.US, "%.2f", stats.max),
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.weight(1f)
      )
    }
  }
}
