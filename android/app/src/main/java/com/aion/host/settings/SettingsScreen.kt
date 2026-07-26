package com.aion.host.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.brain.ModelDownloader
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import com.aion.host.ui.theme.MockupScaffold

/**
 * "DEVICE CONTROLS" replaces what was originally a decorative Developer Mode switch + two no-op
 * rows (API Endpoints / Experimental Features) — these three are the real, working controls that
 * used to live in MainActivity's now-removed debug button row (voice FGS start/stop, the
 * kill-switch safety overlay, and the Explore Device PROFILE-memory scan). Nothing else in the
 * app's UI could reach them once that row was deleted.
 */
@Composable
fun SettingsScreen(
    voiceRunning: Boolean,
    onToggleVoice: () -> Unit,
    overlayRunning: Boolean,
    onToggleOverlay: () -> Unit,
    bubbleRunning: Boolean,
    onToggleBubble: () -> Unit,
    onExploreDevice: () -> Unit,
    modelDownloadState: ModelDownloader.State,
    onDownloadModel: () -> Unit,
    onOpenApiKeys: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    MockupScaffold(
        title = "Settings",
        onBack = onBack,
        trailingIcon = null,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
        ) {
            // Section 1: AION PREFERENCES
            Text(
                "AION PREFERENCES",
                style = MaterialTheme.typography.labelSmall,
                color = AionColors.Primary,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            )
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingsRow(icon = Icons.Filled.RecordVoiceOver, title = "Voice Model", value = "AION Ultra")
                    SettingsRow(icon = Icons.Filled.Language, title = "Language", value = "English (India)")
                    SettingsRow(icon = Icons.Filled.Speed, title = "Voice Speed", value = "Normal")
                    SettingsRow(icon = Icons.Filled.SmartToy, title = "Assistant Name", value = "AION")
                    SettingsRow(
                        icon = Icons.Filled.GraphicEq,
                        title = "Wake Word",
                        value = "Hey AION",
                        showDivider = false,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Section 2: DEVICE CONTROLS — real, working controls, not mockup rows.
            Text(
                "DEVICE CONTROLS",
                style = MaterialTheme.typography.labelSmall,
                color = AionColors.TertiaryContainer,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            )
            GlassPanel(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, AionColors.TertiaryContainer.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                cornerRadius = 16.dp,
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ToggleRow(
                        icon = Icons.Filled.RecordVoiceOver,
                        title = "Voice Listening",
                        subtitle = if (voiceRunning) "Listening (mic active)" else "Idle (mic off)",
                        checked = voiceRunning,
                        onCheckedChange = { onToggleVoice() },
                    )
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    1.dp,
                                ).background(AionColors.OutlineVariant.copy(alpha = 0.3f)),
                    )
                    ToggleRow(
                        icon = Icons.Filled.Shield,
                        title = "Kill-Switch Overlay",
                        subtitle = if (overlayRunning) "Shown — tap to hide" else "Hidden — tap to show",
                        checked = overlayRunning,
                        onCheckedChange = { onToggleOverlay() },
                    )
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    1.dp,
                                ).background(AionColors.OutlineVariant.copy(alpha = 0.3f)),
                    )
                    ToggleRow(
                        icon = Icons.Filled.BubbleChart,
                        title = "Floating Assistant",
                        subtitle =
                            if (bubbleRunning) {
                                "On — stays visible over other apps"
                            } else {
                                "Off — AION closes when you switch apps"
                            },
                        checked = bubbleRunning,
                        onCheckedChange = { onToggleBubble() },
                    )
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    1.dp,
                                ).background(AionColors.OutlineVariant.copy(alpha = 0.3f)),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Filled.Search, AionColors.TertiaryContainer)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Explore Device",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AionColors.OnSurface,
                                )
                                Text(
                                    "Scans installed apps to ground the planner",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AionColors.OnSurfaceVariant,
                                )
                            }
                        }
                        TextButton(onClick = onExploreDevice) { Text("Run") }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Section 3: LOCAL INTELLIGENCE — T-172, the in-app counterpart to the manual `adb
            // push` model setup. Not a toggle: downloading a real ~600MB file is a deliberate,
            // one-time action the owner takes, not an ambient background switch.
            Text(
                "LOCAL INTELLIGENCE",
                style = MaterialTheme.typography.labelSmall,
                color = AionColors.TertiaryContainer,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            )
            GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ModelDownloadRow(modelDownloadState, onDownloadModel, onOpenApiKeys)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ModelDownloadRow(
    state: ModelDownloader.State,
    onDownload: () -> Unit,
    onOpenApiKeys: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                if (state is ModelDownloader.State.Done) Icons.Filled.CloudDone else Icons.Filled.Download,
                AionColors.TertiaryContainer,
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text("On-Device AI Model", style = MaterialTheme.typography.bodyLarge, color = AionColors.OnSurface)
                Text(
                    when (state) {
                        is ModelDownloader.State.Idle -> "Not downloaded — real intent understanding needs this"
                        is ModelDownloader.State.Downloading -> {
                            val total = state.totalBytes
                            if (total != null && total > 0) {
                                val pct = (state.bytesDownloaded * 100 / total)
                                "Downloading… $pct% (${state.bytesDownloaded / 1_000_000}MB / ${total / 1_000_000}MB)"
                            } else {
                                "Downloading… ${state.bytesDownloaded / 1_000_000}MB"
                            }
                        }
                        is ModelDownloader.State.Done -> "Ready — used automatically, falls back if unavailable"
                        is ModelDownloader.State.Failed -> state.message
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (state is ModelDownloader.State.Failed) {
                            AionColors.Error
                        } else {
                            AionColors.OnSurfaceVariant
                        },
                )
            }
        }
        when (state) {
            is ModelDownloader.State.Idle -> TextButton(onClick = onDownload) { Text("Download") }
            is ModelDownloader.State.Downloading -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
            is ModelDownloader.State.Done -> {}
            is ModelDownloader.State.Failed ->
                TextButton(onClick = { if (state.message.contains("token")) onOpenApiKeys() else onDownload() }) {
                    Text(if (state.message.contains("token")) "Add Token" else "Retry")
                }
        }
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    tint: Color,
) {
    Box(
        modifier = Modifier.size(36.dp).background(tint.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, AionColors.TertiaryContainer)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = AionColors.OnSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AionColors.OnSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = AionColors.Background,
                    checkedTrackColor = AionColors.TertiaryContainer,
                    uncheckedThumbColor = AionColors.Outline,
                    uncheckedTrackColor = AionColors.SurfaceContainer,
                ),
        )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    valueColor: Color = AionColors.Primary,
    showDivider: Boolean = true,
    iconColor: Color = AionColors.OnSurfaceVariant,
    iconBgColor: Color = Color.White.copy(alpha = 0.1f),
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).background(iconBgColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.bodyLarge, color = AionColors.OnSurface)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = valueColor,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = AionColors.OutlineVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (showDivider) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
        }
    }
}
