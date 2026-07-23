package com.aion.host.setup

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.security.AuditLogger
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.AionTopBar
import com.aion.host.ui.theme.GlassPanel
import kotlinx.coroutines.launch

@Composable
fun SetupWizardScreen(
    resumeSignal: Int,
    auditLogger: AuditLogger,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statuses by remember {
        mutableStateOf(SetupPermission.entries.associateWith { it.isGranted(context) })
    }

    LaunchedEffect(resumeSignal) {
        statuses = SetupPermission.entries.associateWith { it.isGranted(context) }
    }

    var pendingPermission by remember { mutableStateOf<SetupPermission?>(null) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val permission = pendingPermission
            if (permission != null) {
                statuses = statuses + (permission to granted)
                scope.launch {
                    auditLogger.record(
                        "user",
                        "setup.permission.result",
                        """{"permission":"${permission.name}","granted":$granted}""",
                    )
                }
            }
            pendingPermission = null
        }

    val allGranted = statuses.values.all { it }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulseScale",
    )

    Box(modifier = modifier.fillMaxSize().background(AionColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AionTopBar(
                title = "AION SETUP",
                onBack = { /* Handled by bottom nav for now */ },
                trailingIcon = Icons.Filled.HelpOutline,
                onTrailingClick = {},
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Central Icon Hero
                Box(
                    modifier = Modifier.size(128.dp).padding(bottom = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .scale(pulseScale)
                                .background(AionColors.PrimaryContainer.copy(alpha = 0.2f), CircleShape),
                    )
                    GlassPanel(
                        modifier = Modifier.size(96.dp),
                        cornerRadius = 48.dp,
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = AionColors.PrimaryContainer,
                                modifier =
                                    Modifier
                                        .size(
                                            48.dp,
                                        ).shadow(
                                            12.dp,
                                            ambientColor = AionColors.PrimaryContainer,
                                            spotColor = AionColors.PrimaryContainer,
                                        ),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Grant Required Permissions",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AionColors.OnSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "AION needs these permissions to operate your device on your behalf.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AionColors.OnSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                Spacer(Modifier.height(40.dp))

                // Permissions List
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SetupPermission.entries.forEach { permission ->
                        val granted = statuses[permission] == true
                        val canAct = permission != SetupPermission.DEVICE_OWNER
                        PermissionRow(
                            permission = permission,
                            granted = granted,
                            onClick = {
                                if (canAct) {
                                    scope.launch {
                                        auditLogger.record(
                                            "user",
                                            "setup.permission.tap",
                                            """{"permission":"${permission.name}"}""",
                                        )
                                    }
                                    val runtimePermission = runtimePermissionFor(permission)
                                    if (runtimePermission != null) {
                                        pendingPermission = permission
                                        permissionLauncher.launch(runtimePermission)
                                    } else {
                                        permission.settingsIntent(context)?.let { context.startActivity(it) }
                                    }
                                }
                            },
                        )
                    }
                }

                Spacer(Modifier.height(120.dp))
            }
        }

        // Fixed Bottom Action Area
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, AionColors.Surface.copy(alpha = 0.9f), AionColors.Surface),
                        ),
                    ).padding(24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (allGranted) {
                                    AionColors.PrimaryContainer
                                } else {
                                    AionColors.PrimaryContainer.copy(
                                        alpha = 0.7f,
                                    )
                                },
                            ).then(
                                if (allGranted) {
                                    Modifier.shadow(
                                        15.dp,
                                        ambientColor = AionColors.PrimaryContainer,
                                        spotColor = AionColors.PrimaryContainer,
                                    )
                                } else {
                                    Modifier
                                },
                            ).clickable(enabled = allGranted) { /* Proceed to next step */ },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        if (!allGranted) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                tint = AionColors.OnPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            text = "CONTINUE",
                            style = MaterialTheme.typography.titleMedium,
                            color = AionColors.OnSurfaceVariant,
                            letterSpacing = 1.sp,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "STEP 1 OF 3",
                    style = MaterialTheme.typography.labelSmall,
                    color = AionColors.OutlineVariant,
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}

internal fun runtimePermissionFor(permission: SetupPermission): String? =
    when (permission) {
        SetupPermission.MICROPHONE -> Manifest.permission.RECORD_AUDIO
        SetupPermission.NOTIFICATIONS -> Manifest.permission.POST_NOTIFICATIONS
        SetupPermission.CALENDAR -> Manifest.permission.READ_CALENDAR
        SetupPermission.CALL_LOG -> Manifest.permission.READ_CALL_LOG
        SetupPermission.SMS -> Manifest.permission.READ_SMS
        SetupPermission.LOCATION -> Manifest.permission.ACCESS_COARSE_LOCATION
        else -> null
    }

@Composable
private fun PermissionRow(
    permission: SetupPermission,
    granted: Boolean,
    onClick: () -> Unit,
) {
    val icon =
        when (permission) {
            SetupPermission.ACCESSIBILITY -> Icons.Filled.AccessibilityNew
            SetupPermission.OVERLAY -> Icons.Filled.Layers
            SetupPermission.NOTIFICATIONS -> Icons.Filled.NotificationsActive
            SetupPermission.NOTIFICATION_ACCESS -> Icons.Filled.NotificationsActive
            SetupPermission.USAGE_ACCESS -> Icons.Filled.Apps
            SetupPermission.MICROPHONE -> Icons.Filled.Mic
            SetupPermission.CALENDAR -> Icons.Filled.CalendarToday
            SetupPermission.CALL_LOG -> Icons.Filled.Call
            SetupPermission.SMS -> Icons.Filled.Sms
            SetupPermission.LOCATION -> Icons.Filled.LocationOn
            SetupPermission.DEVICE_OWNER -> Icons.Filled.AdminPanelSettings
            else -> Icons.Filled.Settings
        }

    GlassPanel(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        cornerRadius = 16.dp,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon box
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(
                            if (granted) {
                                AionColors.PrimaryContainer.copy(
                                    alpha = 0.1f,
                                )
                            } else {
                                AionColors.SurfaceContainer
                            },
                            RoundedCornerShape(12.dp),
                        ).border(
                            1.dp,
                            if (granted) {
                                AionColors.PrimaryContainer.copy(
                                    alpha = 0.2f,
                                )
                            } else {
                                AionColors.OutlineVariant.copy(alpha = 0.3f)
                            },
                            RoundedCornerShape(12.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (granted) AionColors.PrimaryContainer else AionColors.Outline,
                )
            }

            Spacer(Modifier.width(16.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = permission.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = AionColors.OnSurface,
                )
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AionColors.OnSurfaceVariant.copy(alpha = 0.6f),
                )
            }

            Spacer(Modifier.width(16.dp))

            // Custom Toggle
            Box(
                modifier =
                    Modifier
                        .width(48.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (granted) {
                                Brush.linearGradient(listOf(AionColors.PrimaryContainer, AionColors.Primary))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        AionColors.OutlineVariant.copy(alpha = 0.3f),
                                        AionColors.OutlineVariant.copy(alpha = 0.3f),
                                    ),
                                )
                            },
                        ).then(
                            if (granted) {
                                Modifier.shadow(
                                    10.dp,
                                    ambientColor = AionColors.PrimaryContainer,
                                    spotColor = AionColors.PrimaryContainer,
                                )
                            } else {
                                Modifier
                            },
                        ).padding(4.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (granted) Color.White else AionColors.Outline)
                            .align(if (granted) Alignment.CenterEnd else Alignment.CenterStart),
                )
            }
        }
    }
}
