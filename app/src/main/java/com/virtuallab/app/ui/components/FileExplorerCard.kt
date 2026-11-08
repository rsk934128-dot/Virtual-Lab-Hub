package com.virtuallab.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtuallab.app.virtual.FileExplorerService
import com.virtuallab.app.virtual.SandboxedLabItem
import kotlinx.coroutines.launch

@Composable
fun FileExplorerCard(
  fileExplorerService: FileExplorerService,
  onOpenLab: (appInfo: ExtractedAppInfo) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val labs by fileExplorerService.labs.collectAsState()
  val scope = rememberCoroutineScope()
  var isExtracting by remember { mutableStateOf(false) }
  var extractionMessage by remember { mutableStateOf<String?>(null) }

  val zipPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      isExtracting = true
      extractionMessage = "জিপ ফাইল স্যান্ডবক্স ডিরেক্টরিতে আনপ্যাক করা হচ্ছে..."
      scope.launch {
        val result = fileExplorerService.extractZipFromUri(uri, "uploaded_lab_${System.currentTimeMillis()}.zip")
        isExtracting = false
        result.onSuccess { lab ->
          extractionMessage = "সফল! '${lab.labName}' ল্যাব ফোল্ডার আনপ্যাক হয়েছে (${lab.totalFiles} ফাইল)।"
        }.onFailure { err ->
          extractionMessage = "ব্যর্থ: ${err.localizedMessage}"
        }
      }
    }
  }

  Card(
    modifier = modifier.testTag("file_explorer_card"),
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
            .background(Color(0xFF00897B)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = "File Explorer",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "ফাইল এক্সপ্লোরার ও স্যান্ডবক্স ল্যাবস (File Explorer)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Internal Storage: /data/user/0/com.virtuallab.app/files/labs/",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "FileExplorer সার্ভিস ZipInputStream লাইব্রেরি ব্যবহার করে যেকোনো আপলোডকৃত ZIP ফাইল স্বয়ংক্রিয়ভাবে অ্যাপের ভেতরের সুরক্ষিত 'labs' ডিরেক্টরিতে এক্সট্র্যাক্ট ও ম্যানেজ করে।",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        lineHeight = 16.sp
      )

      Spacer(modifier = Modifier.height(14.dp))

      Button(
        onClick = { zipPicker.launch("application/zip") },
        enabled = !isExtracting,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("upload_zip_to_labs_button")
      ) {
        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("নতুন জিপ ল্যাবে আপলোড করুন (Extract ZIP to Labs)")
      }

      if (isExtracting) {
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }

      if (extractionMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = extractionMessage ?: "",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          fontSize = 11.sp
        )
      }

      if (labs.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "এক্সট্রাক্টেড ল্যাবসমূহ (${labs.size} টি ডিরেক্টরি)",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          labs.forEach { lab ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color(0xFF00897B),
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = lab.labName,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                      maxLines = 1
                    )
                    Text(
                      text = "${lab.totalFiles} files • ${lab.totalSizeBytes / 1024} KB • ${lab.extractedAt}",
                      style = MaterialTheme.typography.bodySmall,
                      fontFamily = FontFamily.Monospace,
                      fontSize = 10.sp,
                      color = MaterialTheme.colorScheme.outline
                    )
                  }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  if (lab.mainExecutablePath != null) {
                    IconButton(
                      onClick = {
                        onOpenLab(
                          ExtractedAppInfo(
                            appName = lab.labName,
                            indexUrl = "file://${lab.mainExecutablePath}",
                            sourceZipName = "${lab.labName}.zip",
                            totalFiles = lab.totalFiles
                          )
                        )
                      },
                      modifier = Modifier.testTag("run_lab_${lab.labName}")
                    ) {
                      Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run Lab",
                        tint = MaterialTheme.colorScheme.primary
                      )
                    }
                  }

                  IconButton(
                    onClick = { fileExplorerService.deleteLab(lab.labDirectory) },
                    modifier = Modifier.testTag("delete_lab_${lab.labName}")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "Delete Lab",
                      tint = MaterialTheme.colorScheme.error,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
