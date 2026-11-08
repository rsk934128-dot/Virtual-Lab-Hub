package com.virtuallab.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseUser
import com.virtuallab.app.data.model.LabSession
import com.virtuallab.app.data.repository.LabRepository
import com.virtuallab.app.ui.auth.signOutUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface SessionsUiState {
  object Loading : SessionsUiState
  data class Success(val sessions: List<LabSession>) : SessionsUiState
  data class Error(val message: String) : SessionsUiState
}

class DashboardStateHolder(
  private val repository: LabRepository,
  scope: kotlinx.coroutines.CoroutineScope
) {
  val sessionsState: StateFlow<SessionsUiState> = repository.observeSessions()
    .map<List<LabSession>, SessionsUiState> { SessionsUiState.Success(it) }
    .catch { error ->
      emit(SessionsUiState.Error(error.localizedMessage ?: "Failed to load lab sessions"))
    }
    .stateIn(
      scope = scope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = SessionsUiState.Loading
    )

  fun createSession(
    title: String,
    description: String?,
    runtimeType: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onComplete: (Boolean) -> Unit
  ) {
    scope.launch {
      val sessionId = "session_${UUID.randomUUID().toString().take(8)}"
      val result = repository.createSession(sessionId, title, description, runtimeType)
      onComplete(result.isSuccess)
    }
  }

  fun toggleSessionStatus(session: LabSession, scope: kotlinx.coroutines.CoroutineScope) {
    scope.launch {
      val nextStatus = if (session.status == "active") "paused" else "active"
      repository.updateSessionStatus(session.id, nextStatus)
    }
  }

  fun deleteSession(sessionId: String, scope: kotlinx.coroutines.CoroutineScope) {
    scope.launch {
      repository.deleteSession(sessionId)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabDashboardScreen(
  currentUser: FirebaseUser,
  repository: LabRepository,
  onNavigateToSettings: () -> Unit = {},
  onLaunchLiveApp: (com.virtuallab.app.ui.components.ExtractedAppInfo) -> Unit = {}
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  val stateHolder = remember(repository) { DashboardStateHolder(repository, coroutineScope) }
  val sessionsState by stateHolder.sessionsState.collectAsState()

  var showCreateDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Virtual Lab Hub",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = currentUser.email ?: "Firebase Cloud Sandbox",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
          }
        },
        actions = {
          IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.testTag("open_settings_button")
          ) {
            Icon(
              imageVector = androidx.compose.material.icons.Icons.Default.Settings,
              contentDescription = "Settings"
            )
          }

          IconButton(
            onClick = {
              signOutUser(
                context = context,
                credentialManager = credentialManager,
                onSignOutComplete = {},
                scope = coroutineScope
              )
            },
            modifier = Modifier.testTag("sign_out_button")
          ) {
            Icon(
              imageVector = Icons.Default.ExitToApp,
              contentDescription = "Sign Out"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showCreateDialog = true },
        modifier = Modifier.testTag("create_session_fab"),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Lab Session")
      }
    }
  ) { paddingValues ->
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      color = MaterialTheme.colorScheme.background
    ) {
      when (val state = sessionsState) {
        is SessionsUiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        is SessionsUiState.Error -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = state.message,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }
        is SessionsUiState.Success -> {
          if (state.sessions.isEmpty()) {
            EmptyLabState(onCreateClick = { showCreateDialog = true })
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
              contentPadding = PaddingValues(vertical = 16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              item {
                StatusBanner(sessionCount = state.sessions.size)
              }
              item {
                com.virtuallab.app.ui.components.ZipAppLauncherCard(
                  onLaunchLiveApp = onLaunchLiveApp,
                  modifier = Modifier.fillMaxWidth()
                )
              }
              item {
                val fileExplorerService = remember { com.virtuallab.app.virtual.FileExplorerService(context) }
                com.virtuallab.app.ui.components.FileExplorerCard(
                  fileExplorerService = fileExplorerService,
                  onOpenLab = onLaunchLiveApp,
                  modifier = Modifier.fillMaxWidth()
                )
              }
              item {
                com.virtuallab.app.ui.components.VirtualLabAiAssistantWorkbench(
                  modifier = Modifier.fillMaxWidth()
                )
              }
              item {
                com.virtuallab.app.ui.components.AiNativeTerminalWorkbench(
                  modifier = Modifier.fillMaxWidth()
                )
              }
              item {
                com.virtuallab.app.ui.components.AiCodeReviewWorkbench(
                  modifier = Modifier.fillMaxWidth()
                )
              }
              item {
                com.virtuallab.app.ui.components.VirtualEnvironmentManager(
                  modifier = Modifier.fillMaxWidth()
                )
              }
              item {
                com.virtuallab.app.ui.components.VirtualResourceMonitor(
                  modifier = Modifier.fillMaxWidth()
                )
              }
              item {
                com.virtuallab.app.ui.components.PwaBuilderWorkbench(
                  modifier = Modifier.fillMaxWidth()
                )
              }
              item {
                com.virtuallab.app.ui.components.VercelHostingWorkbench(
                  modifier = Modifier.fillMaxWidth()
                )
              }
              item {
                com.virtuallab.app.ui.components.LinuxPackageWorkbench(
                  modifier = Modifier.fillMaxWidth()
                )
              }
              item {
                com.virtuallab.app.ui.components.VirtualLogViewer(
                  modifier = Modifier.fillMaxWidth()
                )
              }
              items(state.sessions, key = { it.id }) { session ->
                SessionCard(
                  session = session,
                  onToggleStatus = { stateHolder.toggleSessionStatus(session, coroutineScope) },
                  onDelete = { stateHolder.deleteSession(session.id, coroutineScope) }
                )
              }
            }
          }
        }
      }
    }
  }

  if (showCreateDialog) {
    CreateSessionDialog(
      onDismiss = { showCreateDialog = false },
      onCreate = { title, desc, runtime ->
        stateHolder.createSession(title, desc, runtime, coroutineScope) {
          showCreateDialog = false
        }
      }
    )
  }
}

