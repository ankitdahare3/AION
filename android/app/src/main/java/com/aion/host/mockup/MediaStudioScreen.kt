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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.AionTopBar
import com.aion.host.ui.theme.GlassPanel

private data class StudioTool(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val MOCK_TOOLS =
    listOf(
        StudioTool("Script Writer", Icons.Filled.Lightbulb),
        StudioTool("Image Generator", Icons.Filled.Palette),
        StudioTool("Video Generator", Icons.Filled.Movie),
        StudioTool("Voice Clone", Icons.Filled.RecordVoiceOver),
        StudioTool("Audio Studio", Icons.Filled.GraphicEq),
        StudioTool("Subtitle Generator", Icons.Filled.ClosedCaption),
    )

private data class RecentCreation(
    val title: String,
    val details: String,
)

private val RECENT_CREATIONS =
    listOf(
        RecentCreation("Motivational Reel", "Today 10:30 AM • 30 sec • 1080x1920"),
        RecentCreation("Tech Review Video", "Yesterday 06:20 PM • 2 min • 1920x1080"),
        RecentCreation("AI Explainer Video", "Yesterday 04:15 PM • 5 min • 1080x1080"),
    )

/** Mockup "AI Media Studio" — owner explicitly deferred video/voice generation. No generation backend exists; tiles are non-interactive. */
@Composable
fun MediaStudioScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(AionColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AionTopBar(title = "AI Studio")

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 100.dp),
            ) {
                // Tools Grid
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)) {
                    val rows = MOCK_TOOLS.chunked(2)
                    rows.forEachIndexed { index, rowTools ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            rowTools.forEach { tool ->
                                GlassPanel(
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(20.dp).clickable { },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(48.dp)
                                                    .background(AionColors.Primary.copy(alpha = 0.1f), CircleShape)
                                                    .padding(bottom = 12.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                tool.icon,
                                                contentDescription = tool.name,
                                                tint = AionColors.Glow,
                                                modifier = Modifier.size(28.dp).offset(y = 6.dp),
                                            )
                                        }
                                        Text(
                                            text = tool.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AionColors.OnSurface,
                                            textAlign = TextAlign.Center,
                                            letterSpacing = 1.sp,
                                        )
                                    }
                                }
                            }
                            // Fill empty space if row is not full
                            if (rowTools.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        if (index < rows.size - 1) {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                // Recent Creations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Recent Creations",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AionColors.OnSurface,
                    )
                    Text(
                        "View All",
                        style = MaterialTheme.typography.labelSmall,
                        color = AionColors.Primary,
                        modifier =
                            Modifier
                                .clickable {
                                },
                    )
                }

                Spacer(Modifier.height(24.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RECENT_CREATIONS.forEach { creation ->
                        GlassPanel(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(96.dp, 64.dp)
                                            .background(AionColors.SurfaceContainer, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }

                                Spacer(Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        creation.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = AionColors.OnSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        creation.details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AionColors.Outline,
                                    )
                                }

                                Icon(Icons.Filled.MoreVert, contentDescription = null, tint = AionColors.Outline)
                            }
                        }
                    }
                }
            }
        }
    }
}
