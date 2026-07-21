package com.aion.host.mockup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import com.aion.host.ui.theme.IllustrativeScreen

/**
 * Mockup "Camera Vision Mode" / "Screen Understanding" — real screen-reading already exists
 * (`AionAccessibilityService.currentScreenText()`, used by the live automation graph), but there's
 * no dedicated "point the camera at something and ask AION about it" flow or UI yet — that would
 * need a real CameraX + ML Kit object-recognition pipeline, not built.
 */
@Composable
fun VisionModeScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "Vision Mode",
        note =
            "Illustrative — no camera-based vision pipeline is built yet; screen-reading (a " +
                "different, real feature) already works via automation goals.",
        modifier = modifier,
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Camera preview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AionColors.OnSurfaceVariant,
                    modifier = Modifier.padding(top = 88.dp),
                )
            }
        }
    }
}
