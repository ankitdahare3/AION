package com.aion.host.mockup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.FlipCameraIos
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel

/**
 * Mockup "Camera Vision Mode" / "Screen Understanding" — real screen-reading already exists
 * (`AionAccessibilityService.currentScreenText()`, used by the live automation graph), but there's
 * no dedicated "point the camera at something and ask AION about it" flow or UI yet — that would
 * need a real CameraX + ML Kit object-recognition pipeline, not built.
 */
@Composable
fun VisionModeScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(AionColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(AionColors.Surface.copy(alpha = 0.6f))
                        .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AionColors.PrimaryFixed)
                    Spacer(Modifier.width(16.dp))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF4ADE80), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Vision Mode Active",
                        style = MaterialTheme.typography.titleLarge,
                        color = AionColors.PrimaryFixed,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp,
                    )
                }
                Icon(Icons.Filled.FlipCameraIos, contentDescription = "Flip Camera", tint = AionColors.PrimaryFixed)
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
            ) {
                // Viewfinder
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(9f / 12f)
                            .background(AionColors.SurfaceContainer)
                            .drawBehind {
                                val cornerSize = 40.dp.toPx()
                                val stroke = 2.dp.toPx()
                                val color = AionColors.Glow

                                // Top Left
                                drawLine(
                                    color,
                                    Offset(20.dp.toPx(), 20.dp.toPx()),
                                    Offset(20.dp.toPx() + cornerSize, 20.dp.toPx()),
                                    stroke,
                                )
                                drawLine(
                                    color,
                                    Offset(20.dp.toPx(), 20.dp.toPx()),
                                    Offset(
                                        20.dp.toPx(),
                                        20.dp.toPx() + cornerSize,
                                    ),
                                    stroke,
                                )

                                // Top Right
                                drawLine(
                                    color,
                                    Offset(size.width - 20.dp.toPx(), 20.dp.toPx()),
                                    Offset(
                                        size.width - 20.dp.toPx() - cornerSize,
                                        20.dp.toPx(),
                                    ),
                                    stroke,
                                )
                                drawLine(
                                    color,
                                    Offset(size.width - 20.dp.toPx(), 20.dp.toPx()),
                                    Offset(
                                        size.width - 20.dp.toPx(),
                                        20.dp.toPx() + cornerSize,
                                    ),
                                    stroke,
                                )

                                // Bottom Left
                                drawLine(
                                    color,
                                    Offset(20.dp.toPx(), size.height - 20.dp.toPx()),
                                    Offset(
                                        20.dp.toPx() + cornerSize,
                                        size.height - 20.dp.toPx(),
                                    ),
                                    stroke,
                                )
                                drawLine(
                                    color,
                                    Offset(20.dp.toPx(), size.height - 20.dp.toPx()),
                                    Offset(
                                        20.dp.toPx(),
                                        size.height - 20.dp.toPx() - cornerSize,
                                    ),
                                    stroke,
                                )

                                // Bottom Right
                                drawLine(
                                    color,
                                    Offset(size.width - 20.dp.toPx(), size.height - 20.dp.toPx()),
                                    Offset(
                                        size.width - 20.dp.toPx() - cornerSize,
                                        size.height - 20.dp.toPx(),
                                    ),
                                    stroke,
                                )
                                drawLine(
                                    color,
                                    Offset(size.width - 20.dp.toPx(), size.height - 20.dp.toPx()),
                                    Offset(
                                        size.width - 20.dp.toPx(),
                                        size.height - 20.dp.toPx() - cornerSize,
                                    ),
                                    stroke,
                                )
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
                    // Target Lock
                    Box(
                        modifier =
                            Modifier
                                .size(192.dp)
                                .border(2.dp, AionColors.PrimaryFixed.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(160.dp)
                                    .border(1.dp, AionColors.PrimaryFixed.copy(alpha = 0.4f), CircleShape),
                        )
                        Icon(
                            Icons.Filled.CenterFocusWeak,
                            contentDescription = null,
                            tint = AionColors.PrimaryFixed,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }

                // Info Card
                Box(modifier = Modifier.padding(horizontal = 24.dp).offset(y = (-48).dp)) {
                    GlassPanel(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column {
                                    Text(
                                        "HP Laptop",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = AionColors.OnSurface,
                                    )
                                    Text(
                                        "HARDWARE IDENTIFIED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AionColors.Glow,
                                        letterSpacing = 1.sp,
                                    )
                                }
                                Box(
                                    modifier =
                                        Modifier
                                            .background(AionColors.SurfaceContainerHighest, CircleShape)
                                            .border(1.dp, AionColors.OutlineVariant.copy(alpha = 0.3f), CircleShape)
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        "SERIAL: 5CD1234XYZ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AionColors.OnSurfaceVariant,
                                        letterSpacing = 1.sp,
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Specs List
                            val specs =
                                listOf(
                                    "Model" to "HP 15s-eq2144AU",
                                    "CPU" to "AMD Ryzen 5 5500U",
                                    "RAM" to "8 GB DDR4 (1 slot empty)",
                                    "Storage" to "512 GB SSD",
                                )
                            specs.forEach { (label, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AionColors.OnSurfaceVariant,
                                    )
                                    Text(
                                        value,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AionColors.OnSurface,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "Battery",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AionColors.OnSurfaceVariant,
                                )
                                Text(
                                    "Good (87%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AionColors.TertiaryFixedDim,
                                    fontWeight = FontWeight.Medium,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "Health",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AionColors.OnSurfaceVariant,
                                )
                                Text(
                                    "Excellent",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4ADE80),
                                    fontWeight = FontWeight.Medium,
                                )
                            }

                            Spacer(Modifier.height(24.dp))

                            // Recommendation
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .background(AionColors.Primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .border(1.dp, AionColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(16.dp),
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = AionColors.PrimaryFixed,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "You can upgrade RAM up to 16 GB to significantly improve multitasking performance in creative apps.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AionColors.PrimaryFixed,
                                    )
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .background(AionColors.TertiaryContainer, RoundedCornerShape(12.dp))
                                            .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "BUY 8GB RAM",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AionColors.OnTertiaryContainer,
                                        letterSpacing = 1.sp,
                                    )
                                }
                                Box(
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .background(
                                                AionColors.PrimaryContainer.copy(alpha = 0.2f),
                                                RoundedCornerShape(12.dp),
                                            ).border(1.dp, AionColors.PrimaryContainer, RoundedCornerShape(12.dp))
                                            .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "HOW TO UPGRADE?",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AionColors.PrimaryFixed,
                                        letterSpacing = 1.sp,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Interaction Bar
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(
                                    AionColors.SurfaceContainerHigh.copy(alpha = 0.8f),
                                    RoundedCornerShape(40.dp),
                                ).border(1.dp, AionColors.OutlineVariant.copy(alpha = 0.1f), RoundedCornerShape(40.dp)),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Filled.PhotoLibrary,
                                contentDescription = "Photo",
                                tint = AionColors.OnSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                "Photo",
                                fontSize = 10.sp,
                                color = AionColors.OnSurfaceVariant,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }

                        Box(
                            modifier =
                                Modifier
                                    .size(80.dp)
                                    .offset(y = (-24).dp)
                                    .background(AionColors.PrimaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Ask",
                                tint = AionColors.OnPrimaryContainer,
                                modifier = Modifier.size(36.dp),
                            )
                            Text(
                                "ASK",
                                fontSize = 12.sp,
                                color = AionColors.PrimaryFixed,
                                letterSpacing = 1.sp,
                                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 32.dp),
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Filled.QrCodeScanner,
                                contentDescription = "Scan",
                                tint = AionColors.PrimaryFixed,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                "Scan",
                                fontSize = 10.sp,
                                color = AionColors.PrimaryFixed,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
