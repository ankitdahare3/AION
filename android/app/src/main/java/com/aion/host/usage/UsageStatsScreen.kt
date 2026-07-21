package com.aion.host.usage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

/** T-154 (EPIC 17) — real today's screen-time breakdown (mockup #36). [resumeSignal] (same
 * pattern as CalendarScreen/CommunicationsScreen) re-reads after returning from the special-access
 * Settings screen where USAGE_ACCESS is granted. */
@Composable
fun UsageStatsScreen(
    resumeSignal: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reader = remember { UsageStatsReader(context) }
    var usage by remember { mutableStateOf(reader.todayUsage()) }

    LaunchedEffect(resumeSignal) {
        usage = reader.todayUsage()
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("App Usage Today", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)
        val total = usage.sumOf { it.foregroundMs }
        Text(
            "Screen time: ${formatDuration(total)}",
            style = MaterialTheme.typography.bodyMedium,
            color = AionColors.OnSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        if (usage.isEmpty()) {
            Text(
                "No usage data yet, or Usage Access hasn't been granted (Setup screen).",
                style = MaterialTheme.typography.bodyMedium,
                color = AionColors.OnSurfaceVariant,
            )
        } else {
            LazyColumn {
                items(usage) { app -> AppUsageRow(app) }
            }
        }
    }
}

@Composable
private fun AppUsageRow(app: AppUsage) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(
            app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = AionColors.OnBackground,
            modifier = Modifier.weight(1f),
        )
        Text(
            formatDuration(app.foregroundMs),
            style = MaterialTheme.typography.bodyMedium,
            color = AionColors.OnSurfaceVariant,
        )
    }
    HorizontalDivider()
}
