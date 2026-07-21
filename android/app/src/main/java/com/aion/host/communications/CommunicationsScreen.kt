package com.aion.host.communications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import java.text.DateFormat
import java.util.Date

/** T-152 (EPIC 17) — real recent calls + SMS (mockup screen #13, "Emails" tab not built: no
 * Gmail/IMAP integration exists yet, out of scope here). [resumeSignal] (same pattern as
 * SetupWizardScreen/CalendarScreen) re-reads after returning from the permission prompt. */
@Composable
fun CommunicationsScreen(
    resumeSignal: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val callReader = remember { CallLogReader(context) }
    val smsReader = remember { SmsReader(context) }
    var calls by remember { mutableStateOf(callReader.recentCalls()) }
    var messages by remember { mutableStateOf(smsReader.recentMessages()) }

    LaunchedEffect(resumeSignal) {
        calls = callReader.recentCalls()
        messages = smsReader.recentMessages()
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Communications", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)
        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            item {
                Text(
                    "Recent Calls",
                    style = MaterialTheme.typography.titleMedium,
                    color = AionColors.OnBackground,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            }
            item {
                GlassPanel {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        if (calls.isEmpty()) {
                            EmptyHint("No calls found, or Call Log access hasn't been granted yet (Setup screen).")
                        } else {
                            calls.forEach { call -> CallRow(call) }
                        }
                    }
                }
            }
            item {
                Text(
                    "Recent Messages",
                    style = MaterialTheme.typography.titleMedium,
                    color = AionColors.OnBackground,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
            }
            item {
                GlassPanel {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        if (messages.isEmpty()) {
                            EmptyHint("No messages found, or SMS access hasn't been granted yet (Setup screen).")
                        } else {
                            messages.forEach { sms -> SmsRow(sms) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = AionColors.OnSurfaceVariant)
}

@Composable
private fun CallRow(call: CallLogItem) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(call.displayName, style = MaterialTheme.typography.bodyLarge, color = AionColors.OnBackground)
            Text(
                call.direction.name.lowercase(),
                style = MaterialTheme.typography.bodySmall,
                color = AionColors.OnSurfaceVariant,
            )
        }
        Text(
            dateTimeFormat.format(Date(call.timestampMs)),
            style = MaterialTheme.typography.bodySmall,
            color = AionColors.OnSurfaceVariant,
        )
    }
    HorizontalDivider(color = AionColors.OutlineVariant)
}

@Composable
private fun SmsRow(sms: SmsItem) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(sms.address, style = MaterialTheme.typography.bodyLarge, color = AionColors.OnBackground)
            Text(
                sms.body.take(60),
                style = MaterialTheme.typography.bodySmall,
                color = AionColors.OnSurfaceVariant,
            )
        }
        Text(
            dateTimeFormat.format(Date(sms.timestampMs)),
            style = MaterialTheme.typography.bodySmall,
            color = AionColors.OnSurfaceVariant,
        )
    }
    HorizontalDivider(color = AionColors.OutlineVariant)
}

private val dateTimeFormat: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
