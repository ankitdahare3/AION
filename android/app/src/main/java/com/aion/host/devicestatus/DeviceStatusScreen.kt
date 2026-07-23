package com.aion.host.devicestatus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrowserUpdated
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import com.aion.host.ui.theme.MockupScaffold
import kotlinx.coroutines.delay

@Composable
fun DeviceStatusScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(DeviceStatusReader(context).read()) }

    // Polling simulation for UI
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            status = DeviceStatusReader(context).read()
        }
    }

    val ramUsed = (status.ramTotalBytes - status.ramFreeBytes).toFloat()
    val ramPercent = if (status.ramTotalBytes > 0) (ramUsed / status.ramTotalBytes * 100).toInt() else 0

    val storageUsed = (status.storageTotalBytes - status.storageFreeBytes).toFloat()
    val storagePercent = if (status.storageTotalBytes > 0) (storageUsed / status.storageTotalBytes * 100).toInt() else 0

    MockupScaffold(
        title = "System Performance",
        onBack = onBack,
        trailingIcon = Icons.Filled.Settings,
        onTrailingClick = { /* Settings */ },
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
        ) {
            // Live Status Indicator in Top Bar is mockable, but we just show it below Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, bottom = 24.dp),
            ) {
                Box(modifier = Modifier.size(6.dp).background(AionColors.PrimaryContainer, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(
                    "LIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AionColors.Primary,
                    letterSpacing = 2.sp,
                )
            }

            // Top Gauges Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GaugeItem(value = 58, label = "CPU", color = AionColors.PrimaryContainer)
                GaugeItem(value = ramPercent, label = "RAM", color = AionColors.Primary)
                GaugeItem(value = status.batteryPercent, label = "BATT", color = AionColors.TertiaryContainer)

                // Storage Gauge uses a different style in HTML (icon instead of percentage text if desired, but we keep GaugeItem logic)
                GaugeItem(value = storagePercent, label = "SSD", color = AionColors.PrimaryContainer)
            }

            Spacer(Modifier.height(32.dp))

            // Network Section
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column {
                            Text(
                                "NETWORK BANDWIDTH",
                                style = MaterialTheme.typography.labelSmall,
                                color = AionColors.OnSurfaceVariant,
                                letterSpacing = 1.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Column {
                                    Text(
                                        "DOWNLOAD",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AionColors.OnSurfaceVariant,
                                        fontSize = 10.sp,
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            "42.6",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = AionColors.Primary,
                                        )
                                        Text(
                                            " Mbps",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AionColors.Primary,
                                            modifier = Modifier.padding(bottom = 4.dp),
                                        )
                                    }
                                }
                                Box(
                                    modifier =
                                        Modifier
                                            .width(
                                                1.dp,
                                            ).height(32.dp)
                                            .background(AionColors.OutlineVariant.copy(alpha = 0.3f)),
                                )
                                Column {
                                    Text(
                                        "UPLOAD",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AionColors.OnSurfaceVariant,
                                        fontSize = 10.sp,
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            "18.3",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = AionColors.Primary,
                                        )
                                        Text(
                                            " Mbps",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AionColors.Primary,
                                            modifier = Modifier.padding(bottom = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                        Icon(Icons.Filled.Podcasts, contentDescription = null, tint = AionColors.PrimaryContainer)
                    }

                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, AionColors.OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .background(AionColors.SurfaceContainerLowest),
                    ) {
                        Box(
                            modifier =
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, AionColors.Background.copy(alpha = 0.4f)),
                                    ),
                                ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Active Processes
            Text(
                "ACTIVE PROCESSES",
                style = MaterialTheme.typography.labelSmall,
                color = AionColors.OnSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProcessRow(Icons.Filled.BrowserUpdated, "Chrome", "12%", AionColors.Primary)
                ProcessRow(Icons.Filled.SettingsInputComponent, "System UI", "8%", AionColors.Primary)
                ProcessRow(Icons.Filled.Bolt, "AION Core", "5%", AionColors.PrimaryContainer, glow = true)
                ProcessRow(Icons.Filled.MoreHoriz, "Others", "7%", AionColors.OnSurfaceVariant)
            }

            Spacer(Modifier.height(40.dp))

            // Boost Button
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AionColors.PrimaryContainer, AionColors.SecondaryContainer),
                            ),
                        ).clickable { }
                        .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.RocketLaunch, contentDescription = null, tint = AionColors.OnPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Boost Performance", style = MaterialTheme.typography.titleMedium, color = AionColors.OnPrimary)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ProcessRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    usage: String,
    color: Color,
    glow: Boolean = false,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        glow = glow,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .background(
                                if (glow) color.copy(alpha = 0.2f) else AionColors.SurfaceContainerHighest,
                                CircleShape,
                            ).border(
                                1.dp,
                                if (glow) color.copy(alpha = 0.5f) else AionColors.OutlineVariant.copy(alpha = 0.3f),
                                CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = color)
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (glow) color else AionColors.OnSurface,
                    fontWeight = if (glow) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            Text(usage, style = MaterialTheme.typography.headlineSmall, color = color)
        }
    }
}

@Composable
private fun GaugeItem(
    value: Int,
    label: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = 270f * (value / 100f),
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Text("$value%", style = MaterialTheme.typography.labelSmall, color = color, fontSize = 10.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = AionColors.OnSurfaceVariant, fontSize = 10.sp)
    }
}
