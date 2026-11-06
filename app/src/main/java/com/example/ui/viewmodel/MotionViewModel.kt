package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.CustomActionEntity
import com.example.data.db.HiddenItemEntity
import com.example.data.db.MotionDatabase
import com.example.data.db.MotionSessionEntity
import com.example.data.db.UserProfileEntity
import com.example.data.export.CsvExporter
import com.example.data.model.ActionCountStat
import com.example.data.model.ActivityPreset
import com.example.data.model.ActivityPresets
import com.example.data.model.SessionFeatureSummary
import com.example.data.model.UserStat
import com.example.data.sensor.LiveSensorState
import com.example.data.sensor.SensorCollectorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface UiMessage {
  data class Success(val message: String) : UiMessage
  data class Info(val message: String) : UiMessage
  data class Error(val message: String) : UiMessage
}

data class PreviewSessionState(
  val session: MotionSessionEntity,
  val headers: List<String> = emptyList(),
  val previewRows: List<List<String>> = emptyList(),
  val stats: SessionFeatureSummary? = null,
  val isLoading: Boolean = true
)

class MotionViewModel(application: Application) : AndroidViewModel(application) {

  private val context = application.applicationContext
  private val database = MotionDatabase.getDatabase(context)
  private val sessionDao = database.sessionDao()
  private val userDao = database.userDao()
  private val customActionDao = database.customActionDao()
  private val hiddenItemDao = database.hiddenItemDao()
  val sensorManager = SensorCollectorManager(context)

  val liveSensorState: StateFlow<LiveSensorState> = sensorManager.liveState
  val recentReadings = sensorManager.recentReadings

  // Input states
  private val _userName = MutableStateFlow("User_01")
  val userName: StateFlow<String> = _userName.asStateFlow()

  // Default action is the first in user's specified list: "stand up"
  private val _actionLabel = MutableStateFlow(ActivityPresets.DEFAULT_LIST.first().label)
  val actionLabel: StateFlow<String> = _actionLabel.asStateFlow()

  private val _samplingHz = MutableStateFlow(50)
  val samplingHz: StateFlow<Int> = _samplingHz.asStateFlow()

  // Filter states for Datasets screen
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _filterUser = MutableStateFlow<String?>(null)
  val filterUser: StateFlow<String?> = _filterUser.asStateFlow()

  private val _filterAction = MutableStateFlow<String?>(null)
  val filterAction: StateFlow<String?> = _filterAction.asStateFlow()

