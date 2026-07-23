package com.aion.host.mockup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.AionTopBar
import com.aion.host.ui.theme.GlassPanel

private data class MockAutomation(
    val name: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val initialEnabled: Boolean = true,
)

private val MOCK_AUTOMATIONS =
    listOf(
        MockAutomation("Morning Routine", "News, weather, schedule, reminders", Icons.Filled.WbSunny),
        MockAutomation("Office Mode", "Silence calls, open work apps", Icons.Filled.Work),
        MockAutomation("Power Saver", "When battery below 20%", Icons.Filled.BatteryChargingFull, false),
        MockAutomation("Night Mode", "At 10:00 PM, dim screen, reduce noise", Icons.Filled.Nightlight),
        MockAutomation("Backup Reminder", "Every Sunday at 11:00 AM", Icons.Filled.Backup),
    )

private val TABS = listOf("All", "Personal", "Work", "System")

/**
 * Mockup "Smart Automations" — no rule-based automation engine exists in AION yet (every goal
 * runs through the real planner one at a time, nothing triggers on a schedule or condition). The
 * toggles here are local Compose state only, not wired to any real trigger, matching the "no fake
 * button that claims a real effect" rule this codebase follows elsewhere.
 */
@Composable
fun SmartAutomationsScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(0) }

    Box(modifier = modifier.fillMaxSize().background(AionColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AionTopBar(
                title = "Automations",
                trailingIcon = Icons.Filled.Add,
                onTrailingClick = { /* Add Automation */ },
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 100.dp),
            ) {
                // Tab Bar
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 24.dp),
                ) {
                    itemsIndexed(TABS) { index, tab ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { selectedTab = index }
                                    .background(
                                        if (isSelected) {
                                            AionColors.PrimaryContainer.copy(
                                                alpha = 0.1f,
                                            )
                                        } else {
                                            AionColors.SurfaceContainerLow.copy(alpha = 0.6f)
                                        },
                                    ).border(
                                        1.dp,
                                        if (isSelected) {
                                            AionColors.PrimaryContainer
                                        } else {
                                            Color.White.copy(
                                                alpha = 0.05f,
                                            )
                                        },
                                        RoundedCornerShape(50),
                                    ).padding(horizontal = 24.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = tab,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) AionColors.Primary else AionColors.OnSurfaceVariant,
                                letterSpacing = 1.sp,
                            )
                        }
                    }
                }

                // Automation List
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    MOCK_AUTOMATIONS.forEach { automation ->
                        AutomationCard(automation)
                    }
                }
            }
        }
    }
}

@Composable
private fun AutomationCard(automation: MockAutomation) {
    var enabled by remember { mutableStateOf(automation.initialEnabled) }

    val borderColor = if (enabled) AionColors.PrimaryContainer else Color.White.copy(alpha = 0.05f)
    val bgColor =
        if (enabled) {
            AionColors.PrimaryContainer.copy(
                alpha = 0.05f,
            )
        } else {
            AionColors.SurfaceContainerLow.copy(alpha = 0.6f)
        }

    GlassPanel(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
    ) {
        Box(modifier = Modifier.background(bgColor)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .background(
                                    if (enabled) {
                                        AionColors.PrimaryContainer.copy(
                                            alpha = 0.1f,
                                        )
                                    } else {
                                        AionColors.SurfaceVariant
                                    },
                                    CircleShape,
                                ).border(
                                    1.dp,
                                    if (enabled) {
                                        AionColors.PrimaryContainer.copy(
                                            alpha = 0.3f,
                                        )
                                    } else {
                                        AionColors.OutlineVariant.copy(alpha = 0.3f)
                                    },
                                    CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            automation.icon,
                            contentDescription = null,
                            tint = if (enabled) AionColors.PrimaryContainer else AionColors.OnSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column {
                        Text(
                            automation.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = AionColors.OnSurface,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            automation.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = AionColors.OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AionColors.PrimaryContainer,
                            uncheckedThumbColor = AionColors.Outline,
                            uncheckedTrackColor = AionColors.SurfaceVariant,
                        ),
                )
            }
        }
    }
}
