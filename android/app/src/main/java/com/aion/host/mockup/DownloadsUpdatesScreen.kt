package com.aion.host.mockup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.IllustrativeScreen
import com.aion.host.ui.theme.StatCard

/**
 * Mockup "Downloads & Updates" — AION isn't a system OS/launcher, so it has no real download
 * manager or OTA update mechanism of its own; app updates happen the normal Android way (Play
 * Store / sideloaded APK), not through an in-app screen.
 */
@Composable
fun DownloadsUpdatesScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "Downloads & Updates",
        note =
            "Illustrative — AION has no own download manager or update mechanism; app updates " +
                "happen the normal Android way.",
        modifier = modifier,
    ) {
        StatCard("Current version", "See the About screen", modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
    }
}
