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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
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

data class VercelDeployment(
  val projectName: String,
  val deploymentUrl: String,
  val framework: String, // "Next.js", "React / Vite", "Node.js Serverless", "HTML/Static"
  val status: String, // "READY", "BUILDING", "QUEUED"
  val branch: String,
  val deployedAt: String
)

@Composable
fun VercelHostingWorkbench(
  modifier: Modifier = Modifier
) {
  var projectNameInput by remember { mutableStateOf("virtual-lab-frontend") }
  var frameworkInput by remember { mutableStateOf("Next.js App Router") }
  var isDeploying by remember { mutableStateOf(false) }
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()

  var deployments by remember {
    mutableStateOf(
      listOf(
        VercelDeployment(
          projectName = "virtual-lab-dashboard",
          deploymentUrl = "https://virtual-lab-dashboard.vercel.app",
          framework = "Next.js",
          status = "READY",
          branch = "main",
          deployedAt = "Just now"
        ),
        VercelDeployment(
          projectName = "virtual-api-gateway",
          deploymentUrl = "https://virtual-api-gateway.vercel.app",
          framework = "Node.js Serverless",
          status = "READY",
          branch = "production",
          deployedAt = "2 hours ago"
        )
      )
    )
  }

  Card(
    modifier = modifier.testTag("vercel_hosting_card"),
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
            .background(Color(0xFF111111)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.CloudQueue,
            contentDescription = "Vercel Hosting",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "ভারসেল হোস্টিং ইন্টিগ্রেশন (Vercel Cloud)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Deploy frontend & fullstack web apps with instant CDN",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "ভার্চুয়াল ল্যাব থেকে যেকোনো ওয়েব প্রজেক্ট বা ড্যাশবোর্ড সরাসরি ভারসেলে (Vercel) লাইভ ডেপ্লয় করুন এবং তার লাইভ URL ভার্চুয়াল ব্রাউজার বা TWA অ্যাপে টেস্ট করুন।",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        lineHeight = 16.sp
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Input Form
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = projectNameInput,
          onValueChange = { projectNameInput = it },
          label = { Text("প্রজেক্টের নাম (Project Name)") },
          singleLine = true,
          modifier = Modifier.weight(1f)
        )

        Button(
          onClick = {
            if (projectNameInput.isNotBlank()) {
              isDeploying = true
              coroutineScope.launch {
                val cleanName = projectNameInput.trim().lowercase().replace(" ", "-")
                VirtualCore.postLog(
                  LogLevel.INFO,
                  "VercelDeployer",
                  "Initiating Vercel deployment build",
                  "Project: $cleanName, Framework: $frameworkInput"
                )

                delay(1500L) // Simulate edge build

                val generatedUrl = "https://$cleanName.vercel.app"
                val newDeployment = VercelDeployment(
                  projectName = cleanName,
                  deploymentUrl = generatedUrl,
                  framework = frameworkInput,
                  status = "READY",
                  branch = "main",
                  deployedAt = "Just now"
                )

                deployments = listOf(newDeployment) + deployments
                isDeploying = false

                VirtualCore.postLog(
                  LogLevel.INFO,
                  "VercelDeployer",
                  "Vercel deployment live successfully",
                  "Live URL: $generatedUrl"
                )
              }
            }
          },
          enabled = projectNameInput.isNotBlank() && !isDeploying,
          shape = RoundedCornerShape(10.dp)
        ) {
          if (isDeploying) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp
            )
          } else {
            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Deploy")
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "অ্যাক্টিভ ডেপ্লয়মেন্টসমূহ (Active Deployments)",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(8.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        deployments.forEach { dep ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(if (dep.status == "READY") Color(0xFF4CAF50) else Color.Yellow)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = dep.projectName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    fontSize = 14.sp
                  )
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.primaryContainer
                ) {
                  Text(
                    text = dep.status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = dep.deploymentUrl,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp
              )

              Spacer(modifier = Modifier.height(2.dp))

              Text(
                text = "Framework: ${dep.framework} • Branch: ${dep.branch} • ${dep.deployedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                fontSize = 10.sp
              )

              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Button(
                  onClick = {
                    clipboardManager.setText(AnnotatedString(dep.deploymentUrl))
                    VirtualCore.postLog(LogLevel.INFO, "Vercel", "URL copied to clipboard", dep.deploymentUrl)
                  },
                  modifier = Modifier.height(32.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Copy Link", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                  onClick = {
                    // Install into virtual environment catalog as testing web app
                    val activeProfileId = VirtualSystemCatalog.activeProfiles.value.firstOrNull()?.id ?: "profile_android_14"
                    VirtualSystemCatalog.installPackageToProfile(
                      profileId = activeProfileId,
                      newPkg = VirtualPackageItem(
                        packageName = "com.vercel.${dep.projectName.replace("-", "")}",
                        appName = "${dep.projectName} (Vercel App)",
                        version = "1.0.0",
                        category = "Vercel Web App"
                      )
                    )
                  },
                  modifier = Modifier.height(32.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("ভার্চুয়াল ল্যাবে লিঙ্ক করুন", fontSize = 11.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}
