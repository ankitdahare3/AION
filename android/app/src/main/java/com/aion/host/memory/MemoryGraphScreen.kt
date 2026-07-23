package com.aion.host.memory

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.brain.MemoryStore
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.AionTopBar

private data class GraphNode(
    val id: String,
    val label: String,
    val value: String,
    val icon: ImageVector,
    val color: Color,
    val topPercent: Float,
    val leftPercent: Float,
)

private val MOCK_NODES =
    listOf(
        GraphNode("people", "People", "186", Icons.Filled.Group, Color(0xFFFFBA3D), 0.25f, 0.30f),
        GraphNode("projects", "Projects", "24", Icons.Filled.Folder, AionColors.PrimaryContainer, 0.30f, 0.70f),
        GraphNode("connections", "Connections", "6.5k", Icons.Filled.Hub, Color(0xFFD1BCFF), 0.65f, 0.80f),
        GraphNode("skills", "Skills", "36", Icons.Filled.Psychology, AionColors.Primary, 0.75f, 0.40f),
        GraphNode("files", "Files", "2,431", Icons.Filled.Description, Color(0xFFB4EBFF), 0.60f, 0.20f),
        GraphNode("ideas", "Ideas", "142", Icons.Filled.Lightbulb, AionColors.PrimaryContainer, 0.15f, 0.55f),
        GraphNode("patents", "Patents", "78", Icons.Filled.Verified, Color(0xFFFFD9A1), 0.45f, 0.15f),
    )

@Composable
fun MemoryGraphScreen(
    memoryStore: MemoryStore,
    resumeSignal: Int,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(20000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
    )

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF080F12))) {
        // Ambient Glow
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush =
                                Brush.radialGradient(
                                    colors = listOf(AionColors.PrimaryContainer.copy(alpha = 0.05f), Color.Transparent),
                                    center = center,
                                    radius = size.minDimension * 1.5f,
                                ),
                        )
                    },
        )

        Column(modifier = Modifier.fillMaxSize()) {
            AionTopBar(
                title = "Memory Graph",
                trailingIcon = Icons.Filled.MoreVert,
                onTrailingClick = { /* More */ },
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = constraints.maxWidth.toFloat()
                val canvasHeight = constraints.maxHeight.toFloat()

                val centerOffset = Offset(canvasWidth * 0.5f, canvasHeight * 0.5f)

                // Lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    MOCK_NODES.forEach { node ->
                        val nodeOffset =
                            Offset(
                                canvasWidth * node.leftPercent,
                                canvasHeight * node.topPercent,
                            )
                        drawLine(
                            color = AionColors.PrimaryContainer.copy(alpha = 0.2f),
                            start = centerOffset,
                            end = nodeOffset,
                            strokeWidth = 1.5.dp.toPx(),
                        )
                    }
                }

                // Center Node
                Box(
                    modifier =
                        Modifier
                            .offset(
                                x = (maxWidth * 0.5f) - 40.dp,
                                y = (maxHeight * 0.5f) - 40.dp,
                            ).size(80.dp)
                            .background(AionColors.PrimaryContainer.copy(alpha = 0.1f), CircleShape)
                            .border(2.dp, AionColors.PrimaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Psychology,
                        contentDescription = "AION",
                        tint = AionColors.PrimaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        text = "AION",
                        color = AionColors.PrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.offset(y = 56.dp),
                    )
                }

                // Branch Nodes
                MOCK_NODES.forEach { node ->
                    Box(
                        modifier =
                            Modifier
                                .offset(
                                    x = (maxWidth * node.leftPercent) - 20.dp,
                                    y = (maxHeight * node.topPercent) - 20.dp,
                                ).size(40.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, node.color, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            node.icon,
                            contentDescription = node.label,
                            tint = node.color,
                            modifier = Modifier.size(20.dp),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.offset(y = 32.dp),
                        ) {
                            Text(
                                text = node.label,
                                color = node.color,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                            )
                            Text(
                                text = " ${node.value}",
                                color = node.color.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
