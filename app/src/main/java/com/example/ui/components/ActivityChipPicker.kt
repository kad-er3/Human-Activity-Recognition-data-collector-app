package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.CustomActionEntity
import com.example.data.model.ActivityPreset

@Composable
fun ActivityChipPicker(
  selectedAction: String,
  actionsList: List<ActivityPreset>,
  deletedActionsList: List<String> = emptyList(),
  onActionSelected: (String) -> Unit,
  onAddNewAction: (label: String, emoji: String) -> Unit,
  onDeleteAction: (String) -> Unit,
  onRestoreAction: (String) -> Unit = {},
  customActionEntities: List<CustomActionEntity> = emptyList(),
  modifier: Modifier = Modifier
) {
  var showCustomDialog by remember { mutableStateOf(false) }
  var showManageActionsDialog by remember { mutableStateOf(false) }
  var actionToDelete by remember { mutableStateOf<String?>(null) }
  var customActionInput by remember { mutableStateOf("") }
  var customEmojiInput by remember { mutableStateOf("⚡") }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "ACTIONS / ACTIVITÉS (${actionsList.size})",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(
          onClick = { showManageActionsDialog = true },
          modifier = Modifier.testTag("btn_manage_actions")
        ) {
          Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp).padding(end = 2.dp))
          Text("Gérer / Supprimer", fontSize = 12.sp)
        }
        TextButton(
          onClick = { showCustomDialog = true },
          modifier = Modifier.testTag("btn_custom_action")
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 2.dp))
          Text("Ajouter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Horizontal scrollable list of actions
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // "+ Add Action" quick chip at the start
      SuggestionChip(
        onClick = { showCustomDialog = true },
        label = { Text("+ Ajouter", fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("chip_add_action_quick")
      )

      actionsList.forEach { preset ->
        val isSelected = selectedAction.equals(preset.label, ignoreCase = true)

        InputChip(
          selected = isSelected,
          onClick = { onActionSelected(preset.label) },
          label = {
            Text("${preset.emoji} ${preset.label}", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
          },
          trailingIcon = {
            IconButton(
              onClick = { actionToDelete = preset.label },
              modifier = Modifier.size(18.dp).testTag("delete_chip_action_${preset.label}")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Supprimer l'action",
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
              )
            }
          },
          colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier.testTag("action_chip_${preset.id}")
        )
      }
    }
  }

  // Confirm delete single action dialog
  actionToDelete?.let { actionLabel ->
    AlertDialog(
      onDismissRequest = { actionToDelete = null },
      icon = {
        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
      },
      title = { Text("Supprimer cette action ?") },
      text = {
        Text("Voulez-vous retirer '$actionLabel' de la liste des actions ? Vous pourrez la restaurer à tout moment via le menu Gérer.")
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteAction(actionLabel)
            actionToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("btn_confirm_delete_action")
        ) {
          Text("Supprimer")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { actionToDelete = null }) {
          Text("Annuler")
        }
      }
    )
  }

  // Manage Actions Dialog (list all actions, delete buttons, restore buttons)
  if (showManageActionsDialog) {
    AlertDialog(
      onDismissRequest = { showManageActionsDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Gérer les actions")
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Supprimez les actions inutiles ou restaurez des actions masquées :",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(260.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            item {
              Text("Actions actives :", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
            items(actionsList) { act ->
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("${act.emoji} ${act.label}", style = MaterialTheme.typography.bodyMedium)
                  IconButton(
                    onClick = { onDeleteAction(act.label) },
                    modifier = Modifier.size(28.dp).testTag("btn_delete_action_item_${act.label}")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "Supprimer",
                      tint = MaterialTheme.colorScheme.error,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }

            if (deletedActionsList.isNotEmpty()) {
              item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))
                Text("Actions supprimées / masquées :", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
              }
              items(deletedActionsList) { deletedAct ->
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                  color = MaterialTheme.colorScheme.surface
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(deletedAct, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(
                      onClick = { onRestoreAction(deletedAct) },
                      modifier = Modifier.testTag("btn_restore_action_$deletedAct")
                    ) {
                      Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Restaurer", fontSize = 11.sp)
                    }
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = { showManageActionsDialog = false }) {
          Text("Terminé")
        }
      }
    )
  }

  // Custom action dialog
  if (showCustomDialog) {
    val quickActionSuggestions = listOf("cycling", "pushups", "boxing", "dancing", "stretching", "brushing teeth")
    AlertDialog(
      onDismissRequest = { showCustomDialog = false },
      title = { Text("Ajouter une action") },
      text = {
        Column {
          Text(
            text = "Spécifiez une nouvelle action ou posture pour vos enregistrements ML.",
            style = MaterialTheme.typography.bodyMedium
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = customEmojiInput,
              onValueChange = { customEmojiInput = it.take(2) },
              label = { Text("Icône") },
              singleLine = true,
              modifier = Modifier
                .width(70.dp)
                .testTag("input_custom_emoji")
            )
            OutlinedTextField(
              value = customActionInput,
              onValueChange = { customActionInput = it },
              label = { Text("Nom de l'action") },
              placeholder = { Text("ex. cycling") },
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("input_custom_action")
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text("Suggestions rapides :", style = MaterialTheme.typography.labelSmall)
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            quickActionSuggestions.forEach { suggestion ->
              SuggestionChip(
                onClick = { customActionInput = suggestion },
                label = { Text(suggestion, fontSize = 11.sp) }
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (customActionInput.isNotBlank()) {
              onAddNewAction(customActionInput.trim(), customEmojiInput.trim().ifEmpty { "⚡" })
              customActionInput = ""
              showCustomDialog = false
            }
          },
          modifier = Modifier.testTag("btn_confirm_custom_action")
        ) {
          Text("Ajouter & Sélectionner")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showCustomDialog = false }) {
          Text("Annuler")
        }
      }
    )
  }
}

@Composable
fun ParticipantUserSelector(
  currentUser: String,
  availableUsers: List<String>,
  deletedUsersList: List<String> = emptyList(),
  onUserSelected: (String) -> Unit,
  onAddNewUser: (String) -> Unit,
  onDeleteUser: (name: String, deleteRecordings: Boolean) -> Unit,
  onRestoreUser: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showAddUserDialog by remember { mutableStateOf(false) }
  var showManageUsersDialog by remember { mutableStateOf(false) }
  var userToDelete by remember { mutableStateOf<String?>(null) }
  var deleteRecordingsChecked by remember { mutableStateOf(false) }
  var newUserNameInput by remember { mutableStateOf("") }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "UTILISATEUR / PARTICIPANT (${availableUsers.size})",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(
          onClick = { showManageUsersDialog = true },
          modifier = Modifier.testTag("btn_manage_users")
        ) {
          Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp).padding(end = 2.dp))
          Text("Gérer / Supprimer", fontSize = 12.sp)
        }
        TextButton(
          onClick = {
            newUserNameInput = ""
            showAddUserDialog = true
          },
          modifier = Modifier.testTag("btn_add_user_dialog")
        ) {
          Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 2.dp))
          Text("Ajouter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // "+ Add User" quick chip
      SuggestionChip(
        onClick = {
          newUserNameInput = ""
          showAddUserDialog = true
        },
        label = { Text("+ Ajouter", fontWeight = FontWeight.Bold) },
        icon = {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
        },
        modifier = Modifier.testTag("chip_add_user_quick")
      )

      // All dynamic & existing users
      availableUsers.forEach { user ->
        val isSelected = currentUser.equals(user, ignoreCase = true)

        InputChip(
          selected = isSelected,
          onClick = { onUserSelected(user) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
          },
          label = {
            Text(
              text = user,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          },
          trailingIcon = {
            IconButton(
              onClick = {
                userToDelete = user
                deleteRecordingsChecked = false
              },
              modifier = Modifier.size(18.dp).testTag("delete_chip_user_$user")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Supprimer l'utilisateur",
                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
              )
            }
          },
          colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
          ),
          modifier = Modifier.testTag("chip_user_$user")
        )
      }
    }
  }

  // Confirm delete single user dialog
  userToDelete?.let { userName ->
    AlertDialog(
      onDismissRequest = { userToDelete = null },
      icon = {
        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
      },
      title = { Text("Supprimer l'utilisateur ?") },
      text = {
        Column {
          Text("Voulez-vous supprimer l'utilisateur '$userName' de la liste des participants ?")
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Checkbox(
              checked = deleteRecordingsChecked,
              onCheckedChange = { deleteRecordingsChecked = it }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Supprimer également ses fichiers CSV enregistrés", fontSize = 12.sp)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteUser(userName, deleteRecordingsChecked)
            userToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("btn_confirm_delete_user")
        ) {
          Text("Supprimer")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { userToDelete = null }) {
          Text("Annuler")
        }
      }
    )
  }

  // Manage Users Dialog (list all users, delete buttons, restore buttons)
  if (showManageUsersDialog) {
    AlertDialog(
      onDismissRequest = { showManageUsersDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Gérer les utilisateurs")
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Supprimez ou restaurez des profils participants :",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(260.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            item {
              Text("Utilisateurs actifs :", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
            items(availableUsers) { u ->
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(u, style = MaterialTheme.typography.bodyMedium)
                  }
                  IconButton(
                    onClick = {
                      userToDelete = u
                      deleteRecordingsChecked = false
                    },
                    modifier = Modifier.size(28.dp).testTag("btn_delete_user_item_$u")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "Supprimer",
                      tint = MaterialTheme.colorScheme.error,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }

            if (deletedUsersList.isNotEmpty()) {
              item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))
                Text("Utilisateurs supprimés / masqués :", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
              }
              items(deletedUsersList) { deletedU ->
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                  color = MaterialTheme.colorScheme.surface
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(deletedU, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(
                      onClick = { onRestoreUser(deletedU) },
                      modifier = Modifier.testTag("btn_restore_user_$deletedU")
                    ) {
                      Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Restaurer", fontSize = 11.sp)
                    }
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = { showManageUsersDialog = false }) {
          Text("Terminé")
        }
      }
    )
  }

  // Dynamic Add User Dialog
  if (showAddUserDialog) {
    val quickUserSuggestions = listOf(
      "User_03", "Subject_B", "Subject_C", "Participant_2", "Tester_1", "Lab_Subject_01"
    ).filter { suggestion -> availableUsers.none { it.equals(suggestion, ignoreCase = true) } }

    AlertDialog(
      onDismissRequest = { showAddUserDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Ajouter un participant")
        }
      },
      text = {
        Column {
          Text(
            text = "Créez un nouveau profil participant. Les enregistrements de capteurs seront étiquetés avec ce nom.",
            style = MaterialTheme.typography.bodyMedium
          )
          Spacer(modifier = Modifier.height(14.dp))
          OutlinedTextField(
            value = newUserNameInput,
            onValueChange = { newUserNameInput = it },
            label = { Text("Nom ou ID participant") },
            placeholder = { Text("ex. Subject_03") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_new_user_name")
          )

          if (quickUserSuggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Suggestions rapides :", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              quickUserSuggestions.forEach { suggestion ->
                SuggestionChip(
                  onClick = { newUserNameInput = suggestion },
                  label = { Text(suggestion, fontSize = 11.sp) }
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newUserNameInput.isNotBlank()) {
              onAddNewUser(newUserNameInput.trim())
              showAddUserDialog = false
            }
          },
          modifier = Modifier.testTag("btn_confirm_add_user")
        ) {
          Text("Ajouter & Sélectionner")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showAddUserDialog = false }) {
          Text("Annuler")
        }
      }
    )
  }
}
