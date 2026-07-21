package com.aion.host.mockup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.IllustrativeScreen
import com.aion.host.ui.theme.ProgressRow

private val MOCK_SKILLS = listOf("Python Programming" to 0.72f, "Data Analysis" to 0.45f, "Prompt Engineering" to 0.81f)

/**
 * Mockup "AI Learning & Skills" — `SkillStore`/`SkillGenerationPipeline` exist in `:brain` and are
 * unit-tested, but have no Hilt binding wired into the real app yet, so nothing ever installs a
 * real Skill today. Static until a real caller exists (see BACKLOG.md).
 */
@Composable
fun AiLearningScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "AION Learning",
        note =
            "Illustrative — the skill-learning engine exists in code but isn't wired into the live " +
                "app yet; no real skill has ever been installed.",
        modifier = modifier,
    ) {
        MOCK_SKILLS.forEach { (name, progress) ->
            ProgressRow(name, progress, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}
