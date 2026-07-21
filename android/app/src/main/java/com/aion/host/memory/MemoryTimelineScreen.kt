package com.aion.host.memory

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
import com.aion.brain.Memory
import com.aion.brain.MemoryStore
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel

/**
 * Mockup "Memory Timeline" screen — every row is a real [Memory] row this app itself already
 * wrote (via `DeviceExplorer`'s "Explore Device" scan, `MemoryConsolidator`, etc.), newest first.
 * [resumeSignal] (same pattern as every other real-data screen) re-reads on resume.
 */
@Composable
fun MemoryTimelineScreen(
    memoryStore: MemoryStore,
    resumeSignal: Int,
    modifier: Modifier = Modifier,
) {
    var memories by remember { mutableStateOf(emptyList<Memory>()) }

    LaunchedEffect(resumeSignal) {
        memories = memoryStore.getAllActive().sortedByDescending { it.created }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Memory Timeline", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)
        if (memories.isEmpty()) {
            Text(
                "Nothing remembered yet — run \"Explore Device\" or use AION for a while to build this up.",
                style = MaterialTheme.typography.bodyMedium,
                color = AionColors.OnSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            GlassPanel(modifier = Modifier.padding(top = 16.dp)) {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(memories) { memory -> MemoryRow(memory) }
                }
            }
        }
    }
}

@Composable
private fun MemoryRow(memory: Memory) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(
            memory.kind.name,
            style = MaterialTheme.typography.labelMedium,
            color = AionColors.Glow,
        )
        Text(memory.text, style = MaterialTheme.typography.bodyMedium, color = AionColors.OnBackground)
        Text(
            relativeTime(memory.created),
            style = MaterialTheme.typography.bodySmall,
            color = AionColors.OnSurfaceVariant,
        )
    }
    HorizontalDivider(color = AionColors.OutlineVariant)
}

private fun relativeTime(ts: Long): String {
    val diffMs = System.currentTimeMillis() - ts
    val minutes = diffMs / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 24 * 60 -> "${minutes / 60}h ago"
        else -> "${minutes / (24 * 60)}d ago"
    }
}
