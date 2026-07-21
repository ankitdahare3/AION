package com.aion.host.mockup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.IllustrativeScreen
import com.aion.host.ui.theme.StatCard

/**
 * Mockup "Travel Mode" — the owner's own scoped decision (T-158) was to read the real booking
 * app's screen via the existing accessibility engine rather than build a dedicated travel screen —
 * that path needs no new UI at all, just a real automation goal like "what's my flight status".
 * This mockup screen has no real data behind it (no flight/hotel API), shown for visual reference only.
 */
@Composable
fun TravelModeScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "Travel Mode",
        note =
            "Illustrative — flight/hotel status is answered by asking AION to read the real booking " +
                "app's screen, not this static mockup.",
        modifier = modifier,
    ) {
        StatCard(
            "Weather",
            "Check your destination's real weather via the Weather screen",
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        StatCard(
            "Flight status",
            "Ask AION: \"what's my flight status\"",
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}
