package com.aion.host.usage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import com.aion.host.ui.theme.MockupScaffold

@Composable
fun UsageStatsScreen(
    resumeSignal: Int,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val reader = remember { UsageStatsReader(context) }
    var usage by remember { mutableStateOf(reader.todayUsage()) }
    var selectedTab by remember { mutableStateOf("Usage") }

    LaunchedEffect(resumeSignal) {
        usage = reader.todayUsage()
    }

    MockupScaffold(
        title = "AION Analytics",
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
            // Tab Navigation
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .background(AionColors.SurfaceContainerLow, RoundedCornerShape(24.dp))
                        .padding(4.dp),
            ) {
                val tabs = listOf("Usage", "Apps", "Limits")
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) AionColors.SurfaceContainer else Color.Transparent)
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            tab,
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (isSelected) {
                                    AionColors.Primary
                                } else {
                                    AionColors.OnSurfaceVariant.copy(
                                        alpha = 0.6f,
                                    )
                                },
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Screen Time Card
            val totalMs = usage.sumOf { it.foregroundMs }
            val totalStr = formatDuration(totalMs)

            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                Brush.horizontalGradient(listOf(AionColors.Primary, AionColors.SecondaryContainer)),
                            ),
                )
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column {
                            Text(
                                "SCREEN TIME",
                                style = MaterialTheme.typography.labelSmall,
                                color = AionColors.OnSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    if (totalMs >
                                        0
                                    ) {
                                        totalStr
                                    } else {
                                        "5h 42m"
                                    },
                                    style = MaterialTheme.typography.displayMedium,
                                    color = AionColors.Primary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AionColors.OnSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp),
                                )
                            }
                        }
                        Box(
                            modifier =
                                Modifier
                                    .background(
                                        AionColors.Primary.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp),
                                    ).padding(8.dp),
                        ) {
                            Icon(Icons.Filled.Insights, contentDescription = null, tint = AionColors.Primary)
                        }
                    }

                    // Bar Chart
                    Spacer(Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        BarColumn(height = 48.dp, label = "12 AM", active = false)
                        BarColumn(height = 96.dp, label = "6 AM", active = true)
                        BarColumn(height = 128.dp, label = "12 PM", active = true)
                        BarColumn(height = 112.dp, label = "6 PM", active = true)
                        BarColumn(height = 64.dp, label = "12 AM", active = false)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Most Used Apps Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Most Used Apps", style = MaterialTheme.typography.headlineMedium, color = AionColors.OnSurface)
                Text(
                    "VIEW ALL",
                    style = MaterialTheme.typography.labelSmall,
                    color = AionColors.Primary,
                    modifier =
                        Modifier
                            .clickable {
                            },
                )
            }

            Spacer(Modifier.height(16.dp))

            if (usage.isEmpty()) {
                // If no real data, show mock data
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MockAppUsageRow("YouTube", "2h 15m", 65, Color(0xFFFF0000), Icons.Filled.Smartphone)
                    MockAppUsageRow("Chrome", "1h 10m", 40, AionColors.Primary, Icons.Filled.Smartphone)
                    MockAppUsageRow("WhatsApp", "45m", 25, Color(0xFF22C55E), Icons.Filled.Smartphone)
                    MockAppUsageRow("Instagram", "30m", 15, Color(0xFFEC4899), Icons.Filled.Smartphone)
                }
            } else {
                usage.take(5).forEach { app ->
                    AppUsageRow(app = app, totalMs = totalMs.coerceAtLeast(1L))
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action Button
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, AionColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable { }
                        .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = AionColors.Primary)
                Spacer(Modifier.width(8.dp))
                Text("Set Usage Limits", style = MaterialTheme.typography.titleMedium, color = AionColors.Primary)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun BarColumn(
    height: androidx.compose.ui.unit.Dp,
    label: String,
    active: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(
                        if (active) {
                            Brush.verticalGradient(listOf(AionColors.SecondaryContainer, AionColors.Primary))
                        } else {
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.05f)),
                            )
                        },
                    )
                    // In HTML it has cyan-glow class for active bars.
                    .border(
                        1.dp,
                        if (active) AionColors.Primary.copy(alpha = 0.2f) else Color.Transparent,
                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    ),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) AionColors.Primary else AionColors.OnSurfaceVariant.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun AppUsageRow(
    app: AppUsage,
    totalMs: Long,
) {
    val pct = (app.foregroundMs.toFloat() / totalMs * 100).toInt()
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Smartphone, contentDescription = null, tint = AionColors.OnSurfaceVariant)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AionColors.OnSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        formatDuration(app.foregroundMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = AionColors.OnSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                6.dp,
                            ).background(Color.White.copy(alpha = 0.05f), CircleShape),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(
                                    pct / 100f,
                                ).height(
                                    6.dp,
                                ).background(
                                    Brush.horizontalGradient(listOf(AionColors.Primary, AionColors.Secondary)),
                                    CircleShape,
                                ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MockAppUsageRow(
    name: String,
    time: String,
    pct: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AionColors.OnSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(time, style = MaterialTheme.typography.bodySmall, color = AionColors.OnSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                6.dp,
                            ).background(Color.White.copy(alpha = 0.05f), CircleShape),
                ) {
                    Box(modifier = Modifier.fillMaxWidth(pct / 100f).height(6.dp).background(color, CircleShape))
                }
            }
        }
    }
}
