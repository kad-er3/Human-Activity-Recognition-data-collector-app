package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.MotionSessionEntity
import com.example.ui.components.SessionItemCard
import com.example.ui.viewmodel.MotionViewModel

@Composable
fun DatasetsScreen(
  viewModel: MotionViewModel,
  modifier: Modifier = Modifier
) {
  val sessions by viewModel.filteredSessions.collectAsState()
  val allSessions by viewModel.allSessions.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val filterUser by viewModel.filterUser.collectAsState()
  val filterAction by viewModel.filterAction.collectAsState()
  val distinctUsers by viewModel.distinctUsers.collectAsState()
  val distinctActions by viewModel.distinctActions.collectAsState()

  var showDeleteAllDialog by remember { mutableStateOf(false) }
  var sessionToDelete by remember { mutableStateOf<MotionSessionEntity?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // Header & Actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Saved Datasets",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "${allSessions.size} CSV recordings stored",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Row {
        if (allSessions.isNotEmpty()) {
          IconButton(
            onClick = { viewModel.exportAllDatasetsZip() },
            modifier = Modifier.testTag("btn_export_all_datasets")
          ) {
            Icon(
              imageVector = Icons.Default.CloudUpload,
              contentDescription = "Export All (ZIP)",
              tint = MaterialTheme.colorScheme.primary
            )
          }
          IconButton(
            onClick = { showDeleteAllDialog = true },
            modifier = Modifier.testTag("btn_clear_all_datasets")
          ) {
            Icon(
              imageVector = Icons.Default.DeleteSweep,
              contentDescription = "Clear All Datasets",
              tint = MaterialTheme.colorScheme.error
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Search Box
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { viewModel.setSearchQuery(it) },
      placeholder = { Text("Search by user, action, or filename...") },
      leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { viewModel.setSearchQuery("") }) {
            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("search_datasets_input")
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Filter Chips for Users and Actions
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.FilterList,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
      )

      FilterChip(
        selected = filterUser == null && filterAction == null,
        onClick = {
          viewModel.setFilterUser(null)
          viewModel.setFilterAction(null)
        },
        label = { Text("All (${allSessions.size})") },
        modifier = Modifier.testTag("filter_chip_all")
      )

      // User filters
      distinctUsers.forEach { user ->
        val isSelected = filterUser.equals(user, ignoreCase = true)
        FilterChip(
          selected = isSelected,
          onClick = {
            viewModel.setFilterUser(if (isSelected) null else user)
          },
          label = { Text("User: $user") },
          modifier = Modifier.testTag("filter_user_$user")
        )
      }

      // Action filters
      distinctActions.forEach { act ->
        val isSelected = filterAction.equals(act, ignoreCase = true)
        FilterChip(
          selected = isSelected,
          onClick = {
            viewModel.setFilterAction(if (isSelected) null else act)
          },
          label = { Text("Action: $act") },
          modifier = Modifier.testTag("filter_action_$act")
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // List of Sessions
    if (sessions.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = if (allSessions.isEmpty()) "No recordings yet" else "No matching datasets found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(sessions, key = { it.id }) { session ->
          SessionItemCard(
            session = session,
            onShare = { viewModel.shareSession(session) },
            onPreview = { viewModel.openCsvPreview(session) },
            onDelete = { sessionToDelete = session }
          )
        }
        item {
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }

  // Confirm delete single session dialog
  sessionToDelete?.let { session ->
    AlertDialog(
      onDismissRequest = { sessionToDelete = null },
      title = { Text("Delete Dataset?") },
      text = {
        Text("Are you sure you want to permanently delete '${session.fileName}'? This cannot be undone.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteSession(session)
            sessionToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { sessionToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Confirm delete all dialog
  if (showDeleteAllDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteAllDialog = false },
      title = { Text("Clear All Datasets?") },
      text = {
        Text("This will permanently delete all ${allSessions.size} CSV files and reset the database.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteAllSessions()
            showDeleteAllDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Clear All")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showDeleteAllDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
