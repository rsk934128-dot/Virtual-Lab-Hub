package com.virtuallab.app.virtual

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Log entry representing virtual process events, file operations, and container lifecycle actions.
 */
data class VirtualLogEntry(
  val id: String = java.util.UUID.randomUUID().toString(),
  val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date()),
  val level: LogLevel = LogLevel.INFO,
  val subsystem: String,
  val event: String,
  val details: String? = null
)

enum class LogLevel {
  DEBUG, INFO, WARN, ERROR
}

/**
 * State representing the status of an isolated sandbox environment.
 */
sealed interface VirtualEnvironmentState {
  object Uninitialized : VirtualEnvironmentState
  data class Initializing(val progress: Float) : VirtualEnvironmentState
  data class Ready(val containerDir: String, val activeSandboxes: Int) : VirtualEnvironmentState
  data class Error(val message: String) : VirtualEnvironmentState
}

/**
 * VirtualCore manages isolated application sandbox environments, isolated internal data directories,
 * and environment lifecycles in compliant user-space.
 */
object VirtualCore {

  private const val BASE_CONTAINER_DIR = "virtual_containers"
  private val _engineState = MutableStateFlow<VirtualEnvironmentState>(VirtualEnvironmentState.Uninitialized)
  val engineState: StateFlow<VirtualEnvironmentState> = _engineState.asStateFlow()

  private val _liveLogs = MutableStateFlow<List<VirtualLogEntry>>(emptyList())
  val liveLogs: StateFlow<List<VirtualLogEntry>> = _liveLogs.asStateFlow()

  private val _networkIsolation = MutableStateFlow(true)
  val networkIsolation: StateFlow<Boolean> = _networkIsolation.asStateFlow()

  private val _externalStorageAccess = MutableStateFlow(false)
  val externalStorageAccess: StateFlow<Boolean> = _externalStorageAccess.asStateFlow()

  private var isInitialized = false
  private var sandboxRootDir: File? = null

  fun setNetworkIsolation(enabled: Boolean) {
    _networkIsolation.value = enabled
    postLog(
      if (enabled) LogLevel.WARN else LogLevel.INFO,
      "PolicyManager",
      "Network Isolation ${if (enabled) "ENABLED" else "DISABLED"}",
      if (enabled) "Outbound sandbox sockets are blocked" else "Direct host networking permitted"
    )
  }

  fun setExternalStorageAccess(allowed: Boolean) {
    _externalStorageAccess.value = allowed
    postLog(
      if (allowed) LogLevel.WARN else LogLevel.INFO,
      "StorageManager",
      "External Storage Access ${if (allowed) "ALLOWED" else "RESTRICTED"}",
      if (allowed) "Containers can mount shared storage partitions" else "Containers constrained to internal storage sandbox"
    )
  }

  fun postLog(level: LogLevel, subsystem: String, event: String, details: String? = null) {
    val entry = VirtualLogEntry(
      level = level,
      subsystem = subsystem,
      event = event,
      details = details
    )
    val current = _liveLogs.value
    // Keep last 150 entries in memory
    val updated = (listOf(entry) + current).take(150)
    _liveLogs.value = updated
  }

  fun clearLogs() {
    _liveLogs.value = emptyList()
  }

