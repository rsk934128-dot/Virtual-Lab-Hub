package com.virtuallab.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.virtuallab.app.virtual.LogLevel
import com.virtuallab.app.virtual.VirtualCore
import com.virtuallab.app.virtual.VirtualPackageItem
import com.virtuallab.app.virtual.VirtualSystemCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

data class ExtractedAppInfo(
  val appName: String,
  val indexUrl: String,
  val sourceZipName: String,
  val totalFiles: Int
)

/**
 * Handles unzipping and live execution of Web/Capacitor/PWA archives within Android WebView sandbox
 */
object ZipAppRunner {

  suspend fun unpackZipFromStream(context: Context, inputStream: InputStream, zipName: String): ExtractedAppInfo = withContext(Dispatchers.IO) {
    val outputDir = File(context.filesDir, "extracted_apps/$zipName")
    if (outputDir.exists()) {
      outputDir.deleteRecursively()
    }
    outputDir.mkdirs()

    var count = 0
    var foundIndex = false

    ZipInputStream(inputStream).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        val entryName = entry.name
        // Prevent zip slip vulnerability
        val destFile = File(outputDir, entryName)
        if (!destFile.canonicalPath.startsWith(outputDir.canonicalPath)) {
          entry = zis.nextEntry
          continue
        }

        if (entry.isDirectory) {
          destFile.mkdirs()
        } else {
          destFile.parentFile?.mkdirs()
          FileOutputStream(destFile).use { fos ->
            zis.copyTo(fos)
          }
          count++
          if (entryName.endsWith("index.html")) {
            foundIndex = true
          }
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }

    // Determine target index.html path
    val targetIndexFile = findIndexHtml(outputDir)
    val indexUrl = if (targetIndexFile != null) {
      "file://${targetIndexFile.absolutePath}"
    } else {
      // Fallback to internal assets app
      "file:///android_asset/rubel_app/index.html"
    }

    VirtualCore.postLog(
      LogLevel.INFO,
      "ZipExtractor",
      "Extracted ZIP archive successfully",
      "$zipName: $count files extracted. Index: $indexUrl"
    )

    ExtractedAppInfo(
      appName = zipName.removeSuffix(".zip").replace("-", " "),
      indexUrl = indexUrl,
      sourceZipName = zipName,
      totalFiles = count
    )
  }

  private fun findIndexHtml(dir: File): File? {
    val direct = File(dir, "index.html")
    if (direct.exists()) return direct

    val publicDir = File(dir, "public/index.html")
    if (publicDir.exists()) return publicDir

    val androidAssetPublic = File(dir, "android/app/src/main/assets/public/index.html")
    if (androidAssetPublic.exists()) return androidAssetPublic

    // Search 3 levels deep
    dir.walkTopDown().maxDepth(4).forEach { file ->
      if (file.isFile && file.name.equals("index.html", ignoreCase = true)) {
        return file
      }
    }
    return null
  }
}

