package com.virtuallab.app.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.virtuallab.app.ai.ChatMessage
import com.virtuallab.app.ai.ChatSender
import com.virtuallab.app.ai.TroubleshooterUiState
import com.virtuallab.app.ai.VirtualLabAiTroubleshooterViewModel

@Composable
fun VirtualLabAiAssistantWorkbench(
  modifier: Modifier = Modifier,
  troubleshooterViewModel: VirtualLabAiTroubleshooterViewModel = viewModel()
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val messages by troubleshooterViewModel.messages.collectAsState()
  val uiState by troubleshooterViewModel.uiState.collectAsState()

  var inputText by remember { mutableStateOf("") }
  var showTelemetryInspector by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()

  // Auto-scroll to latest message
  LaunchedEffect(messages.size, uiState) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  val quickPrompts = listOf(
    "🔍 সমগ্র ল্যাব ডায়াগনসিস" to "Analyze my current virtual lab environment telemetry and report any bottlenecks, memory pressure, or configuration risks.",
    "🌐 নেটওয়ার্ক আইসোলেশন সকেট" to "Why is network isolation blocking socket connections in my virtual container, and how can I safely permit local traffic?",
    "⚡ vRAM ও পারফরম্যান্স অপটিমাইজেশন" to "My container is running with high memory usage. Suggest optimal vRAM and thread allocation for Android 14 container.",
    "📦 ZIP ওয়েব ল্যাব ট্রাবলশুট" to "My extracted ZIP web application is not loading or static assets return 404 in the sandbox. How do I fix it?",
    "🛡️ স্টোরেজ পারমিশন ও পার্টিশন" to "Explain the difference between internal sandbox partition and external storage access policies."
  )

  Card(
    modifier = modifier.testTag("virtual_lab_ai_assistant_workbench"),
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
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Psychology,
              contentDescription = "AI Troubleshooter",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(26.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "AI ল্যাব ট্রাবলশুটার",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
              ) {
                Text(
                  text = "Genkit AI",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = "Firebase AI Logic (Gemini 3.8 Flash) • App Check Secured",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
        }

        IconButton(
          onClick = { troubleshooterViewModel.clearChat() },
          modifier = Modifier.testTag("clear_ai_chat_button")
        ) {
          Icon(
            imageVector = Icons.Default.DeleteSweep,
            contentDescription = "Clear Chat History",
            tint = MaterialTheme.colorScheme.outline
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Telemetry Context Bar
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "লাইভ এনভায়রনমেন্ট টেলিমেট্রি সংযুক্ত",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            TextButton(
              onClick = { showTelemetryInspector = !showTelemetryInspector }
            ) {
              Text(
                text = if (showTelemetryInspector) "টেলিমেট্রি লুকান" else "টেলিমেট্রি দেখুন",
                fontSize = 11.sp
              )
            }
          }

          AnimatedVisibility(visible = showTelemetryInspector) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
              HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.3f))
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = troubleshooterViewModel.buildDiagnosticTelemetry(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier
                  .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp)
                  )
                  .padding(8.dp)
                  .fillMaxWidth()
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Quick Prompt Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        quickPrompts.forEach { (label, prompt) ->
          AssistChip(
            onClick = {
              troubleshooterViewModel.sendMessage(prompt)
            },
            label = { Text(label, fontSize = 11.sp) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
              )
            },
            colors = AssistChipDefaults.assistChipColors(
              containerColor = MaterialTheme.colorScheme.surface
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Chat Messages List
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 220.dp, max = 340.dp)
      ) {
        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(messages, key = { it.id }) { msg ->
            ChatMessageBubble(
              message = msg,
              onCopy = { text ->
                clipboardManager.setText(AnnotatedString(text))
                Toast.makeText(context, "কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
              }
            )
          }

          if (uiState is TroubleshooterUiState.Loading) {
            item {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "AI বিশ্লেষণ করছে এবং সমাধান তৈরি করছে...",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.outline
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Message Input Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = { Text("ভার্চুয়াল ল্যাবের সমস্যা সম্পর্কে প্রশ্ন করুন...", fontSize = 13.sp) },
          modifier = Modifier
            .weight(1f)
            .testTag("ai_assistant_input_field"),
          shape = RoundedCornerShape(12.dp),
          maxLines = 3
        )

        Spacer(modifier = Modifier.width(8.dp))

        val isProcessing = uiState is TroubleshooterUiState.Loading || uiState is TroubleshooterUiState.Streaming
        Button(
          onClick = {
            if (inputText.isNotBlank() && !isProcessing) {
              val query = inputText
              inputText = ""
              troubleshooterViewModel.sendMessage(query)
            }
          },
          enabled = inputText.isNotBlank() && !isProcessing,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("ai_assistant_send_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send to AI Troubleshooter"
          )
        }
      }
    }
  }
}

@Composable
fun ChatMessageBubble(
  message: ChatMessage,
  onCopy: (String) -> Unit
) {
  val isUser = message.sender == ChatSender.USER
  val isSystem = message.sender == ChatSender.SYSTEM

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
  ) {
    if (isSystem) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 2.dp)
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.Top
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = message.text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      Surface(
        shape = RoundedCornerShape(
          topStart = 14.dp,
          topEnd = 14.dp,
          bottomStart = if (isUser) 14.dp else 2.dp,
          bottomEnd = if (isUser) 2.dp else 14.dp
        ),
        color = if (isUser) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier
          .fillMaxWidth(0.92f)
          .testTag("chat_bubble_${message.id}")
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (isUser) Icons.Default.Speed else Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isUser) "আপনি (Developer)" else "Genkit AI Assistant",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = message.timestamp,
                fontSize = 10.sp,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
              )
              if (!isUser && message.text.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                  onClick = { onCopy(message.text) },
                  modifier = Modifier.size(20.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy message",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = message.text.ifBlank { if (message.isStreaming) "টাইপ করা হচ্ছে..." else "" },
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
          )

          if (message.error != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.errorContainer
            ) {
              Text(
                text = "ত্রুটি: ${message.error}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(6.dp)
              )
            }
          }
        }
      }
    }
  }
}
