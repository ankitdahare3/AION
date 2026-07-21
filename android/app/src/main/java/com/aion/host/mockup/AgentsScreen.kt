package com.aion.host.mockup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import com.aion.host.ui.theme.IllustrativeScreen
import com.aion.host.ui.theme.ProgressRow

private data class MockAgent(
    val name: String,
    val task: String,
    val progress: Float,
)

private val MOCK_AGENTS =
    listOf(
        MockAgent("Research Agent", "Gathering information", 0.92f),
        MockAgent("Browser Agent", "Working on the web", 1.0f),
        MockAgent("Coding Agent", "Writing & testing code", 0.67f),
        MockAgent("Design Agent", "Generating visuals", 0.55f),
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
    IllustrativeScreen(
        title = "Agents",
        note = "Illustrative — AION runs one real plan/execute/reflect pipeline today, not independent named agents.",
        modifier = modifier,
    ) {
        MOCK_AGENTS.forEach { agent ->
            GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(agent.name, style = MaterialTheme.typography.bodyLarge, color = AionColors.OnBackground)
                    Text(agent.task, style = MaterialTheme.typography.bodySmall, color = AionColors.OnSurfaceVariant)
                    ProgressRow(label = "", progress = agent.progress)
                }
            }
        }
    }
}
