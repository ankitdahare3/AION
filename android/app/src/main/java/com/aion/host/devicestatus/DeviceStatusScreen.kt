package com.aion.host.devicestatus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors

/** T-153 (EPIC 17) — real device vitals (mockup #27 Performance, #35 Battery). Reads once per
 * composition rather than polling continuously (this screen isn't visible often enough to
 * justify a `LaunchedEffect` timer loop yet — add one if the mockup's live-updating dials matter
 * later, not speculatively now). */
@Composable
fun DeviceStatusScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(DeviceStatusReader(context).read()) }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Device Status", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)

        StatCard("Battery", "${status.batteryPercent}%" + if (status.charging) " · Charging" else "")
        StatCard("RAM", "${formatGb(status.ramFreeBytes)} free of ${formatGb(status.ramTotalBytes)}")
        StatCard("Storage", "${formatGb(status.storageFreeBytes)} free of ${formatGb(status.storageTotalBytes)}")
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AionColors.Surface)
                .padding(16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = AionColors.OnSurfaceMuted)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = AionColors.OnBackground)
    }
}
