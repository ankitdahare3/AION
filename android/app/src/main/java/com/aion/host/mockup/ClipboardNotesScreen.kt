package com.aion.host.mockup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.IllustrativeScreen
import com.aion.host.ui.theme.StatCard

/** Mockup "Clipboard & Notes Manager" — no clipboard-monitoring or notes feature exists in AION. */
@Composable
fun ClipboardNotesScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "Clipboard & Notes",
        note = "Illustrative — no clipboard-monitoring or notes feature is built.",
        modifier = modifier,
    ) {
        StatCard("Saved items", "0", modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
    }
}
