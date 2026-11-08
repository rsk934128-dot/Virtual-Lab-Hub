package com.virtuallab.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

data class CommandHistoryEntry(
  val id: String = java.util.UUID.randomUUID().toString(),
  val command: String,
  val output: String,
  val aiSuggestion: String? = null,
  val isCachedLocally: Boolean = true
)

@Composable
fun AiNativeTerminalWorkbench(
  modifier: Modifier = Modifier
) {
  var commandInput by remember { mutableStateOf("") }
  var autoScaleSleepMode by remember { mutableStateOf(true) }
  var redisPersistenceEnabled by remember { mutableStateOf(true) }

  var terminalEntries by remember {
    mutableStateOf(
      listOf(
        CommandHistoryEntry(
          command = "docker ps -a",
          output = "CONTAINER ID   IMAGE                 STATUS         PORTS\n7f81a02e      virtuallab/node-lab   Up 42 minutes  3000->3000\n3b91c49f      virtuallab/python3    Exited (0)     --",
          aiSuggestion = "💡 পরামর্শ: 3b91c49f কন্টেইনারটি স্লিপ মোডে আছে। পুনরায় স্টার্ট করতে 'docker start 3b91c49f' চালান।"
        ),
        CommandHistoryEntry(
          command = "apt-get upgdare",
          output = "bash: apt-get: upgdare: command not found",
          aiSuggestion = "⚡ এআই কারেকশন: আপনার টাইপিংয়ে ভুল হয়েছে। আপনি সম্ভবত 'sudo apt update && sudo apt upgrade' চালাতে চেয়েছিলেন।"
        )
      )
    )
  }

  Card(
    modifier = modifier.testTag("ai_native_terminal_card"),
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
              .size(40.dp)
              .clip(CircleShape)
              .background(Color(0xFF673AB7)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = "AI-Native Terminal",
              tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "এআই-নেটিভ টার্মিনাল ও অটো-স্কেলিং",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Smart CLI Assistant • Redis Persistence • Knative Sleep",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
        }

        IconButton(
          onClick = { terminalEntries = emptyList() },
          modifier = Modifier.testTag("clear_terminal_history")
        ) {
          Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.outline)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 3 Special Engineering Feature Badges
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Feature 1: AI Native
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(4.dp))
              Text("AI CLI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text("স্মার্ট সাজেশন ও অটো-ফিক্স", fontSize = 9.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
          }
        }

        // Feature 2: Auto-Scaling Sleep Mode
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.secondaryContainer,
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Auto-Scale", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(if (autoScaleSleepMode) "স্লিপ মোড অ্যাক্টিভ" else "স্ট্যান্ডবাই মোড", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
          }
        }

        // Feature 3: Smart Persistence
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.tertiaryContainer,
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Persistence", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(if (redisPersistenceEnabled) "Redis সেভ সক্রিয়" else "মেমোরি ড্রপ", fontSize = 9.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Terminal Output Window
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(230.dp)
          .clip(RoundedCornerShape(12.dp)),
        color = Color(0xFF14141E)
      ) {
        if (terminalEntries.isEmpty()) {
          Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
              text = "কমান্ড লিখুন (যেমন: docker ps, git status, npm run dev, apt update)...",
              color = Color.Gray,
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(terminalEntries, key = { it.id }) { item ->
              Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = "$ ${item.command}",
                  fontFamily = FontFamily.Monospace,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF81D4FA)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = item.output,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 11.sp,
                  color = Color(0xFFE0E0E0),
                  lineHeight = 15.sp
                )

                if (item.aiSuggestion != null) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF261E3E),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = item.aiSuggestion,
                      fontFamily = FontFamily.Monospace,
                      fontSize = 11.sp,
                      color = Color(0xFFD1C4E9),
                      modifier = Modifier.padding(8.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Command Execution Line
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = commandInput,
          onValueChange = { commandInput = it },
          label = { Text("টার্মিনাল কমান্ড (Command)") },
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
          modifier = Modifier
            .weight(1f)
            .testTag("ai_terminal_input")
        )

        Button(
          onClick = {
            val cmd = commandInput.trim()
            if (cmd.isNotBlank()) {
              val (output, suggestion) = evaluateCommandWithAi(cmd)
              terminalEntries = terminalEntries + CommandHistoryEntry(
                command = cmd,
                output = output,
                aiSuggestion = suggestion
              )
              VirtualCore.postLog(LogLevel.INFO, "AITerminal", "Executed command: $cmd", suggestion)
              commandInput = ""
            }
          },
          enabled = commandInput.isNotBlank(),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("ai_terminal_run_button")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Run")
        }
      }
    }
  }
}

private fun evaluateCommandWithAi(cmd: String): Pair<String, String?> {
  val lower = cmd.lowercase()
  return when {
    lower.contains("docker ps") -> {
      "CONTAINER ID   IMAGE                 STATUS         PORTS\n7f81a02e      virtuallab/node-lab   Up 48 minutes  3000->3000" to
        "💡 এআই পর্যবেক্ষণ: আপনার ভার্চুয়াল ল্যাবের নোড কনটেইনার পোর্ট ৩০০০-এ নিরাপদে লাইভ চলছে।"
    }
    lower.contains("docker stop") -> {
      "7f81a02e stopped successfully." to
        "⚡ অটো-স্কেলিং নোটিশ: কন্টেইনার স্লিপ মোডে চলে গেছে। সিপিইউ ও র‍্যাম রিসোর্স ফ্রি করা হয়েছে।"
    }
    lower.contains("git status") -> {
      "On branch main\nYour branch is up to date with 'origin/main'.\nnothing to commit, working tree clean" to
        "💡 গিটহাব পারসিস্টেন্স: সমস্ত পরিবর্তন সফলভাবে গিটহাবে সিঙ্ক করা আছে।"
    }
    lower.contains("npm run dev") || lower.contains("yarn dev") -> {
      "> dev\n> vite\n\n  VITE v6.2.3  ready in 184 ms\n  ➜  Local:   http://localhost:5173/\n  ➜  Network: use --host to expose" to
        "🚀 লোকালহোস্ট ৫১৭৩ পোর্টে সার্ভার রেডি। PWABuilder অথবা ভারসেল দিয়ে সরাসরি লাইভ করা যাবে।"
    }
    lower.startsWith("apt") || lower.startsWith("sudo apt") -> {
      "Reading package lists... Done\nBuilding dependency tree... Done\nAll packages are up to date." to
        "✅ প্যাকেজ রিপোজিটরি আপডেটেড। কোনো প্যাকেজ রিমুভাল প্রয়োজন নেই।"
    }
    else -> {
      "Process finished with exit code 0 for: $cmd" to
        "🤖 এআই অ্যাসিস্ট্যান্ট: কমান্ডটি ভার্চুয়াল স্যান্ডবক্স শেল এনভায়রনমেন্টে সফলভাবে সমাপ্ত হয়েছে।"
    }
  }
}
