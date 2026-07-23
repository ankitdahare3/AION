package com.aion.host.mockup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.AionTopBar
import com.aion.host.ui.theme.GlassPanel

@Composable
fun AiLearningScreen(modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            AionTopBar(
                title = "AION Learning",
                trailingIcon = Icons.Filled.Person, // Mocking the profile image
                onTrailingClick = {},
            )
        },
        containerColor = AionColors.Background,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Your Expertise
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "Your Expertise",
                        style = MaterialTheme.typography.titleLarge,
                        color = AionColors.Primary,
                    )
                    Text(
                        "Lvl 42",
                        style = MaterialTheme.typography.labelSmall,
                        color = AionColors.Outline,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ExpertiseCard(Icons.Filled.Psychology, "Research", "Advanced")
                    ExpertiseCard(Icons.Filled.Code, "Coding", "Intermediate", Color.White.copy(alpha = 0.1f))
                    ExpertiseCard(Icons.Filled.Palette, "Design", "Advanced")
                    ExpertiseCard(Icons.Filled.EditNote, "Writing", "Advanced")
                }
            }

            // Featured Section
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
            ) {
                Box {
                    // Blur blob background
                    Box(
                        modifier =
                            Modifier
                                .size(120.dp)
                                .align(Alignment.TopEnd)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(AionColors.Primary.copy(alpha = 0.15f), Color.Transparent),
                                    ),
                                ),
                    )

                    Column(modifier = Modifier.padding(24.dp)) {
                        Box(
                            modifier =
                                Modifier
                                    .background(AionColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "BASED ON YOUR WORK",
                                style = MaterialTheme.typography.labelSmall,
                                color = AionColors.Glow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Advanced Excel Automation",
                            style = MaterialTheme.typography.headlineSmall,
                            color = AionColors.OnSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Master complex workflows and AI-driven data synthesis in legacy systems.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AionColors.OnSurfaceVariant,
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(AionColors.PrimaryContainer, RoundedCornerShape(12.dp))
                                    .clickable { }
                                    .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Start Learning",
                                style = MaterialTheme.typography.labelLarge,
                                color = AionColors.OnPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = AionColors.OnPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // Active Courses
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Active Courses",
                    style = MaterialTheme.typography.titleLarge,
                    color = AionColors.Primary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                CourseProgressItem(Icons.Filled.Terminal, "Python Programming", 0.72f)
                CourseProgressItem(Icons.Filled.Analytics, "Data Analysis", 0.45f, active = false)
                CourseProgressItem(Icons.Filled.ElectricBolt, "Prompt Engineering", 0.81f)
            }

            // Upcoming Milestone
            Row(
                // Solid border, not dashed like the mockup — Compose has no dashed-border modifier.
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, AionColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .border(2.dp, AionColors.Glow.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = AionColors.Primary)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "NEXT MILESTONE",
                        style = MaterialTheme.typography.labelSmall,
                        color = AionColors.Outline,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Neural Architecture Certificate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AionColors.OnSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AionColors.Primary)
            }
        }
    }
}

@Composable
private fun ExpertiseCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    borderColor: Color = AionColors.Primary.copy(alpha = 0.2f),
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(72.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .border(1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = title, tint = AionColors.Primary, modifier = Modifier.size(28.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = AionColors.OnSurface,
                fontSize = 11.sp,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (subtitle == "Advanced") AionColors.Glow.copy(alpha = 0.7f) else AionColors.Outline,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun CourseProgressItem(
    icon: ImageVector,
    title: String,
    progress: Float,
    active: Boolean = true,
) {
    GlassPanel(
        cornerRadius = 12.dp,
        modifier =
            Modifier.fillMaxWidth().then(
                if (active) {
                    Modifier.border(1.dp, AionColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                },
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .background(AionColors.SurfaceContainer, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = AionColors.Primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AionColors.OnSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (active) AionColors.Glow else AionColors.Outline,
                    fontWeight = FontWeight.Bold,
                )
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(AionColors.SurfaceContainer, CircleShape),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(progress)
                            .fillMaxSize()
                            .background(
                                if (active) {
                                    Brush.horizontalGradient(listOf(Color(0xFF00677e), AionColors.PrimaryContainer))
                                } else {
                                    Brush.horizontalGradient(listOf(AionColors.Outline, AionColors.Outline))
                                },
                                CircleShape,
                            ),
                )
            }
        }
    }
}
