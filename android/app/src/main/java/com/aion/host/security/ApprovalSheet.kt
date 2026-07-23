package com.aion.host.security

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aion.host.ui.theme.AionColors

/**
 * SR-01/02 — renders whenever [ApprovalGateService] has a pending request. No caller exists yet
 * (ExecutorAgent wiring is T-051); this composable is inert (shows nothing) until then, but is
 * fully wired so the first real side-effect action has somewhere to surface its approval prompt.
 */
@Composable
fun ApprovalSheetHost(approvalGateService: ApprovalGateService) {
    val pending by approvalGateService.pending.collectAsState()
    val request = pending ?: return

    Dialog(
        onDismissRequest = { approvalGateService.resolve(request.id, false) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF1A2123).copy(alpha = 0.95f))
                    .border(1.dp, AionColors.TertiaryContainer.copy(alpha = 0.4f), RoundedCornerShape(32.dp)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
            ) {
                // Warning Header
                Box(contentAlignment = Alignment.Center) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.6f,
                        animationSpec =
                            infiniteRepeatable(
                                animation = tween(2000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse,
                            ),
                        label = "alpha",
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(
                                    64.dp,
                                ).background(AionColors.TertiaryContainer.copy(alpha = alpha), CircleShape),
                    )
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = AionColors.TertiaryContainer,
                        modifier = Modifier.size(48.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Approval Required",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AionColors.OnSurface,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "AION is requesting permission to perform this side-effect action. Denying this will hard stop the current task.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AionColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(24.dp))

                // Details Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Action Row
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(12.dp),
                                ).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                    ) {
                        Column {
                            Text(
                                "ACTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = AionColors.OutlineVariant,
                                letterSpacing = 1.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Chat,
                                    contentDescription = null,
                                    tint = AionColors.Tertiary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    request.voiceLine,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AionColors.OnSurface,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Content Preview
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    AionColors.SurfaceContainerHigh.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp),
                                ).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                    ) {
                        Row {
                            Box(modifier = Modifier.width(4.dp).height(40.dp).background(AionColors.Tertiary))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "DETAILS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AionColors.OutlineVariant,
                                    letterSpacing = 1.sp,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "\"${request.detail}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AionColors.OnSurfaceVariant,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Risk Level
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "RISK LEVEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = AionColors.OutlineVariant,
                        letterSpacing = 1.sp,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(AionColors.Tertiary, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Side-Effect / Reversible",
                            style = MaterialTheme.typography.labelSmall,
                            color = AionColors.Tertiary,
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Buttons
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF3CD7FF), Color(0xFF00D4FF))))
                                .clickable { approvalGateService.resolve(request.id, true) }
                                .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "APPROVE ACTION",
                            style = MaterialTheme.typography.labelLarge,
                            color = AionColors.OnPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, AionColors.Error, RoundedCornerShape(16.dp))
                                .clickable { approvalGateService.resolve(request.id, false) }
                                .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "DENY & STOP",
                            style = MaterialTheme.typography.labelLarge,
                            color = AionColors.Error,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
