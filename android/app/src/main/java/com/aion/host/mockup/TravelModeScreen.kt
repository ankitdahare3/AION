package com.aion.host.mockup

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import com.aion.host.ui.theme.MockupScaffold

@Composable
fun TravelModeScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    MockupScaffold(
        title = "TRAVEL MODE",
        onBack = onBack,
        trailingIcon = Icons.Filled.LocationOn,
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
            // Hero Card
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                glow = true,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "UPCOMING TRIP",
                        style = MaterialTheme.typography.labelSmall,
                        color = AionColors.Primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Text(
                        "Your trip to Goa",
                        style = MaterialTheme.typography.displayMedium,
                        color = AionColors.OnSurface,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = AionColors.OnSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "May 24 — May 27",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AionColors.OnSurfaceVariant,
                        )
                    }
                }
            }

            // Grid of Info Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Weather Card
                GlassPanel(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(140.dp),
                    cornerRadius = 16.dp,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Filled.Cloud, contentDescription = null, tint = AionColors.Primary)
                            Text(
                                "GOA",
                                style = MaterialTheme.typography.labelSmall,
                                color = AionColors.OnSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "28°C",
                            style = MaterialTheme.typography.displaySmall,
                            color = AionColors.OnSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Partly Cloudy",
                            style = MaterialTheme.typography.bodySmall,
                            color = AionColors.OnSurfaceVariant,
                        )
                    }
                }

                // Flight Card
                GlassPanel(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(140.dp),
                    cornerRadius = 16.dp,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = AionColors.Primary)
                            Box(
                                modifier =
                                    Modifier
                                        .border(
                                            1.dp,
                                            Color(0xFF81C784).copy(alpha = 0.2f),
                                            RoundedCornerShape(percent = 50),
                                        ).background(
                                            Color(0xFF1B5E20).copy(alpha = 0.4f),
                                            RoundedCornerShape(percent = 50),
                                        ).padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    "ON TIME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF81C784),
                                    fontSize = 10.sp,
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "6E 532",
                            style = MaterialTheme.typography.titleLarge,
                            color = AionColors.OnSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "May 24, 08:45 AM",
                            style = MaterialTheme.typography.bodySmall,
                            color = AionColors.OnSurfaceVariant,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }

            // Hotel Card
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
            ) {
                Row(
                    modifier =
                        Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .background(AionColors.PrimaryContainer.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Hotel, contentDescription = null, tint = AionColors.Primary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Sea Breeze Resort",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AionColors.OnSurface,
                        )
                        Text(
                            "Calangute, Goa • Check-in 2:00 PM",
                            style = MaterialTheme.typography.bodySmall,
                            color = AionColors.OnSurfaceVariant,
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AionColors.OnSurfaceVariant)
                }
            }

            // Itinerary Card
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
            ) {
                Row(
                    modifier =
                        Modifier
                            .background(Color.Transparent)
                            .border(1.dp, Color.Transparent, RoundedCornerShape(16.dp))
                            // Simulating the border-l-4 border-primary
                            .padding(start = 4.dp)
                            .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.width(4.dp).height(88.dp).background(AionColors.Primary))
                    Row(
                        modifier =
                            Modifier
                                .padding(start = 16.dp, top = 20.dp, bottom = 20.dp, end = 20.dp)
                                .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(48.dp)
                                    .background(AionColors.SurfaceContainer, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.ListAlt, contentDescription = null, tint = AionColors.Primary)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "5 Activities Planned",
                                style = MaterialTheme.typography.headlineMedium,
                                color = AionColors.OnSurface,
                            )
                            Text(
                                "Tap to view schedule",
                                style = MaterialTheme.typography.bodySmall,
                                color = AionColors.Primary,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = AionColors.OnSurfaceVariant,
                        )
                    }
                }
            }

            // Action Grid
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                TravelActionItem(Icons.Filled.Map, "NAVIGATE")
                TravelActionItem(Icons.Filled.Translate, "TRANSLATE")
                TravelActionItem(Icons.Filled.Explore, "EXPLORE")
                TravelActionItem(Icons.Filled.Security, "SAFETY")
            }

            // AI Chat Bubble
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), contentAlignment = Alignment.Center) {
                GlassPanel(
                    cornerRadius = 99.dp,
                    glow = true,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(modifier = Modifier.width(3.dp).height(12.dp).background(AionColors.Glow, CircleShape))
                            Box(modifier = Modifier.width(3.dp).height(20.dp).background(AionColors.Glow, CircleShape))
                            Box(modifier = Modifier.width(3.dp).height(16.dp).background(AionColors.Glow, CircleShape))
                            Box(modifier = Modifier.width(3.dp).height(10.dp).background(AionColors.Glow, CircleShape))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Need anything? Just speak.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AionColors.OnSurface,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TravelActionItem(
    icon: ImageVector,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { },
    ) {
        GlassPanel(
            modifier = Modifier.size(64.dp),
            cornerRadius = 32.dp,
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = AionColors.OnSurface)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = AionColors.OnSurfaceVariant,
            fontSize = 10.sp,
        )
    }
}
