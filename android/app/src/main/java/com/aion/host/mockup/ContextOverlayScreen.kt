package com.aion.host.mockup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import com.aion.host.ui.theme.IllustrativeScreen

private val MOCK_ACTIONS =
    listOf(
        "Summarize this video",
        "Translate to Hindi",
        "Explain in simple words",
        "Create notes",
        "Find related videos",
        "Save key points",
    )

/**
 * Mockup "Context Aware Overlay (Any App)" — a system-wide floating overlay that offers
 * contextual actions over whatever app is in front. `KillSwitchOverlayService` proves
 * `SYSTEM_ALERT_WINDOW` overlays are technically possible in this app, but no such context-aware
 * overlay is built — it would need its own accessibility-driven content classifier, not built.
 */
@Composable
fun ContextOverlayScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "Context Overlay",
        note = "Illustrative — no system-wide contextual overlay is built yet; only the kill-switch overlay is real.",
        modifier = modifier,
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AION", style = MaterialTheme.typography.labelLarge, color = AionColors.Glow)
                Text(
                    "I can help you with this video.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AionColors.OnBackground,
                )
                MOCK_ACTIONS.forEach { action ->
                    Text(
                        action,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AionColors.OnSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
