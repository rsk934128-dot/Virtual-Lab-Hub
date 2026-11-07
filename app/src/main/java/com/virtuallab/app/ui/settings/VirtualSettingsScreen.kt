package com.virtuallab.app.ui.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.virtuallab.app.virtual.VirtualCore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualSettingsScreen(
  onNavigateBack: () -> Unit
) {
  BackHandler {
    onNavigateBack()
  }

  val networkIsolation by VirtualCore.networkIsolation.collectAsState()
  val externalStorageAccess by VirtualCore.externalStorageAccess.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Environment Settings",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("settings_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Navigate Back"
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
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Section: Container Isolation Policies
        Text(
          text = "CONTAINER POLICIES & SECURITY",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary
        )

        SettingsToggleCard(
          icon = Icons.Default.Public,
          title = "Enable Network Isolation",
          subtitle = "Restricts sandbox outbound socket connections and isolates virtual IP traffic from the host network.",
          checked = networkIsolation,
          onCheckedChange = { VirtualCore.setNetworkIsolation(it) },
          testTag = "network_isolation_toggle"
        )

        SettingsToggleCard(
          icon = Icons.Default.FolderShared,
          title = "Allow External Storage Access",
          subtitle = "Enables virtual sandboxes to mount and read shared device storage partitions. When disabled, containers are strictly isolated to private files.",
          checked = externalStorageAccess,
          onCheckedChange = { VirtualCore.setExternalStorageAccess(it) },
          testTag = "external_storage_toggle"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Section: Environment Security Status
        Text(
          text = "POLICY STATUS SUMMARY",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary
        )

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
          )
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            PolicyStatusRow(
              icon = Icons.Default.Shield,
              label = "Network Status",
              value = if (networkIsolation) "Isolated (Offline Container Mode)" else "Bridged (Host Network Access)",
              isStrict = networkIsolation
            )

            Spacer(modifier = Modifier.height(12.dp))

            PolicyStatusRow(
              icon = Icons.Default.Lock,
              label = "Filesystem Enclosure",
              value = if (!externalStorageAccess) "Strictly Private Storage (/data/data)" else "Shared Storage Permitted",
              isStrict = !externalStorageAccess
            )
          }
        }
      }
    }
  }
}

@Composable
fun SettingsToggleCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.Top
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
              if (checked) MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.surface
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.testTag(testTag),
        colors = SwitchDefaults.colors(
          checkedThumbColor = MaterialTheme.colorScheme.primary,
          checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
        )
      )
    }
  }
}

@Composable
fun PolicyStatusRow(
  icon: ImageVector,
  label: String,
  value: String,
  isStrict: Boolean
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = if (isStrict) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
      modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Column {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodySmall,
        color = if (isStrict) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
      )
    }
  }
}
