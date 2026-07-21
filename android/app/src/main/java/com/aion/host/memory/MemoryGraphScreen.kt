package com.aion.host.memory

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.brain.Memory
import com.aion.brain.MemoryKind
import com.aion.brain.MemoryStore
import com.aion.host.ui.theme.IllustrativeScreen
import com.aion.host.ui.theme.StatCard

/**
 * Mockup "Memory & Knowledge Graph" — the literal node-graph visualization (People/Projects/
 * Skills/Ideas as connected bubbles) is decorative; AION's real memory store has no such graph
 * structure. The counts themselves ARE real though: same [MemoryStore] every other memory screen
 * reads, just aggregated by [MemoryKind] instead of a per-row timeline.
 */
@Composable
fun MemoryGraphScreen(
    memoryStore: MemoryStore,
    resumeSignal: Int,
    modifier: Modifier = Modifier,
) {
    var memories by remember { mutableStateOf(emptyList<Memory>()) }
    LaunchedEffect(resumeSignal) { memories = memoryStore.getAllActive() }

    IllustrativeScreen(
        title = "Memory Graph",
        note = "The counts below are real; the connected-node visualization mockups show is decorative.",
        modifier = modifier,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            StatCard(
                "Facts",
                "${memories.count { it.kind == MemoryKind.FACT }}",
                modifier = Modifier.weight(1f).padding(end = 6.dp),
            )
            StatCard(
                "Preferences",
                "${memories.count { it.kind == MemoryKind.PREF }}",
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            )
            StatCard(
                "Profile",
                "${memories.count { it.kind == MemoryKind.PROFILE }}",
                modifier = Modifier.weight(1f).padding(start = 6.dp),
            )
        }
        StatCard("Total memories", "${memories.size}", modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
    }
}
