package com.virtuallab.app.virtual

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Virtual Android/Linux sandbox guest operating system profile definition
 */
data class VirtualSystemProfile(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val osType: String, // "Android 14 (AOSP)", "Android 12 (Lite)", "Alpine Linux Sandbox", "Ubuntu Server Minimal"
  val architecture: String, // "arm64-v8a", "x86_64"
  val virtualRamMb: Int, // 1024, 2048, 4096
  val virtualStorageGb: Int, // 8, 16, 32
  val displayResolution: String, // "1080x2400 (FHD+)", "720x1600 (HD+)", "Headless Console"
  val isRunning: Boolean = false,
  val uptimeSeconds: Long = 0L,
  val installedApps: List<VirtualPackageItem> = emptyList()
)

/**
 * Sandboxed virtual package or testing application installed inside the container
 */
data class VirtualPackageItem(
  val packageName: String,
  val appName: String,
  val version: String,
  val category: String, // "Developer Tool", "Network Utility", "System Diagnostic", "Testing Framework"
  val isEnabled: Boolean = true
)

object VirtualSystemCatalog {

  val PRESET_PROFILES = listOf(
    VirtualSystemProfile(
      id = "profile_android_14",
      name = "Android 14 AOSP Sandbox",
      osType = "Android 14 (AOSP User-space)",
      architecture = "arm64-v8a",
      virtualRamMb = 2048,
      virtualStorageGb = 16,
      displayResolution = "1080x2400 (FHD+)",
      installedApps = listOf(
        VirtualPackageItem("com.virtuallab.term", "Virtual Terminal", "1.4.2", "Developer Tool"),
        VirtualPackageItem("com.virtuallab.netdiag", "Network Analyzer", "2.1.0", "Network Utility"),
        VirtualPackageItem("com.virtuallab.storage", "Disk I/O Benchmark", "1.0.5", "System Diagnostic")
      )
    ),
    VirtualSystemProfile(
      id = "profile_android_12_lite",
      name = "Android 12 Lite VM",
      osType = "Android 12 (Lite Container)",
      architecture = "arm64-v8a",
      virtualRamMb = 1024,
      virtualStorageGb = 8,
      displayResolution = "720x1600 (HD+)",
      installedApps = listOf(
        VirtualPackageItem("com.virtuallab.term", "Virtual Terminal", "1.4.2", "Developer Tool"),
        VirtualPackageItem("com.virtuallab.sysinfo", "Hardware Inspector", "3.0.1", "System Diagnostic")
      )
    ),
    VirtualSystemProfile(
      id = "profile_alpine_sandbox",
      name = "Alpine Linux Minimal",
      osType = "Alpine Linux v3.19",
      architecture = "arm64-v8a",
      virtualRamMb = 1024,
      virtualStorageGb = 8,
      displayResolution = "Headless Console",
      installedApps = listOf(
        VirtualPackageItem("org.busybox", "Busybox Coreutils", "1.36.1", "Developer Tool"),
        VirtualPackageItem("net.curl", "cURL Network Utility", "8.5.0", "Network Utility")
      )
    )
  )

  private val _activeProfiles = MutableStateFlow<List<VirtualSystemProfile>>(PRESET_PROFILES)
  val activeProfiles: StateFlow<List<VirtualSystemProfile>> = _activeProfiles.asStateFlow()

  fun toggleProfileRunState(profileId: String) {
    val updated = _activeProfiles.value.map { profile ->
      if (profile.id == profileId) {
        val willRun = !profile.isRunning
        VirtualCore.postLog(
          if (willRun) LogLevel.INFO else LogLevel.WARN,
          "ContainerVM",
          if (willRun) "Virtual system instance started" else "Virtual system instance suspended",
          "${profile.name} (${profile.osType}) - RAM: ${profile.virtualRamMb}MB"
        )
        profile.copy(isRunning = willRun)
      } else profile
    }
    _activeProfiles.value = updated
  }

  fun installPackageToProfile(profileId: String, newPkg: VirtualPackageItem) {
    val updated = _activeProfiles.value.map { profile ->
      if (profile.id == profileId) {
        val newAppList = profile.installedApps + newPkg
        VirtualCore.postLog(
          LogLevel.INFO,
          "PackageManager",
          "Installed app into virtual environment",
          "${newPkg.appName} (${newPkg.packageName}) -> ${profile.name}"
        )
        profile.copy(installedApps = newAppList)
      } else profile
    }
    _activeProfiles.value = updated
  }

  fun uninstallPackageFromProfile(profileId: String, packageName: String) {
    val updated = _activeProfiles.value.map { profile ->
      if (profile.id == profileId) {
        val filtered = profile.installedApps.filter { it.packageName != packageName }
        VirtualCore.postLog(
          LogLevel.WARN,
          "PackageManager",
          "Uninstalled app from virtual container",
          "Removed: $packageName from ${profile.name}"
        )
        profile.copy(installedApps = filtered)
      } else profile
    }
    _activeProfiles.value = updated
  }
}
