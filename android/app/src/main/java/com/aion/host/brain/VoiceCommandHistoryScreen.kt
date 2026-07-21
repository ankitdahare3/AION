package com.aion.host.brain

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel

/**
 * Mockup "Voice Command History" — real past runs from [RoomCheckpointer]'s own `graph_checkpoint`
 * table (T-053's own "snapshot after every step, for debugging/audit" table), filtered to each
 * run's terminal checkpoint. Nothing was previously surfacing this to the UI; it's real, not
 * fabricated — the only new code here is the read query, not new tracking.
 */
@Composable
fun VoiceCommandHistoryScreen(
    checkpointer: RoomCheckpointer,
    resumeSignal: Int,
    modifier: Modifier = Modifier,
) {
    var runs by remember { mutableStateOf(emptyList<GraphCheckpointEntity>()) }
    LaunchedEffect(resumeSignal) { runs = checkpointer.recentCompletedRuns() }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Voice Command History", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)
        if (runs.isEmpty()) {
            Text(
                "Nothing run yet — goals given via the Chat screen will show up here.",
                style = MaterialTheme.typography.bodyMedium,
                color = AionColors.OnSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            GlassPanel(modifier = Modifier.padding(top = 16.dp)) {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(runs) { run -> RunRow(run) }
                }
            }
        }
    }
}

@Composable
private fun RunRow(run: GraphCheckpointEntity) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(run.goal, style = MaterialTheme.typography.bodyLarge, color = AionColors.OnBackground)
        Text(
            run.response ?: "(no response)",
            style = MaterialTheme.typography.bodySmall,
            color = AionColors.OnSurfaceVariant,
        )
    }
    HorizontalDivider(color = AionColors.OutlineVariant)
}
