package com.virtuallab.app.virtual

import android.content.Context
import android.net.Uri
import com.virtuallab.app.virtual.LogLevel
import com.virtuallab.app.virtual.VirtualCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Metadata for a sandboxed lab package extracted from a ZIP file
 */
data class SandboxedLabItem(
  val id: String = UUID.randomUUID().toString(),
  val labName: String,
  val labDirectory: File,
  val totalFiles: Int,
  val totalSizeBytes: Long,
  val extractedAt: String = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date()),
  val mainExecutablePath: String? = null
)

/**
 * FileExplorerService manages the sandboxed 'labs' workspace within the app's internal storage,
 * providing safe ZIP extraction using ZipInputStream with Zip Slip protection.
 */
class FileExplorerService(private val context: Context) {

  private val labsBaseDir = File(context.filesDir, "labs").apply {
    if (!exists()) mkdirs()
  }

  private val _labs = MutableStateFlow<List<SandboxedLabItem>>(emptyList())
  val labs: StateFlow<List<SandboxedLabItem>> = _labs.asStateFlow()

  init {
    refreshLabs()
  }

  /**
   * Refreshes the list of currently extracted lab workspaces.
   */
  fun refreshLabs() {
    val items = mutableListOf<SandboxedLabItem>()
    val labDirs = labsBaseDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

    for (dir in labDirs) {
      val files = dir.walkTopDown().filter { it.isFile }.toList()
      val totalBytes = files.sumOf { it.length() }
      val mainExec = findMainEntryFile(dir)?.absolutePath

      items.add(
        SandboxedLabItem(
          labName = dir.name.replace("_", " "),
          labDirectory = dir,
          totalFiles = files.size,
          totalSizeBytes = totalBytes,
          mainExecutablePath = mainExec
        )
      )
    }

    _labs.value = items.sortedByDescending { it.extractedAt }
  }

  /**
   * Extracts a ZIP file stream into an isolated sandbox folder inside the 'labs' directory.
   */
  suspend fun extractZipToLabSandbox(
    inputStream: InputStream,
    zipFileName: String,
    customLabName: String? = null
  ): Result<SandboxedLabItem> = withContext(Dispatchers.IO) {
    val cleanName = (customLabName?.ifBlank { null } ?: zipFileName.removeSuffix(".zip"))
      .trim()
      .replace("[^a-zA-Z0-9._-]".toRegex(), "_")

    val targetLabDir = File(labsBaseDir, cleanName)
    if (targetLabDir.exists()) {
      targetLabDir.deleteRecursively()
    }
    targetLabDir.mkdirs()

    var fileCount = 0
    var totalBytes = 0L

    try {
      VirtualCore.postLog(
        LogLevel.INFO,
        "FileExplorer",
        "Unpacking ZIP archive to sandboxed lab directory",
        "Target: labs/$cleanName"
      )

      ZipInputStream(inputStream).use { zis ->
        var entry = zis.nextEntry
        val canonicalDestDirPath = targetLabDir.canonicalPath

        while (entry != null) {
          val destFile = File(targetLabDir, entry.name)
          val canonicalDestFilePath = destFile.canonicalPath

          // Enforce strict Zip Slip vulnerability defense
          if (!canonicalDestFilePath.startsWith(canonicalDestDirPath + File.separator) && canonicalDestFilePath != canonicalDestDirPath) {
            VirtualCore.postLog(
              LogLevel.ERROR,
              "FileExplorer",
              "Zip Slip traversal blocked",
              "Entry: ${entry.name}"
            )
            entry = zis.nextEntry
            continue
          }

          if (entry.isDirectory) {
            destFile.mkdirs()
          } else {
            destFile.parentFile?.mkdirs()
            FileOutputStream(destFile).use { fos ->
              val copied = zis.copyTo(fos)
              totalBytes += copied
            }
            fileCount++
          }
          zis.closeEntry()
          entry = zis.nextEntry
        }
      }

      val mainEntry = findMainEntryFile(targetLabDir)?.absolutePath
      val labItem = SandboxedLabItem(
        labName = cleanName.replace("_", " "),
        labDirectory = targetLabDir,
        totalFiles = fileCount,
        totalSizeBytes = totalBytes,
        mainExecutablePath = mainEntry
      )

      refreshLabs()

      VirtualCore.postLog(
        LogLevel.INFO,
        "FileExplorer",
        "Lab sandbox extraction complete",
        "${labItem.labName}: $fileCount files extracted (${totalBytes / 1024} KB)"
      )

      Result.success(labItem)
    } catch (e: Exception) {
      VirtualCore.postLog(
        LogLevel.ERROR,
        "FileExplorer",
        "Extraction error",
        e.localizedMessage ?: "Unknown ZIP extraction error"
      )
      Result.failure(e)
    }
  }

  /**
   * Convenience overload to extract directly from an Android content URI.
   */
  suspend fun extractZipFromUri(uri: Uri, fileName: String): Result<SandboxedLabItem> {
    return try {
      val stream = context.contentResolver.openInputStream(uri)
        ?: return Result.failure(IllegalStateException("Could not open input stream from URI"))
      stream.use { s ->
        extractZipToLabSandbox(s, fileName)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Safely deletes a sandboxed lab environment from internal storage.
   */
  fun deleteLab(labDirectory: File): Boolean {
    val deleted = labDirectory.deleteRecursively()
    refreshLabs()
    VirtualCore.postLog(
      LogLevel.WARN,
      "FileExplorer",
      "Purged lab sandbox environment",
      "Path: ${labDirectory.name}"
    )
    return deleted
  }

  private fun findMainEntryFile(dir: File): File? {
    val directIndex = File(dir, "index.html")
    if (directIndex.exists()) return directIndex

    val publicIndex = File(dir, "public/index.html")
    if (publicIndex.exists()) return publicIndex

    val distIndex = File(dir, "dist/index.html")
    if (distIndex.exists()) return distIndex

    return dir.walkTopDown().maxDepth(4).firstOrNull { file ->
      file.isFile && (file.name.equals("index.html", ignoreCase = true) || file.name.equals("main.sh", ignoreCase = true))
    }
  }
}
