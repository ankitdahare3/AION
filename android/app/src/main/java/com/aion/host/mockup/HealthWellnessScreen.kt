package com.aion.host.mockup

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.IllustrativeScreen
import com.aion.host.ui.theme.StatCard

/**
 * Mockup "Health & Wellness Overview" — no Health Connect / Google Fit integration exists, so
 * steps/heart-rate/sleep have no real source. Shown for visual reference only.
 */
@Composable
fun HealthWellnessScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "Health & Wellness",
        note = "Illustrative — no Health Connect / fitness-tracker integration is built; these numbers aren't real.",
        modifier = modifier,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            StatCard("Steps", "8,452 / 10,000", modifier = Modifier.weight(1f).padding(end = 6.dp))
            StatCard("Heart rate", "72 bpm", modifier = Modifier.weight(1f).padding(start = 6.dp))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            StatCard("Sleep", "7h 45m", modifier = Modifier.weight(1f).padding(end = 6.dp))
            StatCard("Stress", "Low", modifier = Modifier.weight(1f).padding(start = 6.dp))
        }
    }
}
