package com.aion.host

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel

/**
 * T-168 — the "Apps" tile of the bottom nav: a single scrollable list of every screen this import
 * pass added, real and illustrative alike, so MainActivity's own debug button row didn't have to
 * grow by another 18 entries. Real screens and illustrative-mockup screens are listed together,
 * undistinguished here — each screen's own KDoc/note explains which is which.
 */
@Composable
fun AppsHubScreen(
    destinations: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Apps", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)
        destinations.forEach { (label, onClick) ->
            GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AionColors.OnBackground,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
                )
            }
        }
    }
}