  // All recorded sessions from Room
  val allSessions: StateFlow<List<MotionSessionEntity>> = sessionDao.getAllSessions()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Dynamic user profiles from Room combined with users from sessions and defaults
  val savedUserProfiles: StateFlow<List<UserProfileEntity>> = userDao.getAllUsers()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Deleted / hidden items from Room
  val deletedUsers: StateFlow<List<String>> = hiddenItemDao.getHiddenNamesByType("USER")
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val deletedActions: StateFlow<List<String>> = hiddenItemDao.getHiddenNamesByType("ACTION")
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val availableUsers: StateFlow<List<String>> = combine(
    savedUserProfiles,
    sessionDao.getDistinctUsers(),
    deletedUsers
  ) { profiles, sessionUsers, hiddenUsers ->
    val defaultBase = listOf("User_01", "User_02", "Subject_A")
    val combined = (defaultBase + profiles.map { it.name } + sessionUsers)
      .map { it.trim() }
      .filter { it.isNotEmpty() && !hiddenUsers.contains(it) }
      .distinct()

    if (combined.isEmpty()) {
      listOf("User_01")
    } else {
      combined
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("User_01", "User_02", "Subject_A"))

  // Dynamic custom actions from Room
  val customActions: StateFlow<List<CustomActionEntity>> = customActionDao.getAllCustomActions()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Combined full list of actions: Default Presets + Dynamically Added Actions - Deleted Actions
  val allAvailableActions: StateFlow<List<ActivityPreset>> = combine(
    customActions,
    deletedActions
  ) { customList, hiddenActs ->
    val defaultPresets = ActivityPresets.DEFAULT_LIST
    val customPresets = customList.map {
      ActivityPreset(
        id = "custom_${it.label.lowercase().replace(" ", "_")}",
        label = it.label,
        emoji = it.emoji,
        category = "Custom Action",
        description = "User-created dynamic action"
      )
    }
    val all = defaultPresets + customPresets.filter { cp ->
      defaultPresets.none { dp -> dp.label.equals(cp.label, ignoreCase = true) }
    }
    val filtered = all.filter { act ->
      !hiddenActs.any { it.equals(act.label, ignoreCase = true) }
    }
    if (filtered.isEmpty()) {
      defaultPresets.take(1)
    } else {
      filtered
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActivityPresets.DEFAULT_LIST)

  // Filtered sessions
  val filteredSessions: StateFlow<List<MotionSessionEntity>> = combine(
    allSessions,
    _searchQuery,
    _filterUser,
    _filterAction
  ) { sessions, query, userFilter, actionFilter ->
    sessions.filter { session ->
      val matchesQuery = query.isEmpty() ||
          session.actionLabel.contains(query, ignoreCase = true) ||
          session.userName.contains(query, ignoreCase = true) ||
          session.fileName.contains(query, ignoreCase = true)
      val matchesUser = userFilter == null || session.userName.equals(userFilter, ignoreCase = true)
      val matchesAction = actionFilter == null || session.actionLabel.equals(actionFilter, ignoreCase = true)
      matchesQuery && matchesUser && matchesAction
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Distinct users and actions for quick filtering
  val distinctUsers: StateFlow<List<String>> = availableUsers

  val distinctActions: StateFlow<List<String>> = sessionDao.getDistinctActions()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Dashboard Aggregates
  val actionDistributionStats: StateFlow<List<ActionCountStat>> = allSessions.map { sessions ->
    sessions.groupBy { it.actionLabel }
      .map { (label, list) ->
        ActionCountStat(
          actionLabel = label,
          count = list.size,
          totalSamples = list.sumOf { it.sampleCount },
          totalDurationSeconds = list.sumOf { it.durationSeconds }
        )
      }
      .sortedByDescending { it.count }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val userStats: StateFlow<List<UserStat>> = allSessions.map { sessions ->
    sessions.groupBy { it.userName }
      .map { (user, list) ->
        UserStat(
          userName = user,
          sessionCount = list.size,
          actionCount = list.map { it.actionLabel }.distinct().size,
          totalDurationSeconds = list.sumOf { it.durationSeconds }
        )
      }
      .sortedByDescending { it.sessionCount }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Just-finished session for post-recording summary modal
  private val _lastCompletedSession = MutableStateFlow<MotionSessionEntity?>(null)
  val lastCompletedSession: StateFlow<MotionSessionEntity?> = _lastCompletedSession.asStateFlow()

  // Preview bottom sheet state
  private val _previewState = MutableStateFlow<PreviewSessionState?>(null)
  val previewState: StateFlow<PreviewSessionState?> = _previewState.asStateFlow()

  // Notifications
  private val _uiEvents = MutableSharedFlow<UiMessage>()
  val uiEvents: SharedFlow<UiMessage> = _uiEvents.asSharedFlow()

  fun setUserName(name: String) {
    _userName.value = name
  }

  // Add user dynamically
  fun addNewUser(name: String) {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return
    viewModelScope.launch {
      hiddenItemDao.unhideItem(trimmed, "USER")
      userDao.insertUser(UserProfileEntity(name = trimmed))
      _userName.value = trimmed
      _uiEvents.emit(UiMessage.Success("Utilisateur '$trimmed' ajouté"))
    }
  }

  // Delete user (with optional deletion of associated sessions)
  fun deleteUser(name: String, deleteRecordings: Boolean = false) {
    viewModelScope.launch {
      val trimmed = name.trim()
      hiddenItemDao.insertHiddenItem(
        HiddenItemEntity(id = "USER:$trimmed", name = trimmed, type = "USER")
      )
      userDao.deleteUserByName(trimmed)

      if (deleteRecordings) {
        val sessionsToDelete = allSessions.value.filter { it.userName.equals(trimmed, ignoreCase = true) }
        sessionsToDelete.forEach {
          val f = File(it.filePath)
          if (f.exists()) f.delete()
          sessionDao.deleteSession(it)
        }
      }

      // If active user was deleted, switch to next available user
      if (_userName.value.equals(trimmed, ignoreCase = true)) {
        val remaining = availableUsers.value.filterNot { it.equals(trimmed, ignoreCase = true) }
        _userName.value = remaining.firstOrNull() ?: "User_01"
      }

      _uiEvents.emit(UiMessage.Success("Utilisateur '$trimmed' supprimé"))
    }
  }

  fun restoreUser(name: String) {
    viewModelScope.launch {
      val trimmed = name.trim()
      hiddenItemDao.unhideItem(trimmed, "USER")
      userDao.insertUser(UserProfileEntity(name = trimmed))
      _uiEvents.emit(UiMessage.Success("Utilisateur '$trimmed' restauré"))
    }
  }

  fun setActionLabel(action: String) {
    _actionLabel.value = action
  }

  // Add action dynamically
  fun addNewAction(label: String, emoji: String = "⚡") {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) return
    viewModelScope.launch {
      hiddenItemDao.unhideItem(trimmed, "ACTION")
      customActionDao.insertCustomAction(
        CustomActionEntity(
          label = trimmed,
          emoji = emoji.ifBlank { "⚡" }
        )
      )
      _actionLabel.value = trimmed
      _uiEvents.emit(UiMessage.Success("Action '$trimmed' ajoutée"))
    }
  }

  // Delete action (custom or default)
  fun deleteAction(label: String) {
    viewModelScope.launch {
      val trimmed = label.trim()
      customActionDao.deleteCustomActionByLabel(trimmed)
      hiddenItemDao.insertHiddenItem(
        HiddenItemEntity(id = "ACTION:$trimmed", name = trimmed, type = "ACTION")
      )
      // If active action was deleted, switch to next available action
      if (_actionLabel.value.equals(trimmed, ignoreCase = true)) {
        val remaining = allAvailableActions.value.filterNot { it.label.equals(trimmed, ignoreCase = true) }
        _actionLabel.value = remaining.firstOrNull()?.label ?: "stand up"
      }
      _uiEvents.emit(UiMessage.Success("Action '$trimmed' supprimée"))
    }
  }

  fun restoreAction(label: String) {
    viewModelScope.launch {
      val trimmed = label.trim()
      hiddenItemDao.unhideItem(trimmed, "ACTION")
      _uiEvents.emit(UiMessage.Success("Action '$trimmed' restaurée"))
    }
  }

  fun setSamplingHz(hz: Int) {
    _samplingHz.value = hz
    sensorManager.setSamplingRate(hz)
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setFilterUser(user: String?) {
    _filterUser.value = user
  }

  fun setFilterAction(action: String?) {
    _filterAction.value = action
  }

  fun dismissLastCompletedDialog() {
    _lastCompletedSession.value = null
  }

  fun dismissPreviewDialog() {
    _previewState.value = null
  }

  fun toggleSimulationMode(enabled: Boolean) {
    sensorManager.setSimulationMode(enabled)
  }

  fun startRecording() {
    val currentName = _userName.value.trim().ifEmpty { "User_01" }
    val currentAction = _actionLabel.value.trim().ifEmpty { "stand up" }
    val started = sensorManager.startRecording(currentName, currentAction)
    if (started) {
      viewModelScope.launch {
        _uiEvents.emit(UiMessage.Info("Enregistrement démarré : $currentAction ($currentName)"))
      }
    }
  }

  fun togglePause() {
    val isPaused = sensorManager.togglePause()
    viewModelScope.launch {
      _uiEvents.emit(UiMessage.Info(if (isPaused) "Enregistrement en pause" else "Enregistrement repris"))
    }
  }

  fun discardRecording() {
    sensorManager.cancelAndDiscardRecording()
    viewModelScope.launch {
      _uiEvents.emit(UiMessage.Info("Enregistrement annulé"))
    }
  }

  fun stopAndSaveRecording() {
    viewModelScope.launch {
      val session = sensorManager.stopAndSaveRecording()
      if (session != null) {
        sessionDao.insertSession(session)
        // Ensure user is remembered in user profile table
        userDao.insertUser(UserProfileEntity(name = session.userName))
        _lastCompletedSession.value = session
        _uiEvents.emit(
          UiMessage.Success(
            "Fichier ${session.fileName} sauvegardé (${session.sampleCount} lignes, ${String.format(java.util.Locale.US, "%.1f", session.durationSeconds)}s)"
          )
        )
      } else {
        _uiEvents.emit(UiMessage.Error("Enregistrement trop court ou aucun échantillon capturé"))
      }
    }
  }

  fun shareSession(session: MotionSessionEntity) {
    val file = File(session.filePath)
    if (file.exists()) {
      CsvExporter.shareSessionFile(
        context = context,
        file = file,
        userName = session.userName,
        actionLabel = session.actionLabel,
        sampleCount = session.sampleCount,
        durationSeconds = session.durationSeconds
      )
    } else {
      viewModelScope.launch {
        _uiEvents.emit(UiMessage.Error("Fichier CSV introuvable sur le disque"))
      }
    }
  }

  fun exportAllDatasetsZip() {
    viewModelScope.launch {
      val sessions = allSessions.value
      val files = sessions.map { File(it.filePath) }.filter { it.exists() }
      if (files.isEmpty()) {
        _uiEvents.emit(UiMessage.Info("Aucun dataset à exporter"))
        return@launch
      }
      val distinctActionsCount = sessions.map { it.actionLabel }.distinct().size
      val success = CsvExporter.shareAllDatasetsZip(
        context = context,
        files = files,
        totalActions = distinctActionsCount,
        totalSessions = sessions.size
      )
      if (success) {
        _uiEvents.emit(UiMessage.Success("${files.size} datasets zippés pour l'export cloud/email"))
      }
    }
  }

  fun openCsvPreview(session: MotionSessionEntity) {
    _previewState.value = PreviewSessionState(session = session, isLoading = true)
    viewModelScope.launch {
      val file = File(session.filePath)
      val (headers, rows) = CsvExporter.readCsvPreview(file, maxRows = 25)
      val stats = CsvExporter.computeSessionFeatureSummary(file)
      _previewState.value = PreviewSessionState(
        session = session,
        headers = headers,
        previewRows = rows,
        stats = stats,
        isLoading = false
      )
    }
  }

  fun deleteSession(session: MotionSessionEntity) {
    viewModelScope.launch {
      try {
        val file = File(session.filePath)
        if (file.exists()) {
          file.delete()
        }
        sessionDao.deleteSession(session)
        _uiEvents.emit(UiMessage.Success("Session ${session.fileName} supprimée"))
      } catch (e: Exception) {
        _uiEvents.emit(UiMessage.Error("Erreur lors de la suppression : ${e.message}"))
      }
    }
  }

  fun deleteAllSessions() {
    viewModelScope.launch {
      try {
        val sessions = allSessions.value
        sessions.forEach {
          val f = File(it.filePath)
          if (f.exists()) f.delete()
        }
        sessionDao.deleteAllSessions()
        _uiEvents.emit(UiMessage.Success("Tous les datasets ont été effacés"))
      } catch (e: Exception) {
        _uiEvents.emit(UiMessage.Error("Erreur : ${e.message}"))
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    sensorManager.stopListening()
  }
}
