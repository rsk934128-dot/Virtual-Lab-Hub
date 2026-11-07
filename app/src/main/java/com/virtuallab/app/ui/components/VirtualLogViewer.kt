package com.virtuallab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.virtuallab.app.virtual.VirtualLogEntry

@Composable
fun VirtualLogViewer(
  modifier: Modifier = Modifier
) {
  val logs by VirtualCore.liveLogs.collectAsState()

  Card(
    modifier = modifier.testTag("virtual_log_viewer_card"),
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
              imageVector = Icons.Default.Terminal,
              contentDescription = "Terminal Logs",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Virtual Process & Event Stream",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "${logs.size} telemetry & lifecycle event(s)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
        }

        IconButton(
          onClick = { VirtualCore.clearLogs() },
          modifier = Modifier.testTag("clear_logs_button")
        ) {
          Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = "Clear Logs",
            tint = MaterialTheme.colorScheme.outline
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(240.dp)
          .clip(RoundedCornerShape(12.dp)),
        color = Color(0xFF1E1E24)
      ) {
        if (logs.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No container events recorded yet",
              style = MaterialTheme.typography.bodySmall,
              color = Color.LightGray.copy(alpha = 0.6f),
              fontFamily = FontFamily.Monospace
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(logs, key = { it.id }) { log ->
              LogItemRow(log)
            }
          }
        }
      }
    }
  }
}

@Composable
fun LogItemRow(entry: VirtualLogEntry) {
  val levelColor = when (entry.level) {
    LogLevel.DEBUG -> Color(0xFF81D4FA)
    LogLevel.INFO -> Color(0xFFA5D6A7)
    LogLevel.WARN -> Color(0xFFFFE082)
    LogLevel.ERROR -> Color(0xFFEF9A9A)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("log_entry_${entry.id}")
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = entry.timestamp,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        color = Color.Gray
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "[${entry.level.name}]",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = levelColor
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = entry.subsystem,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        color = Color.White
      )
    }

    Text(
      text = entry.event,
      fontSize = 12.sp,
      fontFamily = FontFamily.Monospace,
      color = Color(0xFFEEEEEE),
      modifier = Modifier.padding(start = 4.dp, top = 2.dp)
    )

    if (!entry.details.isNullOrBlank()) {
      Text(
        text = "↳ ${entry.details}",
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        color = Color.LightGray.copy(alpha = 0.8f),
        modifier = Modifier.padding(start = 12.dp)
      )
    }
  }
}