@Composable
fun StatusBanner(sessionCount: Int) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.secondary),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.DeveloperBoard,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondary,
          modifier = Modifier.size(24.dp)
        )
      }
      Spacer(modifier = Modifier.width(16.dp))
      Column {
        Text(
          text = "Active Cloud Sandbox Pods",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
          text = "$sessionCount isolated environment instance(s) running",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
        )
      }
    }
  }
}

@Composable
fun SessionCard(
  session: LabSession,
  onToggleStatus: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("session_card_${session.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(
                if (session.status == "active") MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
              )
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = session.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (session.status == "active") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ) {
          Text(
            text = session.status.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = if (session.status == "active") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
          )
        }
      }

      if (!session.description.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = session.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Runtime: ${session.runtimeType}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline
        )

        Row {
          IconButton(
            onClick = onToggleStatus,
            modifier = Modifier.testTag("toggle_status_${session.id}")
          ) {
            Icon(
              imageVector = if (session.status == "active") Icons.Default.Stop else Icons.Default.PlayArrow,
              contentDescription = if (session.status == "active") "Pause" else "Start",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag("delete_session_${session.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete Session",
              tint = MaterialTheme.colorScheme.error
            )
          }
        }
      }
    }
  }
}

@Composable
fun EmptyLabState(onCreateClick: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.DeveloperBoard,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(44.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "No Lab Sessions Yet",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Create your first isolated container sandbox to start testing and syncing configs.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = onCreateClick,
        modifier = Modifier.testTag("empty_state_create_button"),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create Session")
      }
    }
  }
}

@Composable
fun CreateSessionDialog(
  onDismiss: () -> Unit,
  onCreate: (title: String, description: String?, runtimeType: String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var runtimeType by remember { mutableStateOf("android-container") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "New Lab Sandbox",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Session Name") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("session_name_input")
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description (Optional)") },
          maxLines = 3,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("session_desc_input")
        )

        OutlinedTextField(
          value = runtimeType,
          onValueChange = { runtimeType = it },
          label = { Text("Runtime Environment") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("runtime_type_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onCreate(title.trim(), description.trim().ifBlank { null }, runtimeType.trim().ifBlank { "android-container" })
          }
        },
        enabled = title.isNotBlank(),
        modifier = Modifier.testTag("dialog_create_confirm")
      ) {
        Text("Create")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