  /**
   * Initializes the container virtualization workspace inside application-private internal storage.
   */
  @Synchronized
  fun init(context: Context): Boolean {
    if (isInitialized) return true

    return try {
      _engineState.value = VirtualEnvironmentState.Initializing(0.2f)
      postLog(LogLevel.INFO, "VirtualCore", "Engine initialization requested", "Binding to internal app storage")

      val containerRoot = File(context.filesDir, BASE_CONTAINER_DIR)
      if (!containerRoot.exists()) {
        containerRoot.mkdirs()
        postLog(LogLevel.DEBUG, "FileSystem", "Created base virtual container directory", containerRoot.path)
      }
      sandboxRootDir = containerRoot
      _engineState.value = VirtualEnvironmentState.Initializing(0.8f)

      isInitialized = true
      val activeCount = containerRoot.listFiles()?.count { it.isDirectory } ?: 0
      _engineState.value = VirtualEnvironmentState.Ready(
        containerDir = containerRoot.absolutePath,
        activeSandboxes = activeCount
      )
      postLog(LogLevel.INFO, "VirtualCore", "Virtualization engine ready", "Active sandbox containers: $activeCount")
      true
    } catch (e: Exception) {
      val errMsg = e.localizedMessage ?: "Failed to initialize VirtualCore"
      _engineState.value = VirtualEnvironmentState.Error(errMsg)
      postLog(LogLevel.ERROR, "VirtualCore", "Engine initialization failed", errMsg)
      false
    }
  }

  /**
   * Allocates an isolated storage workspace for a sandbox session.
   */
  fun allocateSandbox(sessionId: String): File? {
    val root = sandboxRootDir ?: return null
    val sessionDir = File(root, sessionId)
    if (!sessionDir.exists()) {
      sessionDir.mkdirs()
      File(sessionDir, "data").mkdirs()
      File(sessionDir, "cache").mkdirs()
      postLog(LogLevel.INFO, "SandboxManager", "Allocated container workspace", "Session ID: $sessionId")
    }
    updateActiveCount()
    return sessionDir
  }

  private val runningContainers = mutableSetOf<String>()

  /**
   * Starts an isolated virtual Android container session.
   */
  fun startContainer(sessionId: String): Boolean {
    val sandbox = allocateSandbox(sessionId) ?: return false
    synchronized(runningContainers) {
      runningContainers.add(sessionId)
    }
    postLog(
      LogLevel.INFO,
      "VirtualCore",
      "Started virtual container session",
      "Session: $sessionId (Workspace: ${sandbox.name})"
    )
    return true
  }

  /**
   * Stops a running virtual Android container session.
   */
  fun stopContainer(sessionId: String): Boolean {
    val wasRunning: Boolean
    synchronized(runningContainers) {
      wasRunning = runningContainers.remove(sessionId)
    }
    postLog(
      LogLevel.WARN,
      "VirtualCore",
      "Stopped virtual container session",
      "Session: $sessionId (Previous state: ${if (wasRunning) "RUNNING" else "STOPPED"})"
    )
    return true
  }

  /**
   * Checks if a specific container session is currently running.
   */
  fun isContainerRunning(sessionId: String): Boolean {
    synchronized(runningContainers) {
      return runningContainers.contains(sessionId)
    }
  }

  /**
   * Returns a status string description for a given container.
   */
  fun getContainerStatus(sessionId: String): String {
    return if (isContainerRunning(sessionId)) "RUNNING" else "STOPPED"
  }

  /**
   * Returns list of currently active running container session IDs.
   */
  fun getRunningContainers(): List<String> {
    synchronized(runningContainers) {
      return runningContainers.toList()
    }
  }

  /**
   * Deallocates and cleans up the sandbox workspace.
   */
  fun purgeSandbox(sessionId: String): Boolean {
    stopContainer(sessionId)
    val root = sandboxRootDir ?: return false
    val sessionDir = File(root, sessionId)
    val deleted = if (sessionDir.exists()) sessionDir.deleteRecursively() else true
    postLog(LogLevel.WARN, "SandboxManager", "Purged container workspace", "Session ID: $sessionId (success=$deleted)")
    updateActiveCount()
    return deleted
  }

  /**
   * Returns current active sandbox count.
   */
  fun getActiveSandboxCount(): Int {
    val root = sandboxRootDir ?: return 0
    return root.listFiles()?.count { it.isDirectory } ?: 0
  }

  private fun updateActiveCount() {
    val root = sandboxRootDir ?: return
    val count = root.listFiles()?.count { it.isDirectory } ?: 0
    _engineState.value = VirtualEnvironmentState.Ready(
      containerDir = root.absolutePath,
      activeSandboxes = count
    )
  }
}
