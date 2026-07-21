package com.aion.host.mockup

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

/**
 * Mockup "AI Orb Overlay (Global Access)" — a persistent floating launcher over every app,
 * always-on-top. `KillSwitchOverlayService` is the one real `SYSTEM_ALERT_WINDOW` overlay this app
 * has; a second, always-visible quick-actions orb overlay isn't built.
 */
@Composable
fun OrbOverlayScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "Global Orb Overlay",
        note = "Illustrative — no persistent floating launcher overlay is built yet.",
        modifier = modifier,
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(
                "Quick Speak · Analyze Screen · Take Photo · Open AION",
                style = MaterialTheme.typography.bodyMedium,
                color = AionColors.OnBackground,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
