package com.aion.host.calendar

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
import com.aion.host.ui.theme.GlassPanel
import java.text.DateFormat
import java.util.Date

/** T-151 (EPIC 17) — today's real events, read via [CalendarReader]. [resumeSignal] (same
 * pattern as SetupWizardScreen) re-reads after returning from the permission prompt so a
 * just-granted permission shows real events without needing to navigate away and back. */
@Composable
fun CalendarScreen(
    resumeSignal: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reader = remember { CalendarReader(context) }
    var events by remember { mutableStateOf(reader.todayEvents()) }

    LaunchedEffect(resumeSignal) {
        events = reader.todayEvents()
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Today", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)
        if (events.isEmpty()) {
            Text(
                "No events found — either your day is free, or Calendar access hasn't been granted yet (Setup screen).",
                style = MaterialTheme.typography.bodyMedium,
                color = AionColors.OnSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            GlassPanel(modifier = Modifier.padding(top = 16.dp)) {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(events) { event -> EventRow(event) }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            if (event.allDay) "All day" else timeFormat.format(Date(event.startMs)),
            style = MaterialTheme.typography.labelLarge,
            color = AionColors.Glow,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(event.title, style = MaterialTheme.typography.bodyLarge, color = AionColors.OnBackground)
    }
    HorizontalDivider(color = AionColors.OutlineVariant)
}

private val timeFormat: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
