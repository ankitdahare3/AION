package com.aion.host.notifications

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
import com.aion.host.svc.NotificationIngestion
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel

/**
 * Mockup "Notifications Intelligent View" — real ingested notifications
 * ([AionNotificationListener][com.aion.host.svc.AionNotificationListener] already writes these as
 * `Memory` rows), filtered to [NotificationIngestion.PROVENANCE] so this doesn't also show
 * PROFILE/exploration memories. "Intelligent" here just means honest: no summarization/priority
 * ranking is built, so none is claimed — this is a real, unfiltered read of what was ingested.
 */
@Composable
fun NotificationsScreen(
    memoryStore: MemoryStore,
    resumeSignal: Int,
    modifier: Modifier = Modifier,
) {
    var notifications by remember { mutableStateOf(emptyList<Memory>()) }

    LaunchedEffect(resumeSignal) {
        notifications =
            memoryStore
                .getAllActive()
                .filter { it.provenance == NotificationIngestion.PROVENANCE }
                .sortedByDescending { it.created }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Notifications", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)
        if (notifications.isEmpty()) {
            Text(
                "Nothing ingested yet — the notification-access permission needs to be granted (Setup screen).",
                style = MaterialTheme.typography.bodyMedium,
                color = AionColors.OnSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            GlassPanel(modifier = Modifier.padding(top = 16.dp)) {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(notifications) { entry -> NotificationRow(entry) }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(entry: Memory) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(entry.text, style = MaterialTheme.typography.bodyMedium, color = AionColors.OnBackground)
    }
    HorizontalDivider(color = AionColors.OutlineVariant)
}
