package com.aion.host.mockup

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import com.aion.host.ui.theme.IllustrativeScreen

private val MOCK_DEVICES =
    listOf("Living Room Lights", "Bedroom AC", "Living Room TV", "Bedroom Fan", "Curtains", "Door Lock")

/** Mockup "Smart Home Control" — owner explicitly deferred this feature (no smart-home integration built). Local toggle state only. */
@Composable
fun SmartHomeScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "Smart Home",
        note = "Illustrative — no smart-home integration is built; the owner deferred this feature.",
        modifier = modifier,
    ) {
        MOCK_DEVICES.forEach { device ->
            var on by remember { mutableStateOf(false) }
            GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        device,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AionColors.OnBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = on, onCheckedChange = { on = it })
                }
            }
        }
    }
}
