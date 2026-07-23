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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.AionTopBar
import com.aion.host.ui.theme.GlassPanel
import kotlinx.coroutines.launch

/** T-024 — API keys settings screen. Values only ever touch [SecretVault]; never logged (SR-08). */
@Composable
fun SecretsScreen(
    secretVault: SecretVault,
    auditLogger: AuditLogger,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize().background(AionColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AionTopBar(
                title = "Secret Vault",
                trailingIcon = Icons.Filled.Settings,
                onTrailingClick = { /* Settings */ },
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 100.dp),
            ) {
                // Security Banner
                GlassPanel(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF34D399).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF34D399))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Keystore-encrypted API Keys",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF34D399),
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 0.4f,
                                    targetValue = 1f,
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
                                                6.dp,
                                            ).background(Color(0xFF10B981).copy(alpha = alpha), CircleShape),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "FLAG_SECURE active.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AionColors.OnSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                ProviderKey.entries.forEach { providerKey ->
                    ProviderKeyRow(providerKey, secretVault) {
                        scope.launch {
                            auditLogger.record("user", "secretvault.save", """{"provider":"${providerKey.name}"}""")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderKeyRow(
    providerKey: ProviderKey,
    secretVault: SecretVault,
    onSaved: () -> Unit,
) {
    var value by remember { mutableStateOf(secretVault.get(providerKey) ?: "") }
    var saved by remember { mutableStateOf(secretVault.has(providerKey)) }
    var passwordVisible by remember { mutableStateOf(false) }

    GlassPanel(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, AionColors.PrimaryContainer.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(providerKey.label, style = MaterialTheme.typography.labelSmall, color = AionColors.OnSurface)
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = AionColors.OnSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.height(12.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(AionColors.SurfaceContainerLowest, RoundedCornerShape(8.dp))
                        .border(1.dp, AionColors.OutlineVariant, RoundedCornerShape(8.dp)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                    BasicTextField(
                        value = value,
                        onValueChange = {
                            value = it
                            saved = false
                        },
                        textStyle =
                            MaterialTheme.typography.bodyMedium.copy(
                                color = AionColors.Primary,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation =
                            if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation(
                                    '•',
                                )
                            },
                        cursorBrush = SolidColor(AionColors.Primary),
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                    )
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = AionColors.OnSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (saved) Color(0xFF10B981) else AionColors.TertiaryContainer
                    Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (saved) "Connected" else "Unsaved Changes",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontSize =
                            androidx.compose.ui.unit
                                .TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                    )
                }

                if (!saved) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AionColors.PrimaryContainer.copy(alpha = 0.1f))
                                .border(1.dp, AionColors.PrimaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable {
                                    secretVault.put(providerKey, value)
                                    saved = true
                                    onSaved()
                                }.padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("SAVE", style = MaterialTheme.typography.labelSmall, color = AionColors.PrimaryContainer)
                    }
                }
            }
        }
    }
}
