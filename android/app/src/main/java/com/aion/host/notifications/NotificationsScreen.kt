package com.aion.host.notifications

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.brain.Memory
import com.aion.brain.MemoryStore
import com.aion.host.svc.NotificationIngestion
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import com.aion.host.ui.theme.MockupScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    memoryStore: MemoryStore,
    resumeSignal: Int,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var notifications by remember { mutableStateOf(emptyList<Memory>()) }

    LaunchedEffect(resumeSignal) {
        notifications =
            memoryStore
                .getAllActive()
                .filter { it.provenance == NotificationIngestion.PROVENANCE }
                .sortedByDescending { it.created }
    }

    MockupScaffold(
        title = "AION",
        onBack = onBack,
        trailingIcon = Icons.Filled.Search,
        onTrailingClick = {},
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Tab Navigation
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SMART", style = MaterialTheme.typography.labelMedium, color = AionColors.Primary)
                        Spacer(Modifier.height(4.dp))
                        Box(modifier = Modifier.width(32.dp).height(2.dp).background(AionColors.Primary))
                    }
                }
                item {
                    Text(
                        "IMPORTANT",
                        style = MaterialTheme.typography.labelMedium,
                        color = AionColors.OnSurfaceVariant,
                    )
                }
                item { Text("ALL", style = MaterialTheme.typography.labelMedium, color = AionColors.OnSurfaceVariant) }
                item {
                    Text(
                        "MUTED",
                        style = MaterialTheme.typography.labelMedium,
                        color = AionColors.OnSurfaceVariant,
                    )
                }
            }

            // Smart Summary Card
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                glow = true,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column {
                            Text(
                                "Smart Summary",
                                style = MaterialTheme.typography.headlineSmall,
                                color = AionColors.OnSurface,
                            )
                            Text(
                                if (notifications.isNotEmpty()) {
                                    "You have ${notifications.size} updates."
                                } else {
                                    "No new notifications."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = AionColors.OnSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = AionColors.Primary.copy(alpha = 0.5f),
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if (notifications.isEmpty()) {
                        // Mock Items from Stitch design if no real notifications
                        MockNotificationRow(Color(0xFFFF4D4D), "HR MAIL", "5M AGO", "Verification document required")
                        MockNotificationRow(
                            Color(0xFFFF9933),
                            "RAHUL SHARMA",
                            "15M AGO",
                            "Let's reschedule the meeting.",
                        )
                        MockNotificationRow(
                            Color(0xFF00FF9D),
                            "BANK ALERT",
                            "30M AGO",
                            "₹24,560 credited to your account",
                        )
                        MockNotificationRow(AionColors.Primary, "SYSTEM", "1H AGO", "AION OS update available")
                    } else {
                        // Real Notifications
                        notifications.forEachIndexed { index, memory ->
                            val color =
                                when (index % 4) {
                                    0 -> AionColors.Primary
                                    1 -> Color(0xFFFF9933)
                                    2 -> Color(0xFF00FF9D)
                                    else -> Color(0xFFFF4D4D)
                                }
                            RealNotificationRow(color, memory)
                        }
                    }
                }
            }

            // Silent Notifications Section
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("SILENT NOTIFICATIONS", style = MaterialTheme.typography.labelMedium, color = AionColors.Outline)
                Box(
                    modifier =
                        Modifier
                            .background(AionColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(percent = 50))
                            .clickable { }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        "CLEAR ALL",
                        style = MaterialTheme.typography.labelSmall,
                        color = AionColors.Primary,
                        fontSize = 10.sp,
                    )
                }
            }

            SilentNotificationRow(Icons.Filled.Mail, "Gmail", "4h ago", "12 new emails")
            SilentNotificationRow(Icons.Filled.Work, "LinkedIn", "3h ago", "5 new notifications")
        }
    }
}

@Composable
private fun MockNotificationRow(
    dotColor: Color,
    title: String,
    time: String,
    message: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(dotColor)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = AionColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(time, style = MaterialTheme.typography.bodySmall, color = AionColors.Outline, fontSize = 10.sp)
            }
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = AionColors.OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RealNotificationRow(
    dotColor: Color,
    memory: Memory,
) {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = formatter.format(Date(memory.created))

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(dotColor)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "NOTIFICATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = AionColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    timeString,
                    style = MaterialTheme.typography.bodySmall,
                    color = AionColors.Outline,
                    fontSize = 10.sp,
                )
            }
            Text(
                memory.text,
                style = MaterialTheme.typography.bodySmall,
                color = AionColors.OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier =
            Modifier
                .size(8.dp)
                .background(color, CircleShape)
                // Simulated glow
                .border(2.dp, color.copy(alpha = 0.4f), CircleShape),
    )
}

@Composable
private fun SilentNotificationRow(
    icon: ImageVector,
    title: String,
    time: String,
    message: String,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clickable { },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(AionColors.SurfaceContainer, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = AionColors.OnSurfaceVariant)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, style = MaterialTheme.typography.labelSmall, color = AionColors.OnSurface)
                    Text(time, style = MaterialTheme.typography.bodySmall, color = AionColors.Outline, fontSize = 10.sp)
                }
                Text(message, style = MaterialTheme.typography.bodySmall, color = AionColors.OnSurfaceVariant)
            }
        }
    }
}
