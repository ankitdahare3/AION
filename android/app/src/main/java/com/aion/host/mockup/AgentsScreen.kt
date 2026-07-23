package com.aion.host.mockup

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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.AionTopBar
import com.aion.host.ui.theme.GlassPanel

private data class MockAgent(
    val name: String,
    val task: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
)

private val MOCK_AGENTS =
    listOf(
        MockAgent("Research Agent", "Web, News, Data", Icons.Filled.Search, Color(0xFF3B82F6)),
        MockAgent("Coding Agent", "Code, Debug, Automate", Icons.Filled.Code, Color(0xFFA855F7)),
        MockAgent("Design Agent", "Visuals, UI, Media", Icons.Filled.Palette, Color(0xFFEC4899)),
    )

/**
 * Mockup "Agents Dashboard" / "Multi-Agent Workspace" / "AI Settings & Agent Management" —
 * AION has exactly one real agent pipeline (planner -> executor -> reflector, see
 * `com.aion.brain.AionGraph`), not a multi-agent orchestration system with independently-running
 * named agents. This is a visual mockup of that design, not a real status display — no backend
 * exists yet to make these numbers genuine.
 */
@Composable
fun AgentsScreen(modifier: Modifier = Modifier) {
    var autoAgentMode by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }

    Box(modifier = modifier.fillMaxSize().background(AionColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AionTopBar(
                title = "Agent Center",
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
                // Auto Agent Mode Toggle
                GlassPanel(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "Auto Agent Mode",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AionColors.OnSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Allow AI to use agents automatically",
                                style = MaterialTheme.typography.bodySmall,
                                color = AionColors.Outline,
                            )
                        }
                        Switch(
                            checked = autoAgentMode,
                            onCheckedChange = { autoAgentMode = it },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AionColors.PrimaryContainer,
                                    uncheckedThumbColor = AionColors.Outline,
                                    uncheckedTrackColor = AionColors.SurfaceContainerHighest,
                                ),
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Tabs
                Row(
                    modifier =
                        Modifier.fillMaxWidth().drawBehind {
                            drawLine(
                                Color.White.copy(alpha = 0.05f),
                                Offset(0f, size.height),
                                Offset(size.width, size.height),
                                1.dp.toPx(),
                            )
                        },
                ) {
                    Box(
                        modifier =
                            Modifier.clickable { selectedTab = 0 }.padding(bottom = 12.dp, end = 16.dp).drawBehind {
                                drawLine(
                                    if (selectedTab ==
                                        0
                                    ) {
                                        AionColors.Primary
                                    } else {
                                        Color.Transparent
                                    },
                                    Offset(0f, size.height),
                                    Offset(size.width, size.height),
                                    2.dp.toPx(),
                                )
                            },
                    ) {
                        Text(
                            "MY AGENTS",
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (selectedTab ==
                                    0
                                ) {
                                    AionColors.Primary
                                } else {
                                    AionColors.Outline
                                },
                            letterSpacing = 1.sp,
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .clickable { selectedTab = 1 }
                                .padding(
                                    bottom = 12.dp,
                                    start = 8.dp,
                                    end = 16.dp,
                                ).drawBehind {
                                    drawLine(
                                        if (selectedTab ==
                                            1
                                        ) {
                                            AionColors.Primary
                                        } else {
                                            Color.Transparent
                                        },
                                        Offset(0f, size.height),
                                        Offset(size.width, size.height),
                                        2.dp.toPx(),
                                    )
                                },
                    ) {
                        Text(
                            "AI AGENTS",
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (selectedTab ==
                                    1
                                ) {
                                    AionColors.Primary
                                } else {
                                    AionColors.Outline
                                },
                            letterSpacing = 1.sp,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Scrollable Agents List
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (selectedTab == 0) {
                        MOCK_AGENTS.forEach { agent ->
                            AgentCard(agent)
                        }
                    } else {
                        Text(
                            "No AI Agents available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AionColors.OnSurfaceVariant,
                            modifier = Modifier.padding(vertical = 32.dp).align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentCard(agent: MockAgent) {
    var enabled by remember { mutableStateOf(true) }

    GlassPanel(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .background(agent.color.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, agent.color.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(agent.icon, contentDescription = null, tint = agent.color, modifier = Modifier.size(28.dp))
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(agent.name, style = MaterialTheme.typography.bodyLarge, color = AionColors.OnSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(agent.task, style = MaterialTheme.typography.bodySmall, color = AionColors.Outline)
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
                        uncheckedTrackColor = AionColors.SurfaceContainerHighest,
                    ),
            )
        }
    }
}
