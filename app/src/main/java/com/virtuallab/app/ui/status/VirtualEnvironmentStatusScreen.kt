package com.virtuallab.app.ui.status

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtuallab.app.virtual.LogLevel
import com.virtuallab.app.virtual.VirtualCore
import com.virtuallab.app.virtual.VirtualSystemCatalog
import java.util.UUID

enum class EnvironmentHealthStatus(
  val label: String,
  val color: Color,
  val icon: ImageVector
) {
  RUNNING("Running", Color(0xFF2E7D32), Icons.Default.CheckCircle),
  PAUSED("Paused", Color(0xFFED6C02), Icons.Default.Pause),
  ERROR("Error", Color(0xFFD32F2F), Icons.Default.Error)
}

data class LabEnvironmentItem(
  val id: String,
  val name: String,
  val osType: String,
  val category: String, // "Android Sandbox", "Linux Container", "Web Application"
  val status: EnvironmentHealthStatus,
  val cpuUsagePercent: Int,
  val ramUsageMb: Int,
  val maxRamMb: Int,
  val storageGb: Int,
  val networkIsolated: Boolean,
  val errorDetails: String? = null,
  val uptimeText: String = "00:00:00"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualEnvironmentStatusScreen(
  onNavigateBack: () -> Unit,
  onLaunchAiTroubleshoot: (environmentName: String, errorReason: String?) -> Unit = { _, _ -> }
) {
  BackHandler(onBack = onNavigateBack)

  val profiles by VirtualSystemCatalog.activeProfiles.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf<EnvironmentHealthStatus?>(null) }

  // Synthesize environment items combining live profile states and simulated failure telemetry
  val environments = remember(profiles) {
    listOf(
      LabEnvironmentItem(
        id = "env_aosp_14",
        name = "Android 14 (AOSP) Production Sandbox",
        osType = "Android 14.0 (API 34)",
        category = "Android Sandbox",
        status = if (profiles.find { it.id == "profile_android_14" }?.isRunning == true) EnvironmentHealthStatus.RUNNING else EnvironmentHealthStatus.PAUSED,
        cpuUsagePercent = if (profiles.find { it.id == "profile_android_14" }?.isRunning == true) 34 else 0,
        ramUsageMb = if (profiles.find { it.id == "profile_android_14" }?.isRunning == true) 1240 else 0,
        maxRamMb = 2048,
        storageGb = 16,
        networkIsolated = VirtualCore.networkIsolation.value,
        uptimeText = "03:42:15"
      ),
      LabEnvironmentItem(
        id = "env_android_12_lite",
        name = "Android 12 Lite Testing Container",
        osType = "Android 12.0 (API 31)",
        category = "Android Sandbox",
        status = if (profiles.find { it.id == "profile_android_12_lite" }?.isRunning == true) EnvironmentHealthStatus.RUNNING else EnvironmentHealthStatus.PAUSED,
        cpuUsagePercent = if (profiles.find { it.id == "profile_android_12_lite" }?.isRunning == true) 18 else 0,
        ramUsageMb = if (profiles.find { it.id == "profile_android_12_lite" }?.isRunning == true) 680 else 0,
        maxRamMb = 1024,
        storageGb = 8,
        networkIsolated = VirtualCore.networkIsolation.value,
        uptimeText = "01:15:30"
      ),
      LabEnvironmentItem(
        id = "env_alpine_linux",
        name = "Alpine Linux Minimal Container",
        osType = "Alpine Linux v3.19",
        category = "Linux Container",
        status = if (profiles.find { it.id == "profile_alpine_sandbox" }?.isRunning == true) EnvironmentHealthStatus.RUNNING else EnvironmentHealthStatus.PAUSED,
        cpuUsagePercent = if (profiles.find { it.id == "profile_alpine_sandbox" }?.isRunning == true) 8 else 0,
        ramUsageMb = if (profiles.find { it.id == "profile_alpine_sandbox" }?.isRunning == true) 320 else 0,
        maxRamMb = 1024,
        storageGb = 8,
        networkIsolated = VirtualCore.networkIsolation.value,
        uptimeText = "05:08:44"
      ),
      LabEnvironmentItem(
        id = "env_ubuntu_server_err",
        name = "Ubuntu 22.04 LTS Micro Container",
        osType = "Ubuntu 22.04 (arm64)",
        category = "Linux Container",
        status = EnvironmentHealthStatus.ERROR,
        cpuUsagePercent = 98,
        ramUsageMb = 2048,
        maxRamMb = 2048,
        storageGb = 16,
        networkIsolated = true,
        errorDetails = "OOM Killer: Virtual RAM allocation exceeded limit (2048/2048 MB). Host socket disconnected on port 3000.",
        uptimeText = "CRASHED"
      ),
      LabEnvironmentItem(
        id = "env_vercel_node_runner",
        name = "Node.js Edge Sandbox Runtime",
        osType = "Node.js 20.x Isolated V8",
        category = "Web Application",
        status = EnvironmentHealthStatus.PAUSED,
        cpuUsagePercent = 0,
        ramUsageMb = 0,
        maxRamMb = 1024,
        storageGb = 4,
        networkIsolated = false,
        uptimeText = "00:00:00"
      )
    )
  }

  val filteredEnvironments = environments.filter { item ->
    val matchesFilter = selectedFilter == null || item.status == selectedFilter
    val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) ||
      item.osType.contains(searchQuery, ignoreCase = true) ||
      item.category.contains(searchQuery, ignoreCase = true)
    matchesFilter && matchesSearch
  }

  val runningCount = environments.count { it.status == EnvironmentHealthStatus.RUNNING }
  val pausedCount = environments.count { it.status == EnvironmentHealthStatus.PAUSED }
  val errorCount = environments.count { it.status == EnvironmentHealthStatus.ERROR }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "ভার্চুয়াল ল্যাব স্ট্যাটাস",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Virtual Environment States & Health Monitor",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("status_screen_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Dashboard"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      )
    }
  ) { paddingValues ->
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      color = MaterialTheme.colorScheme.background
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp)
          .testTag("status_screen_lazy_column"),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // KPI Summary Cards Row
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            StatusKpiMetricCard(
              title = "Running",
              count = runningCount,
              color = Color(0xFF2E7D32),
              icon = Icons.Default.CheckCircle,
              modifier = Modifier.weight(1f)
            )
            StatusKpiMetricCard(
              title = "Paused",
              count = pausedCount,
              color = Color(0xFFED6C02),
              icon = Icons.Default.Pause,
              modifier = Modifier.weight(1f)
            )
            StatusKpiMetricCard(
              title = "Error",
              count = errorCount,
              color = Color(0xFFD32F2F),
              icon = Icons.Default.Error,
              modifier = Modifier.weight(1f)
            )
          }
        }

        // Search Bar
        item {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("এনভায়রনমেন্টের নাম বা ওএস দিয়ে খুঁজুন...", fontSize = 13.sp) },
            leadingIcon = {
              Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("status_screen_search_bar")
          )
        }

        // Filter Chips Row
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = selectedFilter == null,
              onClick = { selectedFilter = null },
              label = { Text("All (${environments.size})") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
              ),
              modifier = Modifier.testTag("filter_chip_all")
            )

            EnvironmentHealthStatus.values().forEach { status ->
              FilterChip(
                selected = selectedFilter == status,
                onClick = { selectedFilter = if (selectedFilter == status) null else status },
                label = { Text("${status.label} (${environments.count { it.status == status }})") },
                leadingIcon = {
                  Icon(
                    imageVector = status.icon,
                    contentDescription = null,
                    tint = status.color,
                    modifier = Modifier.size(16.dp)
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = status.color.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag("filter_chip_${status.name.lowercase()}")
              )
            }
          }
        }

        // Environment Cards List
        if (filteredEnvironments.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "কোনো ভার্চুয়াল এনভায়রনমেন্ট পাওয়া যায়নি।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
              )
            }
          }
        } else {
          items(filteredEnvironments, key = { it.id }) { env ->
            VirtualEnvironmentStatusCard(
              item = env,
              onToggleState = {
                val profileId = when (env.id) {
                  "env_aosp_14" -> "profile_android_14"
                  "env_android_12_lite" -> "profile_android_12_lite"
                  "env_alpine_linux" -> "profile_alpine_sandbox"
                  else -> null
                }
                if (profileId != null) {
                  VirtualSystemCatalog.toggleProfileRunState(profileId)
                }
              },
              onAiTroubleshoot = {
                onLaunchAiTroubleshoot(env.name, env.errorDetails)
              }
            )
          }
        }
      }
    }
  }
}

