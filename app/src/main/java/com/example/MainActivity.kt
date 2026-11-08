package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.virtuallab.app.data.repository.LabRepository
import com.virtuallab.app.ui.auth.AuthGateScreen
import com.virtuallab.app.ui.auth.attemptAutoSignIn
import com.virtuallab.app.ui.dashboard.LabDashboardScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    com.virtuallab.app.ai.configureAppCheck(this, intent)
    com.virtuallab.app.virtual.VirtualCore.init(applicationContext)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        AppRoot()
      }
    }
  }
}

@Composable
fun AppRoot() {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

  val repository = remember { LabRepository(context.applicationContext) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
      if (auth.currentUser != null) {
        coroutineScope.launch {
          repository.ensureUserProfile(
            displayName = auth.currentUser?.displayName ?: "Developer",
            email = auth.currentUser?.email
          )
        }
      }
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  LaunchedEffect(Unit) {
    if (currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {
          currentUser = Firebase.auth.currentUser
        },
        onUnauthenticated = {
          // Stay on interactive AuthGateScreen
        },
        scope = coroutineScope
      )
    }
  }

  var showSettings by remember { mutableStateOf(false) }
  var showStatusScreen by remember { mutableStateOf(false) }
  var activeLiveApp by remember { mutableStateOf<com.virtuallab.app.ui.components.ExtractedAppInfo?>(null) }

  val user = currentUser
  if (user == null) {
    AuthGateScreen(
      onAuthSuccess = {
        currentUser = Firebase.auth.currentUser
      }
    )
  } else if (activeLiveApp != null) {
    com.virtuallab.app.ui.components.LiveAppRunnerScreen(
      appInfo = activeLiveApp!!,
      onClose = { activeLiveApp = null }
    )
  } else if (showStatusScreen) {
    com.virtuallab.app.ui.status.VirtualEnvironmentStatusScreen(
      onNavigateBack = { showStatusScreen = false },
      onLaunchAiTroubleshoot = { _, _ ->
        showStatusScreen = false
      }
    )
  } else if (showSettings) {
    com.virtuallab.app.ui.settings.VirtualSettingsScreen(
      onNavigateBack = { showSettings = false }
    )
  } else {
    LabDashboardScreen(
      currentUser = user,
      repository = repository,
      onNavigateToSettings = { showSettings = true },
      onNavigateToStatus = { showStatusScreen = true },
      onLaunchLiveApp = { appInfo -> activeLiveApp = appInfo }
    )
  }
}
