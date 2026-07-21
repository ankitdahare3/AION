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

private val MOCK_TOOLS =
    listOf(
        "Script Writer" to "Generate ideas, scripts",
        "Image Generator" to "Create stunning images",
        "Video Generator" to "Create videos with AI",
        "Voice Clone" to "Clone your voice",
        "Audio Studio" to "Edit & enhance audio",
        "Subtitle Generator" to "Auto subtitles for video",
    )

/** Mockup "AI Media Studio" — owner explicitly deferred video/voice generation. No generation backend exists; tiles are non-interactive. */
@Composable
fun MediaStudioScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "AI Studio",
        note = "Illustrative — no image/video/voice generation backend is built; the owner deferred this feature.",
        modifier = modifier,
    ) {
        MOCK_TOOLS.forEach { (name, description) ->
            GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(name, style = MaterialTheme.typography.bodyLarge, color = AionColors.OnBackground)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = AionColors.OnSurfaceVariant)
                }
            }
        }
    }
}
