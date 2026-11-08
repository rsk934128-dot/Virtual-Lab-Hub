package com.virtuallab.app.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.virtuallab.app.virtual.LogLevel
import com.virtuallab.app.virtual.VirtualCore
import com.virtuallab.app.virtual.VirtualEnvironmentState
import com.virtuallab.app.virtual.VirtualSystemCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "AiTroubleshooter"

enum class ChatSender {
  USER, ASSISTANT, SYSTEM
}

data class ChatMessage(
  val id: String = UUID.randomUUID().toString(),
  val sender: ChatSender,
  val text: String,
  val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
  val isStreaming: Boolean = false,
  val error: String? = null,
  val telemetrySnippet: String? = null
)

sealed interface TroubleshooterUiState {
  data object Idle : TroubleshooterUiState
  data object Loading : TroubleshooterUiState
  data class Streaming(val currentReply: String) : TroubleshooterUiState
  data class Success(val lastReply: String) : TroubleshooterUiState
  data class Error(val message: String) : TroubleshooterUiState
}

class VirtualLabAiTroubleshooterViewModel : ViewModel() {

  // Lazy initialization of Firebase AI Logic model as mandated
  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        temperature = 0.3f
        topP = 0.9f
        maxOutputTokens = 2048
      }
    )
  }

  private val _messages = MutableStateFlow<List<ChatMessage>>(
    listOf(
      ChatMessage(
        sender = ChatSender.SYSTEM,
        text = "স্বাগতম! আমি Virtual Lab Hub AI ট্রাবলশুটার অ্যাসিস্ট্যান্ট (Firebase Genkit & AI Logic চালিত)। আপনার ভার্চুয়াল কন্টেইনার, স্যান্ডবক্স মেমরি, নেটওয়ার্ক আইসোলেশন ও কনফিগারেশন সমস্যা সমাধানে সাহায্য করতে প্রস্তুত।"
      )
    )
  )
  val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

  private val _uiState = MutableStateFlow<TroubleshooterUiState>(TroubleshooterUiState.Idle)
  val uiState: StateFlow<TroubleshooterUiState> = _uiState.asStateFlow()

  private val chatHistory = mutableListOf<Content>()

  /**
   * Builds live diagnostic telemetry snapshot of the virtual environment to ground the AI.
   */
  fun buildDiagnosticTelemetry(): String {
    val engineState = when (val state = VirtualCore.engineState.value) {
      is VirtualEnvironmentState.Ready -> "READY (Dir: ${state.containerDir}, Active: ${state.activeSandboxes})"
      is VirtualEnvironmentState.Initializing -> "INITIALIZING (${(state.progress * 100).toInt()}%)"
      is VirtualEnvironmentState.Error -> "ERROR: ${state.message}"
      VirtualEnvironmentState.Uninitialized -> "UNINITIALIZED"
    }

    val runningContainers = VirtualCore.getRunningContainers()
    val profiles = VirtualSystemCatalog.activeProfiles.value.joinToString(separator = "\n") { p ->
      "  - ${p.name} [OS: ${p.osType}, Running: ${p.isRunning}, vRAM: ${p.virtualRamMb}MB, vDisk: ${p.virtualStorageGb}GB, Apps: ${p.installedApps.size}]"
    }

    val networkStatus = if (VirtualCore.networkIsolation.value) "ENABLED (Outbound sockets restricted)" else "DISABLED (Direct networking allowed)"
    val storageStatus = if (VirtualCore.externalStorageAccess.value) "ALLOWED (Shared storage mounted)" else "RESTRICTED (Internal sandbox only)"

    val recentLogs = VirtualCore.liveLogs.value.take(6).joinToString(separator = "\n") { l ->
      "  [${l.timestamp}] [${l.level}] ${l.subsystem}: ${l.event}${if (l.details != null) " - ${l.details}" else ""}"
    }

    return """
[Virtual Lab Environment Live Diagnostics]
- Engine State: $engineState
- Active Sandbox Count: ${VirtualCore.getActiveSandboxCount()}
- Running Container IDs: ${if (runningContainers.isEmpty()) "None (All Standby)" else runningContainers.joinToString(", ")}
- Network Isolation Policy: $networkStatus
- External Storage Policy: $storageStatus
- System Profiles:
$profiles
- Recent System Logs:
$recentLogs
    """.trimIndent()
  }

  /**
   * Sends user query with live environment context and streams the Gemini response.
   */
  fun sendMessage(userText: String, includeTelemetry: Boolean = true) {
    if (userText.isBlank() || _uiState.value is TroubleshooterUiState.Loading || _uiState.value is TroubleshooterUiState.Streaming) {
      return
    }

    val telemetry = if (includeTelemetry) buildDiagnosticTelemetry() else null
    val userMsg = ChatMessage(
      sender = ChatSender.USER,
      text = userText.trim(),
      telemetrySnippet = telemetry
    )

    val currentList = _messages.value.toMutableList()
    currentList.add(userMsg)

    // Placeholder assistant message for streaming
    val assistantMsgId = UUID.randomUUID().toString()
    val initialAssistantMsg = ChatMessage(
      id = assistantMsgId,
      sender = ChatSender.ASSISTANT,
      text = "",
      isStreaming = true
    )
    currentList.add(initialAssistantMsg)
    _messages.value = currentList

    viewModelScope.launch {
      _uiState.value = TroubleshooterUiState.Loading
      VirtualCore.postLog(
        LogLevel.INFO,
        "AIAssistant",
        "AI Troubleshooting Query Initiated",
        "Prompt length: ${userText.length} chars (Context Attached: ${includeTelemetry})"
      )

      val accumulatedReply = StringBuilder()
      try {
        val promptWithContext = buildString {
          appendLine("You are the expert Virtual Lab Hub AI Assistant and Technical Troubleshooter.")
          appendLine("The user is managing user-space virtual Android containers, Linux guest profiles (Alpine/AOSP), ZIP sandboxes, and cloud tooling.")
          appendLine("Provide concise, highly accurate, actionable troubleshooting steps.")
          appendLine("When suggesting shell or container commands, format them in markdown backticks.")
          appendLine("Language guideline: Provide helpful answers in Bengali or English based on user's query language.")
          if (telemetry != null) {
            appendLine("\n--- LIVE ENVIRONMENT TELEMETRY ---")
            appendLine(telemetry)
            appendLine("--- END TELEMETRY ---\n")
          }
          appendLine("User Question: $userText")
        }

        // Realistic coroutine timeout window of 60s as mandated
        withTimeout(60_000L) {
          model.generateContentStream(promptWithContext).collect { chunk ->
            val chunkText = chunk.text.orEmpty()
            accumulatedReply.append(chunkText)
            val updatedText = accumulatedReply.toString()

            _uiState.value = TroubleshooterUiState.Streaming(updatedText)
            updateMessageContent(assistantMsgId, updatedText, isStreaming = true)
          }
        }

        val finalText = accumulatedReply.toString()
        if (finalText.isNotBlank()) {
          _uiState.value = TroubleshooterUiState.Success(finalText)
          updateMessageContent(assistantMsgId, finalText, isStreaming = false)
          chatHistory.add(content("user") { text(userText) })
          chatHistory.add(content("model") { text(finalText) })

          VirtualCore.postLog(
            LogLevel.INFO,
            "AIAssistant",
            "AI Troubleshooting Stream Finished",
            "Tokens received successfully"
          )
        } else {
          val emptyMsg = "মডেল থেকে কোনো তথ্য পাওয়া যায়নি (Empty Response)। অনুগ্রহ করে পুনরায় প্রশ্ন করুন।"
          _uiState.value = TroubleshooterUiState.Error(emptyMsg)
          updateMessageContent(assistantMsgId, emptyMsg, isStreaming = false, error = emptyMsg)
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI Generation timed out", e)
        val errorMsg = "AI রেসপন্স পেতে ৬০ সেকেন্ডের বেশি সময় লেগেছে (Timeout)। অনুগ্রহ করে আবার চেষ্টা করুন।"
        _uiState.value = TroubleshooterUiState.Error(errorMsg)
        updateMessageContent(assistantMsgId, errorMsg, isStreaming = false, error = errorMsg)
        VirtualCore.postLog(LogLevel.ERROR, "AIAssistant", "Generation Timed Out", e.message)
      } catch (e: CancellationException) {
        // MUST rethrow CancellationException to preserve cooperative coroutine cancellation
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "AI Troubleshooting query failed", e)
        val errorMsg = e.localizedMessage ?: "AI সার্ভিস সংযোগে সমস্যা হয়েছে।"
        _uiState.value = TroubleshooterUiState.Error(errorMsg)
        updateMessageContent(
          assistantMsgId,
          "ত্রুটি: $errorMsg\n(App Check attestation বা নেটওয়ার্ক সংযোগ পরীক্ষা করুন।)",
          isStreaming = false,
          error = errorMsg
        )
        VirtualCore.postLog(LogLevel.ERROR, "AIAssistant", "AI Query Failed", errorMsg)
      }
    }
  }

  private fun updateMessageContent(
    id: String,
    newText: String,
    isStreaming: Boolean,
    error: String? = null
  ) {
    val updated = _messages.value.map { msg ->
      if (msg.id == id) {
        msg.copy(
          text = newText,
          isStreaming = isStreaming,
          error = error
        )
      } else msg
    }
    _messages.value = updated
  }

  fun clearChat() {
    chatHistory.clear()
    _messages.value = listOf(
      ChatMessage(
        sender = ChatSender.SYSTEM,
        text = "চ্যাট হিস্ট্রি মুছে ফেলা হয়েছে। আপনি নতুন করে আপনার ভার্চুয়াল ল্যাবের সমস্যা সম্পর্কে প্রশ্ন করতে পারেন।"
      )
    )
    _uiState.value = TroubleshooterUiState.Idle
  }
}
