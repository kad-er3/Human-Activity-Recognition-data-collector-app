package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CsvPreviewBottomSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DatasetsScreen
import com.example.ui.screens.RecordScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MotionViewModel
import com.example.ui.viewmodel.UiMessage
import kotlinx.coroutines.flow.collectLatest

enum class AppTab(val title: String) {
  RECORD("Record"),
  DASHBOARD("Dashboard"),
  DATASETS("Datasets")
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MotionApp()
      }
    }
  }
}

@Composable
fun MotionApp(
  viewModel: MotionViewModel = viewModel()
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val snackbarHostState = remember { SnackbarHostState() }
  val previewState by viewModel.previewState.collectAsState()

  // Handle system back button: return to record tab if in sub-tab
  BackHandler(enabled = selectedTab != 0) {
    selectedTab = 0
  }

  // Listen to UI messages from ViewModel
  LaunchedEffect(Unit) {
    viewModel.uiEvents.collectLatest { event ->
      when (event) {
        is UiMessage.Success -> snackbarHostState.showSnackbar(event.message)
        is UiMessage.Info -> snackbarHostState.showSnackbar(event.message)
        is UiMessage.Error -> snackbarHostState.showSnackbar(event.message)
      }
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      NavigationBar(modifier = Modifier.testTag("app_bottom_bar")) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(imageVector = Icons.Default.Sensors, contentDescription = "Record") },
          label = { Text("Record") },
          modifier = Modifier.testTag("nav_item_record")
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = { Icon(imageVector = Icons.Default.BarChart, contentDescription = "Dashboard") },
          label = { Text("Dashboard") },
          modifier = Modifier.testTag("nav_item_dashboard")
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(imageVector = Icons.Default.Folder, contentDescription = "Datasets") },
          label = { Text("Datasets") },
          modifier = Modifier.testTag("nav_item_datasets")
        )
      }
    }
  ) { innerPadding ->
    when (selectedTab) {
      0 -> RecordScreen(
        viewModel = viewModel,
        modifier = Modifier.padding(innerPadding)
      )
      1 -> DashboardScreen(
        viewModel = viewModel,
        onNavigateToRecord = { selectedTab = 0 },
        onNavigateToDatasets = { selectedTab = 2 },
        modifier = Modifier.padding(innerPadding)
      )
      2 -> DatasetsScreen(
        viewModel = viewModel,
        modifier = Modifier.padding(innerPadding)
      )
    }

    // CSV Details / Preview Bottom Sheet
    previewState?.let { state ->
      CsvPreviewBottomSheet(
        previewState = state,
        onDismiss = { viewModel.dismissPreviewDialog() },
        onShare = {
          viewModel.shareSession(state.session)
        }
      )
    }
  }
}
