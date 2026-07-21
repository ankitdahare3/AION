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
 * Mockup "Shortcuts & Voice Commands" — no custom-shortcut engine exists; every goal already goes
 * through the one real path (type or speak it on the Chat screen), there's no separate saved-
 * shortcuts feature to manage.
 */
@Composable
fun ShortcutsScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "Shortcuts",
        note =
            "Illustrative — no saved-shortcuts feature exists; every goal already goes through " +
                "the real Chat screen.",
        modifier = modifier,
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(
                "Talk to AION directly on the Chat screen instead — typed or spoken goals both work today.",
                style = MaterialTheme.typography.bodyMedium,
                color = AionColors.OnSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
