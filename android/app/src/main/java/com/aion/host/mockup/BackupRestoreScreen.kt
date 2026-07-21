package com.aion.host.mockup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.IllustrativeScreen
import com.aion.host.ui.theme.StatCard

/**
 * Mockup "Backup & Restore Center" — Google Drive backup is a real, scoped roadmap item (TASKS.md
 * EPIC 17) blocked on a real Google Cloud Console project + OAuth consent screen the owner has to
 * set up themselves — not something an agent can complete on their behalf. Static until that's done.
 */
@Composable
fun BackupRestoreScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "Backup & Restore",
        note = "Not yet built — Google Drive backup needs the owner's own Google Cloud Console + OAuth setup first.",
        modifier = modifier,
    ) {
        StatCard("Status", "Not configured", modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
    }
}
