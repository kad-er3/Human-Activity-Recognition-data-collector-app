package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.CsvExporter
import com.example.ui.components.ActivityChipPicker
import com.example.ui.components.ParticipantUserSelector
import com.example.ui.components.SensorWaveformGraph
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.MotionViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
  viewModel: MotionViewModel,
  modifier: Modifier = Modifier
) {
  val liveState by viewModel.liveSensorState.collectAsState()
  val recentReadings by viewModel.recentReadings.collectAsState()
  val userName by viewModel.userName.collectAsState()
  val actionLabel by viewModel.actionLabel.collectAsState()
  val availableUsers by viewModel.availableUsers.collectAsState()
  val allActions by viewModel.allAvailableActions.collectAsState()
  val customActions by viewModel.customActions.collectAsState()
  val deletedUsers by viewModel.deletedUsers.collectAsState()
  val deletedActions by viewModel.deletedActions.collectAsState()
  val samplingHz by viewModel.samplingHz.collectAsState()
  val lastCompletedSession by viewModel.lastCompletedSession.collectAsState()

  var showSettingsDialog by remember { mutableStateOf(false) }

  // Pulsing animation for active recording
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(800),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // Top Bar Settings & Status
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Sensors,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "Motion Data Collector",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "${samplingHz} Hz • Accelerometer + Gyroscope",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (liveState.isRecording) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(DangerRed.copy(alpha = 0.2f))
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(DangerRed)
                .alpha(pulseAlpha)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (liveState.isPaused) "PAUSED" else "REC",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = DangerRed
            )
          }
        }

        IconButton(
          onClick = { showSettingsDialog = true },
          modifier = Modifier.testTag("btn_sensor_settings")
        ) {
          Icon(imageVector = Icons.Default.Tune, contentDescription = "Sensor Settings")
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // User Identifier Section (Dynamic Users & Deletion)
    ParticipantUserSelector(
      currentUser = userName,
      availableUsers = availableUsers,
      deletedUsersList = deletedUsers,
      onUserSelected = { viewModel.setUserName(it) },
      onAddNewUser = { viewModel.addNewUser(it) },
      onDeleteUser = { name, deleteRecordings -> viewModel.deleteUser(name, deleteRecordings) },
      onRestoreUser = { viewModel.restoreUser(it) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Action / Activity Picker Section (Dynamic Actions & Deletion)
    ActivityChipPicker(
      selectedAction = actionLabel,
      actionsList = allActions,
      deletedActionsList = deletedActions,
      onActionSelected = { viewModel.setActionLabel(it) },
      onAddNewAction = { label, emoji -> viewModel.addNewAction(label, emoji) },
      onDeleteAction = { viewModel.deleteAction(it) },
      onRestoreAction = { viewModel.restoreAction(it) },
      customActionEntities = customActions
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Real-time Waveform Canvas Visualizer
    SensorWaveformGraph(
      readings = recentReadings,
      latestReading = recentReadings.lastOrNull()
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Session Metrics HUD
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      )
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "ELAPSED TIME",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          val totalSec = liveState.elapsedSeconds
          val mins = (totalSec / 60).toInt()
          val secs = (totalSec % 60).toInt()
          val millis = ((totalSec * 10) % 10).toInt()
          Text(
            text = String.format(Locale.US, "%02d:%02d.%01d", mins, secs, millis),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            fontFamily = FontFamily.Monospace,
            color = if (liveState.isRecording) DangerRed else MaterialTheme.colorScheme.onSurface
          )
        }

        Box(
          modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "SAMPLES LOGGED",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${liveState.sampleCount}",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Box(
          modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "SAMPLING RATE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          val effectiveHz = if (liveState.currentHz > 0) liveState.currentHz else samplingHz.toDouble()
          Text(
            text = String.format(Locale.US, "%.0f Hz", effectiveHz),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Primary Action Controls
    if (!liveState.isRecording) {
      Button(
        onClick = { viewModel.startRecording() },
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("btn_start_recording"),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(16.dp)
      ) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "START RECORDING (${actionLabel.uppercase()})",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Discard Button
        OutlinedButton(
          onClick = { viewModel.discardRecording() },
          modifier = Modifier
            .weight(1f)
            .height(54.dp)
            .testTag("btn_discard_recording"),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(imageVector = Icons.Default.Close, contentDescription = null)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Discard")
        }

        // Pause/Resume Button
        OutlinedButton(
          onClick = { viewModel.togglePause() },
          modifier = Modifier
            .weight(1.2f)
            .height(54.dp)
            .testTag("btn_pause_recording"),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(
            imageVector = if (liveState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            contentDescription = null
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (liveState.isPaused) "Resume" else "Pause")
        }

        // Stop & Save CSV Button
        Button(
          onClick = { viewModel.stopAndSaveRecording() },
          modifier = Modifier
            .weight(1.8f)
            .height(54.dp)
            .testTag("btn_stop_recording"),
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(imageVector = Icons.Default.Stop, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Stop & Save CSV", fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Helper Card: How this CSV helps ML Models
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      )
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint = SuccessGreen,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Generates standardized 13-column CSV files with millisecond timestamps, 3-axis Accel + Gyro, vector norms, user IDs, and action labels for direct ML training in TensorFlow, PyTorch, or Scikit-Learn.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }

  // Sensor Settings Dialog
  if (showSettingsDialog) {
    AlertDialog(
      onDismissRequest = { showSettingsDialog = false },
      title = { Text("Sensor Configuration") },
      text = {
        Column {
          Text(
            text = "Sampling Frequency (Hz):",
            style = MaterialTheme.typography.labelLarge
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(25, 50, 100).forEach { hz ->
              FilterChip(
                selected = samplingHz == hz,
                onClick = { viewModel.setSamplingHz(hz) },
                label = { Text("$hz Hz") },
                modifier = Modifier.testTag("chip_hz_$hz")
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Realistic Motion Simulation",
                style = MaterialTheme.typography.titleSmall
              )
              Text(
                text = "Generates realistic activity patterns for emulator testing or stationary environments.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = liveState.isSimulationMode,
              onCheckedChange = { viewModel.toggleSimulationMode(it) },
              modifier = Modifier.testTag("switch_simulation_mode")
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Hardware status: Accel: ${if (liveState.hasHardwareAccelerometer) "Found" else "Emulated"}, Gyro: ${if (liveState.hasHardwareGyroscope) "Found" else "Emulated"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
          )
        }
      },
      confirmButton = {
        Button(onClick = { showSettingsDialog = false }) {
          Text("Done")
        }
      }
    )
  }

  // Post-recording Success Dialog
  lastCompletedSession?.let { session ->
    AlertDialog(
      onDismissRequest = { viewModel.dismissLastCompletedDialog() },
      icon = {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(SuccessGreen.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(28.dp)
          )
        }
      },
      title = {
        Text(
          text = "Session Saved Successfully!",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "File: ${session.fileName}",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("User:", style = MaterialTheme.typography.bodyMedium)
            Text(session.userName, fontWeight = FontWeight.Bold)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Action:", style = MaterialTheme.typography.bodyMedium)
            Text(session.actionLabel, fontWeight = FontWeight.Bold)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Samples:", style = MaterialTheme.typography.bodyMedium)
            Text("${session.sampleCount} rows", fontWeight = FontWeight.Bold)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Duration:", style = MaterialTheme.typography.bodyMedium)
            Text("${String.format(Locale.US, "%.1f", session.durationSeconds)}s", fontWeight = FontWeight.Bold)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("File Size:", style = MaterialTheme.typography.bodyMedium)
            Text(CsvExporter.formatFileSize(session.fileSizeBytes), fontWeight = FontWeight.Bold)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.shareSession(session)
            viewModel.dismissLastCompletedDialog()
          },
          modifier = Modifier.testTag("btn_dialog_share_session")
        ) {
          Icon(imageVector = Icons.Default.IosShare, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Export via Email / Cloud")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { viewModel.dismissLastCompletedDialog() }) {
          Text("Close")
        }
      }
    )
  }
}
