package com.aion.host.mockup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import com.aion.host.ui.theme.IllustrativeScreen

/** Mockup "3D AI Holographic Mode" — owner explicitly deferred this feature. No 3D rendering pipeline exists. */
@Composable
fun HolographicModeScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "Holographic Mode",
        note = "Illustrative — the owner deferred this feature; no 3D avatar rendering exists.",
        modifier = modifier,
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth().height(220.dp).padding(top = 12.dp)) {
            Text(
                "3D avatar preview",
                style = MaterialTheme.typography.bodyMedium,
                color = AionColors.OnSurfaceVariant,
                modifier = Modifier.padding(top = 100.dp, start = 16.dp),
            )
        }
    }
}
