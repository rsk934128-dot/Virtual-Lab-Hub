package com.virtuallab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtuallab.app.virtual.VirtualPackageItem
import com.virtuallab.app.virtual.VirtualSystemCatalog
import com.virtuallab.app.virtual.VirtualSystemProfile

@Composable
fun VirtualEnvironmentManager(
  modifier: Modifier = Modifier
) {
  val profiles by VirtualSystemCatalog.activeProfiles.collectAsState()
  var profileForAppInstall by remember { mutableStateOf<VirtualSystemProfile?>(null) }

  Card(
    modifier = modifier.testTag("virtual_environment_manager_card"),
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
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Smartphone,
            contentDescription = "Virtual Environment Launcher",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "ভার্চুয়াল অ্যান্ড্রয়েড ও লিনাক্স এনভায়রনমেন্ট",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "User-space Virtual Containers & Testing Sandbox",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        profiles.forEach { profile ->
          VirtualProfileCard(
            profile = profile,
            onToggleRun = { VirtualSystemCatalog.toggleProfileRunState(profile.id) },
            onAddApp = { profileForAppInstall = profile },
            onDeleteApp = { pkg -> VirtualSystemCatalog.uninstallPackageFromProfile(profile.id, pkg) }
          )
        }
      }
    }
  }

  profileForAppInstall?.let { targetProfile ->
    InstallAppModalDialog(
      profile = targetProfile,
      onDismiss = { profileForAppInstall = null },
      onConfirmInstall = { newPkg ->
        VirtualSystemCatalog.installPackageToProfile(targetProfile.id, newPkg)
        profileForAppInstall = null
      }
    )
  }
}

@Composable
fun VirtualProfileCard(
  profile: VirtualSystemProfile,
  onToggleRun: () -> Unit,
  onAddApp: () -> Unit,
  onDeleteApp: (packageName: String) -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("virtual_profile_card_${profile.id}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
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
              .background(if (profile.isRunning) Color(0xFF4CAF50) else Color.Gray)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = profile.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (profile.isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ) {
          Text(
            text = if (profile.isRunning) "RUNNING" else "STANDBY",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = if (profile.isRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "${profile.osType} • Arch: ${profile.architecture} • Screen: ${profile.displayResolution}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Memory,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${profile.virtualRamMb} MB vRAM",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.SdStorage,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color(0xFFFFA000)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${profile.virtualStorageGb} GB Storage",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(10.dp))

      // Installed Apps List
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.secondary
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "ভার্চুয়াল অ্যাপস (${profile.installedApps.size})",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        TextButton(
          onClick = onAddApp,
          modifier = Modifier.testTag("add_app_to_${profile.id}")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("অ্যাপ যুক্ত করুন", fontSize = 12.sp)
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        profile.installedApps.forEach { appItem ->
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Android,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = appItem.appName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    fontSize = 12.sp
                  )
                  Text(
                    text = "${appItem.category} • v${appItem.version}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                  )
                }
              }

              IconButton(
                onClick = { onDeleteApp(appItem.packageName) },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Uninstall App",
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Action Button (Start / Stop Virtual System)
      Button(
        onClick = onToggleRun,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("toggle_run_${profile.id}"),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(
          imageVector = if (profile.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = null,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (profile.isRunning) "এনভায়রনমেন্ট বন্ধ করুন (Suspend)" else "ভার্চুয়াল সিস্টেম চালু করুন (Launch)")
      }
    }
  }
}

@Composable
fun InstallAppModalDialog(
  profile: VirtualSystemProfile,
  onDismiss: () -> Unit,
  onConfirmInstall: (VirtualPackageItem) -> Unit
) {
  var appName by remember { mutableStateOf("") }
  var packageName by remember { mutableStateOf("com.virtual.") }
  var category by remember { mutableStateOf("Developer Tool") }
  var version by remember { mutableStateOf("1.0.0") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "ভার্চুয়াল সিস্টেমে অ্যাপ ইন্সটল",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "লক্ষ্য: ${profile.name} (${profile.osType})",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
          value = appName,
          onValueChange = {
            appName = it
            if (packageName.startsWith("com.virtual.")) {
              val clean = it.lowercase().replace(" ", "")
              packageName = "com.virtual.$clean"
            }
          },
          label = { Text("অ্যাপের নাম (App Name)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = packageName,
          onValueChange = { packageName = it },
          label = { Text("প্যাকেজ আইডি (Package Name)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          label = { Text("ক্যাটাগরি (Category)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = version,
          onValueChange = { version = it },
          label = { Text("ভার্সন (Version)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (appName.isNotBlank() && packageName.isNotBlank()) {
            onConfirmInstall(
              VirtualPackageItem(
                packageName = packageName.trim(),
                appName = appName.trim(),
                version = version.trim().ifBlank { "1.0.0" },
                category = category.trim().ifBlank { "Developer Tool" }
              )
            )
          }
        },
        enabled = appName.isNotBlank() && packageName.isNotBlank()
      ) {
        Text("ইন্সটল করুন")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("বাতিল")
      }
    }
  )
}
