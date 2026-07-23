package com.aion.host

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.security.AuditLogger
import com.aion.host.security.SecretVault
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.AionTopBar
import com.aion.host.ui.theme.GlassPanel
import com.aion.host.ui.theme.glowShadow

@Composable
fun HomeScreen(
    auditLogger: AuditLogger,
    secretVault: SecretVault,
    voiceRunning: Boolean,
    onTapToSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries by auditLogger.observeEntries().collectAsState(initial = emptyList())
    val latestActivity = entries.maxByOrNull { it.seq }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulseScale",
    )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(AionColors.Background),
    ) {
        AionTopBar(
            title = "AION",
            trailingIcon = Icons.Filled.GridView,
            onTrailingClick = { /* Apps hub handled by bottom nav in MainActivity */ },
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Central Voice Interaction Area
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (voiceRunning) "LISTENING..." else "TAP TO SPEAK",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AionColors.Glow,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 32.dp),
                    )

                    // Orb
                    Box(
                        modifier =
                            Modifier
                                .size(192.dp)
                                .scale(if (voiceRunning) pulseScale else 1f)
                                .shadow(
                                    elevation = 32.dp,
                                    shape = CircleShape,
                                    ambientColor = AionColors.Glow,
                                    spotColor = AionColors.Glow,
                                ).clip(CircleShape)
                                .background(
                                    Brush.radialGradient(listOf(AionColors.PrimaryContainer, AionColors.Surface)),
                                ).clickable(onClick = onTapToSpeak),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(AionColors.PrimaryContainer.copy(alpha = 0.2f)),
                        )
                    }

                    Spacer(Modifier.height(32.dp))
                    Text(
                        text = if (voiceRunning) "You can speak now" else "AION is idle",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AionColors.OnSurface.copy(alpha = 0.8f),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Suggestions Cluster
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                QuickActionRow("Open Instagram and upload reel", Icons.Filled.PhotoCamera)
                QuickActionRow("Call Rahul", Icons.Filled.Call)
                QuickActionRow("Prepare my meeting report", Icons.Filled.Description)
            }

            Spacer(Modifier.height(32.dp))

            // Active Context / Recent Activity
            GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (latestActivity != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = latestActivity.action,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = AionColors.OnSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = relativeTime(latestActivity.ts),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AionColors.OnSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Icon(Icons.Filled.Download, contentDescription = null, tint = AionColors.Glow)
                        }
                    } else {
                        Text(
                            "Nothing yet — actions you approve will show up here.",
                            color = AionColors.OnSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Mode Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModeDot("Tap", active = !voiceRunning)
                Spacer(Modifier.width(24.dp))
                ModeDot("Speak", active = voiceRunning)
                Spacer(Modifier.width(24.dp))
                ModeDot("Done", active = false)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun QuickActionRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    GlassPanel(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { /* T-051: Send goal to executor */ },
        cornerRadius = 12.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AionColors.Glow,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = AionColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ModeDot(
    label: String,
    active: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
        Box(
            modifier =
                Modifier
                    .size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (active) AionColors.PrimaryContainer else AionColors.OnSurfaceVariant)
                    .then(if (active) Modifier.glowShadow(CircleShape) else Modifier),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (active) AionColors.Glow else AionColors.OnSurfaceVariant,
        )
    }
}

private fun relativeTime(ts: Long): String {
    val diffMs = System.currentTimeMillis() - ts
    val minutes = diffMs / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 24 * 60 -> "${minutes / 60}h ago"
        else -> "${minutes / (24 * 60)}d ago"
    }
}