@Composable
fun StatusKpiMetricCard(
  title: String,
  count: Int,
  color: Color,
  icon: ImageVector,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = color,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = color
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun VirtualEnvironmentStatusCard(
  item: LabEnvironmentItem,
  onToggleState: () -> Unit,
  onAiTroubleshoot: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulsing_dot")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alpha"
  )

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("environment_card_${item.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Header: OS Icon, Name, and Status Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(
                when (item.category) {
                  "Android Sandbox" -> MaterialTheme.colorScheme.primaryContainer
                  "Linux Container" -> Color(0xFFFFF3E0)
                  else -> MaterialTheme.colorScheme.secondaryContainer
                }
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = when (item.category) {
                "Android Sandbox" -> Icons.Default.Android
                "Linux Container" -> Icons.Default.Terminal
                else -> Icons.Default.Cloud
              },
              contentDescription = null,
              tint = when (item.category) {
                "Android Sandbox" -> MaterialTheme.colorScheme.primary
                "Linux Container" -> Color(0xFFE65100)
                else -> MaterialTheme.colorScheme.secondary
              },
              modifier = Modifier.size(22.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = item.name,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1
            )
            Text(
              text = "${item.osType} • ${item.category}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Status Badge Pill
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = item.status.color.copy(alpha = 0.12f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(item.status.color)
                .then(
                  if (item.status == EnvironmentHealthStatus.RUNNING) {
                    Modifier.alpha(pulseAlpha)
                  } else Modifier
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = item.status.label.uppercase(),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = item.status.color
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Error Alert Banner (if status is ERROR)
      if (item.status == EnvironmentHealthStatus.ERROR && item.errorDetails != null) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(
              imageVector = Icons.Default.BugReport,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "কনফিগারেশন বা রানটাইম ত্রুটি",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onErrorContainer
              )
              Text(
                text = item.errorDetails,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      // Telemetry Meters: CPU & RAM Progress
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("vCPU লোড", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Text("${item.cpuUsagePercent}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(4.dp))
          LinearProgressIndicator(
            progress = { item.cpuUsagePercent / 100f },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = if (item.cpuUsagePercent > 85) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("vRAM ব্যবহার", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Text("${item.ramUsageMb} / ${item.maxRamMb} MB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(4.dp))
          val ramFraction = if (item.maxRamMb > 0) item.ramUsageMb.toFloat() / item.maxRamMb else 0f
          LinearProgressIndicator(
            progress = { ramFraction },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = if (ramFraction > 0.9f) Color(0xFFD32F2F) else Color(0xFF4CAF50),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Specs Summary Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.SdStorage,
            contentDescription = null,
            tint = Color(0xFFFFA000),
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${item.storageGb} GB স্টোরেজ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.width(12.dp))

          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = if (item.networkIsolated) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (item.networkIsolated) "নেটওয়ার্ক আইসোলেটেড" else "ওপেন নেটওয়ার্ক",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Text(
          text = "আপটাইম: ${item.uptimeText}",
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
          color = MaterialTheme.colorScheme.outline
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))
      Spacer(modifier = Modifier.height(12.dp))

      // Action Buttons Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = onToggleState,
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (item.status == EnvironmentHealthStatus.RUNNING) {
              Color(0xFFED6C02)
            } else {
              MaterialTheme.colorScheme.primary
            }
          ),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = if (item.status == EnvironmentHealthStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (item.status == EnvironmentHealthStatus.RUNNING) "Pause" else "Launch",
            fontSize = 12.sp
          )
        }

        OutlinedButton(
          onClick = onAiTroubleshoot,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("AI ট্রাবলশুট", fontSize = 12.sp)
        }
      }
    }
  }
}
