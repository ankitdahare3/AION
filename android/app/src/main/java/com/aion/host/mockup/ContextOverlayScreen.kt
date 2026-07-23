package com.aion.host.mockup

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel

private data class ContextAction(
    val icon: ImageVector,
    val label: String,
)

private val MOCK_ACTIONS =
    listOf(
        ContextAction(Icons.Filled.Summarize, "Summarize this video"),
        ContextAction(Icons.Filled.Translate, "Translate to Hindi"),
        ContextAction(Icons.Filled.School, "Explain in simple words"),
        ContextAction(Icons.Filled.Description, "Create notes"),
        ContextAction(Icons.Filled.Search, "Find related videos"),
        ContextAction(Icons.Filled.Bookmark, "Save key points"),
        ContextAction(Icons.Filled.AccountTree, "Create mind map"),
    )

@Composable
fun ContextOverlayScreen(modifier: Modifier = Modifier) {
    // Simulated overlay background (dimmed)
    Box(modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) {
        // Simulated Top Bar
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(AionColors.Surface.copy(alpha = 0.6f))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AionColors.OnSurfaceVariant)
                Spacer(Modifier.width(12.dp))

                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = "alpha",
                )
                Box(
                    modifier =
                        Modifier
                            .size(
                                8.dp,
                            ).background(AionColors.PrimaryContainer.copy(alpha = alpha), CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "YOUTUBE CONTEXT",
                    style = MaterialTheme.typography.labelSmall,
                    color = AionColors.OnSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Cast, contentDescription = "Cast", tint = AionColors.OnSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = AionColors.OnSurfaceVariant)
            }
        }

        // Bottom Sheet Style Panel
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
        ) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth().height(500.dp),
                cornerRadius = 32.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Logo & Brand
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Icon(
                            Icons.Filled.BubbleChart,
                            contentDescription = "AION",
                            tint = AionColors.PrimaryContainer,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "AION",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AionColors.PrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp,
                        )
                    }
                    Text(
                        "I can help you with this video.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AionColors.OnSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 24.dp),
                    )

                    // Action List
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(MOCK_ACTIONS) { action ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                        .clickable { }
                                        .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(40.dp)
                                                .background(
                                                    AionColors.PrimaryContainer.copy(alpha = 0.2f),
                                                    RoundedCornerShape(12.dp),
                                                ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            action.icon,
                                            contentDescription = null,
                                            tint = AionColors.PrimaryContainer,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = action.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = AionColors.OnSurface,
                                    )
                                }
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AionColors.Outline)
                            }
                        }
                    }
                }
            }
        }
    }
}