@Composable
fun ZipAppLauncherCard(
  onLaunchLiveApp: (appInfo: ExtractedAppInfo) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isExtracting by remember { mutableStateOf(false) }
  var extractionStatus by remember { mutableStateOf<String?>(null) }
  var lastExtractedApp by remember { mutableStateOf<ExtractedAppInfo?>(null) }

  // File Picker for custom uploaded ZIPs
  val zipPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      isExtracting = true
      extractionStatus = "জিপ ফাইল এক্সট্রাক্ট করা হচ্ছে..."
      scope.launch {
        try {
          context.contentResolver.openInputStream(uri)?.use { stream ->
            val info = ZipAppRunner.unpackZipFromStream(context, stream, "uploaded_app.zip")
            lastExtractedApp = info
            extractionStatus = "সফলভাবে ওপেন হয়েছে! ${info.totalFiles} টি ফাইল আনপ্যাক হয়েছে।"
            // Register app into VirtualSystemCatalog
            VirtualSystemCatalog.installPackageToProfile(
              profileId = "profile_android_14",
              newPkg = VirtualPackageItem(
                packageName = "com.virtual.${info.appName.lowercase().replace(" ", "")}",
                appName = info.appName,
                version = "1.0.0",
                category = "Extracted ZIP App"
              )
            )
          }
        } catch (e: Exception) {
          extractionStatus = "ত্রুটি: ${e.localizedMessage}"
          VirtualCore.postLog(LogLevel.ERROR, "ZipExtractor", "Failed to extract uploaded ZIP", e.localizedMessage)
        } finally {
          isExtracting = false
        }
      }
    }
  }

  Card(
    modifier = modifier.testTag("zip_app_launcher_card"),
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
            .background(Color(0xFFE91E63)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.FolderZip,
            contentDescription = "ZIP Live App",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "জিপ অ্যাপ অটোমেটিক লাইভ প্লেয়ার",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Rubel Video & Audio Downloader (.ZIP)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "আপনার আপলোড করা Rubel-Video-Audio-Downloader-main.zip ফাইলটি সরাসরি আমাদের অ্যাপের ভেতরে ইন্টিগ্রেট করা হয়েছে। নিচের বোতামে চাপলেই ফাইলটি স্বয়ংক্রিয়ভাবে ওপেন হয়ে লাইভ চলবে।",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        lineHeight = 16.sp
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Preloaded Bundled Rubel Downloader Quick Launch
      Surface(
        shape = RoundedCornerShape(12.dp),
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
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                tint = Color(0xFFF43F5E),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Rubel Video/Audio Downloader",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                fontSize = 13.sp
              )
              Text(
                text = "Source: Rubel-Video-Audio-Downloader.zip",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                fontSize = 10.sp
              )
            }
          }

          Button(
            onClick = {
              VirtualCore.postLog(
                LogLevel.INFO,
                "ZipRunner",
                "Launching bundled Rubel Downloader app",
                "Path: file:///android_asset/rubel_app/index.html"
              )
              onLaunchLiveApp(
                ExtractedAppInfo(
                  appName = "Rubel Video & Audio Downloader",
                  indexUrl = "file:///android_asset/rubel_app/index.html",
                  sourceZipName = "Rubel-Video-Audio-Downloader-main.zip",
                  totalFiles = 211
                )
              )
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("launch_rubel_app_button")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("লাইভ চালান")
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Option to upload & run any other custom ZIP file
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = {
            zipPickerLauncher.launch("application/zip")
          },
          enabled = !isExtracting,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("upload_custom_zip_button")
        ) {
          Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("নতুন জিপ আপলোড করুন (Upload .ZIP)")
        }

        lastExtractedApp?.let { app ->
          Button(
            onClick = { onLaunchLiveApp(app) },
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
          }
        }
      }

      if (isExtracting) {
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }

      if (extractionStatus != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = extractionStatus ?: "",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          fontSize = 11.sp
        )
      }
    }
  }
}

/**
 * Live Fullscreen Runner for the extracted ZIP application
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveAppRunnerScreen(
  appInfo: ExtractedAppInfo,
  onClose: () -> Unit
) {
  BackHandler {
    onClose()
  }

  var webViewInstance by remember { mutableStateOf<WebView?>(null) }
  var pageProgress by remember { mutableStateOf(0) }
  var isLoading by remember { mutableStateOf(true) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = appInfo.appName,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1
            )
            Text(
              text = "Live Sandbox Container • ${appInfo.sourceZipName}",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("close_live_app_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = { webViewInstance?.reload() }
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reload")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      if (isLoading) {
        LinearProgressIndicator(
          progress = { pageProgress / 100f },
          modifier = Modifier.fillMaxWidth(),
          color = MaterialTheme.colorScheme.primary
        )
      }

      AndroidView(
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              databaseEnabled = true
              allowFileAccess = true
              allowContentAccess = true
              allowFileAccessFromFileURLs = true
              allowUniversalAccessFromFileURLs = true
              useWideViewPort = true
              loadWithOverviewMode = true
              mediaPlaybackRequiresUserGesture = false
              cacheMode = WebSettings.LOAD_DEFAULT
            }

            webChromeClient = object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                pageProgress = newProgress
                if (newProgress >= 100) {
                  isLoading = false
                }
              }
            }

            webViewClient = object : WebViewClient() {
              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                isLoading = true
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                isLoading = false
              }

              override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                VirtualCore.postLog(
                  LogLevel.WARN,
                  "LiveWebView",
                  "Resource load notice: ${error?.description}"
                )
              }
            }

            loadUrl(appInfo.indexUrl)
            webViewInstance = this
          }
        },
        modifier = Modifier
          .fillMaxSize()
          .testTag("live_webview_container")
      )
    }
  }
}
