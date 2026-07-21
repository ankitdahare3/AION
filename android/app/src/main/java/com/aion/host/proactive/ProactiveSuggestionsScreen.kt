package com.aion.host.proactive

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aion.host.calendar.CalendarReader
import com.aion.host.devicestatus.DeviceStatusReader
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import com.aion.host.usage.UsageStatsReader

/** T-155 (EPIC 17, mockup #11) — wires the three real readers into [ProactiveSuggestionEngine].
 * [resumeSignal] (same pattern as every other real-data screen here) re-evaluates on resume so a
 * just-granted permission or a newly-crossed threshold shows up without navigating away and back. */
@Composable
fun ProactiveSuggestionsScreen(
    resumeSignal: Int,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var suggestions by remember { mutableStateOf(emptyList<ProactiveSuggestion>()) }

    LaunchedEffect(resumeSignal) {
        val status = DeviceStatusReader(context).read()
        val nextEvent = CalendarReader(context).todayEvents().firstOrNull { it.startMs >= System.currentTimeMillis() }
        val screenTimeMs = UsageStatsReader(context).todayUsage().sumOf { it.foregroundMs }
        suggestions =
            ProactiveSuggestionEngine.evaluate(
                batteryPercent = status.batteryPercent,
                charging = status.charging,
                nowMs = System.currentTimeMillis(),
                nextEventStartMs = nextEvent?.startMs,
                nextEventTitle = nextEvent?.title,
                screenTimeTodayMs = screenTimeMs,
            )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Suggestions", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)
        if (suggestions.isEmpty()) {
            Text(
                "Nothing to flag right now — battery's fine, no event coming up, no long streak on screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = AionColors.OnSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            suggestions.forEach { suggestion ->
                SuggestionCard(suggestion, onOpenCalendar, context)
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: ProactiveSuggestion,
    onOpenCalendar: () -> Unit,
    context: android.content.Context,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(suggestion.message, style = MaterialTheme.typography.bodyLarge, color = AionColors.OnBackground)
            // TAKE_A_BREAK has no real backend action to offer (no reminder-scheduling system
            // exists yet) — showing it without a fake "Remind me" button that does nothing is the
            // honest call.
            when (suggestion.kind) {
                SuggestionKind.BATTERY_LOW ->
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        TextButton(
                            onClick = { context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) },
                        ) {
                            Text("Open Battery Settings")
                        }
                    }
                SuggestionKind.UPCOMING_EVENT ->
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        TextButton(onClick = onOpenCalendar) { Text("View Calendar") }
                    }
                SuggestionKind.TAKE_A_BREAK -> {}
            }
        }
    }
}
