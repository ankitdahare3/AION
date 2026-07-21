package com.aion.host.devicestatus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.StatCard

/** T-153 (EPIC 17) — real device vitals (mockup #27 Performance, #35 Battery). Reads once per
 * composition rather than polling continuously (this screen isn't visible often enough to
 * justify a `LaunchedEffect` timer loop yet — add one if the mockup's live-updating dials matter
 * later, not speculatively now). */
@Composable
fun DeviceStatusScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(DeviceStatusReader(context).read()) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Device Status", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)

        StatCard(
            "Battery",
            "${status.batteryPercent}%" + if (status.charging) " · Charging" else "",
            modifier = Modifier.fillMaxWidth(),
        )
        StatCard(
            "RAM",
            "${formatGb(status.ramFreeBytes)} free of ${formatGb(status.ramTotalBytes)}",
            modifier = Modifier.fillMaxWidth(),
        )
        StatCard(
            "Storage",
            "${formatGb(status.storageFreeBytes)} free of ${formatGb(status.storageTotalBytes)}",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
