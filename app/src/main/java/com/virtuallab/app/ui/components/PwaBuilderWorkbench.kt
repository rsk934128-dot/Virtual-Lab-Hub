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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.virtuallab.app.virtual.VirtualPackageItem
import com.virtuallab.app.virtual.VirtualSystemCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PwaManifestAnalysis(
  val webAppUrl: String,
  val name: String,
  val shortName: String,
  val packageId: String,
  val themeColor: String,
  val displayMode: String,
  val twaReady: Boolean
)

@Composable
fun PwaBuilderWorkbench(
  modifier: Modifier = Modifier
) {
  var urlInput by remember { mutableStateOf("https://myapp.vercel.app") }
  var isPackaging by remember { mutableStateOf(false) }
  var analysisResult by remember { mutableStateOf<PwaManifestAnalysis?>(null) }
  val scope = rememberCoroutineScope()

  Card(
    modifier = modifier.testTag("pwa_builder_workbench_card"),
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
            imageVector = Icons.Default.Language,
            contentDescription = "PWABuilder",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "PWABuilder Android TWA Packaging",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Progressive Web App to Android APK/AAB Converter",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Enter any PWA / Web application URL to analyze web manifest, generate Trusted Web Activity (TWA) packages, and test deploy into your virtual environment.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        lineHeight = 16.sp
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = urlInput,
          onValueChange = { urlInput = it },
          label = { Text("PWA Web URL") },
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("pwa_url_input")
        )

        Button(
          onClick = {
            if (urlInput.isNotBlank()) {
              isPackaging = true
              scope.launch {
                VirtualCore.postLog(
                  LogLevel.INFO,
                  "PWABuilder",
                  "Analyzing PWA Manifest",
                  "Target URL: $urlInput"
                )
                delay(1200L) // Simulate manifest fetching and TWA synthesis

                val domain = urlInput.replace("https://", "").replace("http://", "").split("/").firstOrNull() ?: "webapp"
                val appLabel = domain.split(".").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "WebApp"
                val pkgId = "com.pwa.${domain.replace(".", "").lowercase()}"

                val result = PwaManifestAnalysis(
                  webAppUrl = urlInput.trim(),
                  name = "$appLabel PWA",
                  shortName = appLabel,
                  packageId = pkgId,
                  themeColor = "#6750A4",
                  displayMode = "standalone (TWA)",
                  twaReady = true
                )
                analysisResult = result
                isPackaging = false

                VirtualCore.postLog(
                  LogLevel.INFO,
                  "PWABuilder",
                  "TWA Package Synthesized",
                  "Generated Package: $pkgId (Ready for Android deployment)"
                )
              }
            }
          },
          enabled = urlInput.isNotBlank() && !isPackaging,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("pwa_analyze_button")
        ) {
          if (isPackaging) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp
            )
          } else {
            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Build")
          }
        }
      }

      analysisResult?.let { analysis ->
        Spacer(modifier = Modifier.height(14.dp))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = Color(0xFF4CAF50),
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "PWA Manifest Validated",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
              ) {
                Text(
                  text = "TWA APK READY",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "App Name: ${analysis.name} (${analysis.shortName})",
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Package ID: ${analysis.packageId}",
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Display: ${analysis.displayMode} • Color: ${analysis.themeColor}",
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
              onClick = {
                // Deploy into active virtual environment profiles
                val activeProfileId = VirtualSystemCatalog.activeProfiles.value.firstOrNull()?.id ?: "profile_android_14"
                VirtualSystemCatalog.installPackageToProfile(
                  profileId = activeProfileId,
                  newPkg = VirtualPackageItem(
                    packageName = analysis.packageId,
                    appName = analysis.name,
                    version = "1.0.0-twa",
                    category = "PWA / Web App"
                  )
                )
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("deploy_pwa_to_sandbox_button"),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("ভার্চুয়াল স্যান্ডবক্সে টেস্ট রান করুন (Deploy to Sandbox)")
            }
          }
        }
      }
    }
  }
}
