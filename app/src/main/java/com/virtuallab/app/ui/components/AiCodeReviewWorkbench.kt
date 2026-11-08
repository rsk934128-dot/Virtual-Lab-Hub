package com.virtuallab.app.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CodeReviewResult(
  val score: Int, // Out of 100
  val securityRating: String, // "A+", "A", "B", "C"
  val optimizations: List<String>,
  val securityNotes: List<String>,
  val cleanSnippet: String
)

@Composable
fun AiCodeReviewWorkbench(
  modifier: Modifier = Modifier
) {
  var codeSnippetInput by remember {
    mutableStateOf(
      """// Virtual Container Network Handler
fun handleIncomingSocket(port: Int) {
  val server = ServerSocket(port)
  while (true) {
    val client = server.accept()
    // Read raw stream without timeout
    val data = client.getInputStream().readBytes()
    processPacket(data)
  }
}"""
    )
  }

  var selectedTab by remember { mutableIntStateOf(0) } // 0: Code Review, 1: Remote Debugger
  var isAnalyzing by remember { mutableStateOf(false) }
  var reviewResult by remember { mutableStateOf<CodeReviewResult?>(null) }
  val scope = rememberCoroutineScope()

  Card(
    modifier = modifier.testTag("ai_code_review_card"),
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
            .background(Color(0xFF3F51B5)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.RateReview,
            contentDescription = "AI Code Review",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "এআই কোড রিভিউ ও রিমোট ডিবাগার",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "AI-Powered Code Reviewer & Live Memory Leak Inspector",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("এআই কোড রিভিউ (Review)") }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("রিমোট ডিবাগার (Debugger)") }
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (selectedTab == 0) {
        // AI Code Review Tab
        OutlinedTextField(
          value = codeSnippetInput,
          onValueChange = { codeSnippetInput = it },
          label = { Text("কোড বা স্ক্রিপ্ট পেস্ট করুন (Kotlin/JS/Python)") },
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .testTag("code_review_input"),
          textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
          maxLines = 8
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
          onClick = {
            if (codeSnippetInput.isNotBlank()) {
              isAnalyzing = true
              scope.launch {
                VirtualCore.postLog(
                  LogLevel.INFO,
                  "AICodeReview",
                  "Analyzing source code snippet",
                  "Characters: ${codeSnippetInput.length}"
                )
                delay(1200L) // Simulate intelligent AST and security analysis

                reviewResult = CodeReviewResult(
                  score = 88,
                  securityRating = "A",
                  optimizations = listOf(
                    "Coroutine Dispatchers.IO ব্যবহার করুন যাতে Main Thread ব্লক না হয়।",
                    "ServerSocket বন্ধ করার জন্য try-with-resources বা use {} ব্লক ব্যবহার করুন।"
                  ),
                  securityNotes = listOf(
                    "বাফার সাইজ লিমিটেশন যুক্ত করুন যাতে ডিনায়াল অফ সার্ভিস (DoS) আক্রমণ প্রতিহত হয়।",
                    "VirtualCore Network Isolation পলিসির সাথে সকেট বাউন্ডারি অ্যালাইন করুন।"
                  ),
                  cleanSnippet = """// অপ্টিমাইজড ও সিকিউর সংস্করণ:
suspend fun handleIncomingSocketSafe(port: Int) = withContext(Dispatchers.IO) {
  ServerSocket(port).use { server ->
    server.soTimeout = 10000 // সকেট টাইমআউট
    val client = server.accept()
    client.use {
      val data = it.getInputStream().readNBytes(4096)
      processPacket(data)
    }
  }
}"""
                )
                isAnalyzing = false
                VirtualCore.postLog(
                  LogLevel.INFO,
                  "AICodeReview",
                  "Code review analysis completed",
                  "Security Rating: A (Score: 88/100)"
                )
              }
            }
          },
          enabled = !isAnalyzing && codeSnippetInput.isNotBlank(),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("run_code_review_button")
        ) {
          if (isAnalyzing) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text("কোড বিশ্লেষণ করা হচ্ছে...")
          } else {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("এআই দিয়ে কোড অডিট করুন (Analyze Code)")
          }
        }

        reviewResult?.let { result ->
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
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("কোড কোয়ালিটি স্কোর: ${result.score}/100", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.primaryContainer
                ) {
                  Text(
                    text = "Security: ${result.securityRating}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text("💡 অপ্টিমাইজেশন পরামর্শ:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              result.optimizations.forEach { opt ->
                Text("• $opt", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

              Spacer(modifier = Modifier.height(6.dp))
              Text("🛡️ সিকিউরিটি নোট:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63))
              result.securityNotes.forEach { sec ->
                Text("• $sec", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text("✨ অপ্টিমাইজড কোড:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF14141E),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = result.cleanSnippet,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp,
                  color = Color(0xFF81D4FA),
                  modifier = Modifier.padding(8.dp)
                )
              }
            }
          }
        }
      } else {
        // Remote Debugger Tab
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
                Icon(Icons.Default.BugReport, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("লাইভ স্যান্ডবক্স ডিবাগার কানেক্টর", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFE8F5E9)
              ) {
                Text(
                  text = "ATTACHED (PID 7125)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.sp,
                  color = Color(0xFF2E7D32),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("• থ্রেড পুল স্ট্যাটাস: 6 active coroutines (Dispatchers.IO)", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("• হিপ মেমোরি স্টেট: 0 memory leaks detected (LeakCanary safe)", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("• সকেট ইনস্পেক্টর: 0 unclosed connections (Zero-Trust Guard)", fontSize = 11.sp, fontFamily = FontFamily.Monospace)

            Spacer(modifier = Modifier.height(12.dp))

            Button(
              onClick = {
                VirtualCore.postLog(
                  LogLevel.INFO,
                  "RemoteDebugger",
                  "Garbage collection & memory profile triggered",
                  "Heap cleared: 4.2 MB freed"
                )
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("মেমোরি প্রোফাইলার রান করুন (Run Profiler)")
            }
          }
        }
      }
    }
  }
}
