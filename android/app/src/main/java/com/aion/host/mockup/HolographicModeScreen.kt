package com.aion.host.mockup

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel

@Composable
fun HolographicModeScreen(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "alpha",
    )

    Box(modifier = modifier.fillMaxSize().background(AionColors.Background)) {
        // Scan Line Simulation
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        100.dp,
                    ).background(AionColors.PrimaryContainer.copy(alpha = 0.05f)),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Simulated Top Bar
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(AionColors.Surface.copy(alpha = 0.6f))
                        .border(1.dp, AionColors.OutlineVariant.copy(alpha = 0.1f))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = "AION",
                        tint = AionColors.Primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "AION",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AionColors.Primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier =
                            Modifier
                                .background(AionColors.Primary.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, AionColors.Primary.copy(alpha = 0.3f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        8.dp,
                                    ).background(AionColors.Primary.copy(alpha = alpha), CircleShape),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "HOLOGRAPHIC MODE",
                            style = MaterialTheme.typography.labelSmall,
                            color = AionColors.Primary,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = AionColors.OnSurfaceVariant)
                }
            }

            // Main Content Area
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Placeholder for 3D Hologram
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.ViewInAr,
                        contentDescription = "3D Hologram Area",
                        tint = AionColors.PrimaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.size(120.dp),
                    )
                }

                // Greeting Section
                Text(
                    text = "Good Evening, Ankit.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = AionColors.OnSurface,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "How can I assist you?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AionColors.OnSurfaceVariant,
                )

                Spacer(Modifier.height(32.dp))

                // Action Grid
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ActionChip(Icons.Filled.CalendarToday, "Plan my day")
                        ActionChip(Icons.Filled.MusicNote, "Play music")
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ActionChip(Icons.Filled.Chat, "Open WhatsApp")
                        ActionChip(Icons.Filled.Psychology, "Research AI")
                    }
                }
            }

            // Bottom Controls Shell
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 0.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Main Interaction Bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = AionColors.Outline,
                            modifier = Modifier.size(32.dp),
                        )

                        Box(
                            modifier =
                                Modifier
                                    .size(80.dp)
                                    .background(AionColors.Primary.copy(alpha = 0.2f), CircleShape)
                                    .border(1.dp, AionColors.Primary.copy(alpha = 0.5f), CircleShape)
                                    .clickable { },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Mic,
                                contentDescription = "Mic",
                                tint = AionColors.Primary,
                                modifier = Modifier.size(36.dp),
                            )
                        }

                        Icon(
                            Icons.Filled.ViewInAr,
                            contentDescription = "AR View",
                            tint = AionColors.Outline,
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    // System Labels
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        SystemLabel("SYSTEM", "ONLINE")
                        SystemLabel("LATENCY", "12ms")
                        SystemLabel("SYNC", "READY")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                .border(1.dp, AionColors.OutlineVariant.copy(alpha = 0.3f), CircleShape)
                .clickable { }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = AionColors.Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AionColors.OnSurface,
        )
    }
}

@Composable
private fun SystemLabel(
    title: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = AionColors.OnSurfaceVariant.copy(alpha = 0.5f),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = AionColors.Primary,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
