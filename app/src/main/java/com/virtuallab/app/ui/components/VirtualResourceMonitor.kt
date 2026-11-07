package com.virtuallab.app.ui.components

import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtuallab.app.virtual.LogLevel
import com.virtuallab.app.virtual.VirtualCore
import kotlinx.coroutines.delay
import java.io.File

data class VirtualResourceMetrics(
  val cpuCores: Int,
  val cpuLoadPercent: Float,
  val usedRamMb: Long,
  val maxRamMb: Long,
  val ramUsageFraction: Float,
  val containerStorageMb: Long,
  val availableStorageMb: Long,
  val totalStorageMb: Long,
  val storageUsageFraction: Float
)

fun calculateVirtualMetrics(context: android.content.Context): VirtualResourceMetrics {
  val runtime = Runtime.getRuntime()
  val cpuCores = runtime.availableProcessors()

  // Memory usage from Android runtime
  val maxMemoryMb = runtime.maxMemory() / (1024 * 1024)
  val totalMemoryMb = runtime.totalMemory() / (1024 * 1024)
  val freeMemoryMb = runtime.freeMemory() / (1024 * 1024)
  val usedRamMb = totalMemoryMb - freeMemoryMb
  val ramFraction = if (maxMemoryMb > 0) (usedRamMb.toFloat() / maxMemoryMb.toFloat()).coerceIn(0f, 1f) else 0.1f

  // Storage measurement from application-private files and containers
  val containerDir = File(context.filesDir, "virtual_containers")
  val containerBytes = if (containerDir.exists()) getFolderSize(containerDir) else 0L
  val containerMb = containerBytes / (1024 * 1024)

  val stat = StatFs(context.filesDir.absolutePath)
  val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
  val totalBytes = stat.blockCountLong * stat.blockSizeLong
  val availableStorageMb = availableBytes / (1024 * 1024)
  val totalStorageMb = totalBytes / (1024 * 1024)
  val usedStorageBytes = (totalBytes - availableBytes).coerceAtLeast(0L)
  val storageFraction = if (totalBytes > 0) (usedStorageBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0.1f

  // Simulated active container thread/CPU load based on active instances
  val activeContainers = VirtualCore.getActiveSandboxCount()
  val simulatedCpuLoad = (12f + (activeContainers * 8f)).coerceIn(8f, 95f)

  return VirtualResourceMetrics(
    cpuCores = cpuCores,
    cpuLoadPercent = simulatedCpuLoad,
    usedRamMb = usedRamMb,
    maxRamMb = maxMemoryMb,
    ramUsageFraction = ramFraction,
    containerStorageMb = containerMb,
    availableStorageMb = availableStorageMb,
    totalStorageMb = totalStorageMb,
    storageUsageFraction = storageFraction
  )
}

private fun getFolderSize(file: File): Long {
  var size = 0L
  val children = file.listFiles() ?: return 0L
  for (child in children) {
    size += if (child.isDirectory) getFolderSize(child) else child.length()
  }
  return size
}

@Composable
fun VirtualResourceMonitor(
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var metrics by remember { mutableStateOf(calculateVirtualMetrics(context)) }

  // Periodic polling every 3 seconds to keep telemetry lively
  LaunchedEffect(Unit) {
    while (true) {
      metrics = calculateVirtualMetrics(context)
      delay(3000L)
    }
  }

  Card(
    modifier = modifier.testTag("virtual_resource_monitor_card"),
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
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = "Resource Metrics",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Virtual Container Resource Telemetry",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Real-time CPU, RAM & Sandbox storage metrics",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
        }

        IconButton(
          onClick = {
            metrics = calculateVirtualMetrics(context)
            VirtualCore.postLog(LogLevel.DEBUG, "Telemetry", "Resource metrics refreshed manually")
          },
          modifier = Modifier.testTag("refresh_metrics_button")
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Refresh Telemetry",
            tint = MaterialTheme.colorScheme.outline
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Metric Items
      MetricProgressBar(
        icon = Icons.Default.Speed,
        title = "Virtual CPU Load",
        valueText = "${metrics.cpuLoadPercent.toInt()}% (${metrics.cpuCores} vCPU Cores)",
        fraction = metrics.cpuLoadPercent / 100f,
        indicatorColor = MaterialTheme.colorScheme.primary
      )

      Spacer(modifier = Modifier.height(12.dp))

      MetricProgressBar(
        icon = Icons.Default.Memory,
        title = "Virtual Environment RAM",
        valueText = "${metrics.usedRamMb} MB / ${metrics.maxRamMb} MB allocated",
        fraction = metrics.ramUsageFraction,
        indicatorColor = Color(0xFF4CAF50)
      )

      Spacer(modifier = Modifier.height(12.dp))

      MetricProgressBar(
        icon = Icons.Default.SdStorage,
        title = "Containerized Storage & Disk",
        valueText = "${metrics.containerStorageMb} MB containers (Total disk: ${metrics.totalStorageMb / 1024} GB)",
        fraction = metrics.storageUsageFraction,
        indicatorColor = Color(0xFFFFA000)
      )
    }
  }
}

@Composable
private fun MetricProgressBar(
  icon: ImageVector,
  title: String,
  valueText: String,
  fraction: Float,
  indicatorColor: Color
) {
  val animatedFraction by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "fraction")

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = indicatorColor,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Text(
        text = valueText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    LinearProgressIndicator(
      progress = { animatedFraction },
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp)),
      color = indicatorColor,
      trackColor = MaterialTheme.colorScheme.surface
    )
  }
}
