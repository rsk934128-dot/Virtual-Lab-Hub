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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtuallab.app.virtual.LogLevel
import com.virtuallab.app.virtual.VirtualCore

data class AptCommandGuide(
  val command: String,
  val description: String,
  val removesPackages: Boolean
)

val APT_GUIDES = listOf(
  AptCommandGuide(
    command = "apt update",
    description = "Refreshes the package index and retrieves latest versions from repositories. Does not install or upgrade.",
    removesPackages = false
  ),
  AptCommandGuide(
    command = "apt upgrade",
    description = "Upgrades installed packages and resolves new dependencies. Will NOT remove existing packages.",
    removesPackages = false
  ),
  AptCommandGuide(
    command = "apt full-upgrade",
    description = "Completes deep upgrades and resolves complex dependencies, allowing package removals if necessary.",
    removesPackages = true
  )
)

@Composable
fun LinuxPackageWorkbench(
  modifier: Modifier = Modifier
) {
  var selectedCommand by remember { mutableStateOf("apt update") }
  var terminalOutput by remember { mutableStateOf("Ready to simulate package manager operations.") }

  Card(
    modifier = modifier.testTag("linux_package_workbench_card"),
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
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Code,
            contentDescription = "Package Manager",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "Linux Environment Package Workbench",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "APT Package management & maintenance simulator",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Quick Guide Chips
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        APT_GUIDES.forEach { guide ->
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (selectedCommand == guide.command) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            onClick = {
              selectedCommand = guide.command
              terminalOutput = "Selected ${guide.command}:\n${guide.description}\nAllowed to remove packages: ${if (guide.removesPackages) "YES" else "NO"}"
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = guide.command,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = if (selectedCommand == guide.command) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = guide.description,
                  style = MaterialTheme.typography.bodySmall,
                  fontSize = 11.sp,
                  color = if (selectedCommand == guide.command) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline
                )
              }
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (guide.removesPackages) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
              ) {
                Text(
                  text = if (guide.removesPackages) "Can Remove" else "Safe",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  color = if (guide.removesPackages) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Command Execution Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = selectedCommand,
          onValueChange = { selectedCommand = it },
          label = { Text("Command") },
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
          modifier = Modifier
            .weight(1f)
            .testTag("package_command_input")
        )

        Button(
          onClick = {
            val cmd = selectedCommand.trim()
            val result = when {
              cmd == "apt update" || cmd == "sudo apt update" -> {
                VirtualCore.postLog(LogLevel.INFO, "APT", "Package index update requested", "Hit 20 repositories")
                """$ sudo apt update
Hit:1 http://archive.ubuntu.com/ubuntu jammy InRelease
Get:2 http://security.ubuntu.com/ubuntu jammy-security InRelease [110 kB]
Fetched 110 kB in 1s (98.2 kB/s)
Reading package lists... Done
Building dependency tree... Done
12 packages can be upgraded. Run 'apt list --upgradable' to see them."""
              }
              cmd == "apt upgrade" || cmd == "sudo apt upgrade" -> {
                VirtualCore.postLog(LogLevel.INFO, "APT", "Package upgrade executed", "12 packages upgraded")
                """$ sudo apt upgrade
Reading package lists... Done
Building dependency tree... Done
Calculating upgrade... Done
The following packages will be upgraded:
  curl libcurl4 git openssl systemd
12 upgraded, 0 newly installed, 0 to remove and 0 not upgraded.
Need to get 14.8 MB of archives.
Processing triggers... Done."""
              }
              cmd == "apt full-upgrade" || cmd == "sudo apt full-upgrade" -> {
                VirtualCore.postLog(LogLevel.WARN, "APT", "Full upgrade with dependency resolution executed", "0 removed, all upgraded")
                """$ sudo apt full-upgrade
Reading package lists... Done
Building dependency tree... Done
Calculating upgrade... Done
0 upgraded, 0 newly installed, 0 to remove.
System is fully updated and dependencies resolved."""
              }
              else -> {
                VirtualCore.postLog(LogLevel.DEBUG, "Console", "Custom command executed: $cmd")
                "$ $cmd\nCommand executed successfully in virtual container space."
              }
            }
            terminalOutput = result
          },
          modifier = Modifier.testTag("execute_command_button"),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = "Run")
          Spacer(modifier = Modifier.width(4.dp))
          Text("Run")
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Terminal Output Box
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(130.dp)
          .clip(RoundedCornerShape(8.dp)),
        color = Color(0xFF181820)
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text(
            text = terminalOutput,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color(0xFF81C784),
            lineHeight = 15.sp
          )
        }
      }
    }
  }
}
