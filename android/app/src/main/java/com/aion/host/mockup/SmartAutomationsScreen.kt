package com.aion.host.mockup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import com.aion.host.ui.theme.IllustrativeScreen

private data class MockAutomation(
    val name: String,
    val description: String,
)

private val MOCK_AUTOMATIONS =
    listOf(
        MockAutomation("Morning Routine", "News, weather, schedule, reminders"),
        MockAutomation("Office Mode", "Silence calls, open work apps"),
        MockAutomation("Power Saver", "When battery below 20%"),
        MockAutomation("Night Mode", "At 10:00 PM, dim screen, reduce noise"),
        MockAutomation("Backup Reminder", "Every Sunday at 11:00 PM"),
    )

/**
 * Mockup "Smart Automations" — no rule-based automation engine exists in AION yet (every goal
 * runs through the real planner one at a time, nothing triggers on a schedule or condition). The
 * toggles here are local Compose state only, not wired to any real trigger, matching the "no fake
 * button that claims a real effect" rule this codebase follows elsewhere.
 */
@Composable
fun SmartAutomationsScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "Automations",
        note =
            "Illustrative — AION has no scheduled/conditional automation engine yet; these toggles " +
                "aren't wired to anything real.",
        modifier = modifier,
    ) {
        MOCK_AUTOMATIONS.forEach { automation ->
            var enabled by remember { mutableStateOf(true) }
            GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            automation.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AionColors.OnBackground,
                        )
                        Text(
                            automation.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = AionColors.OnSurfaceVariant,
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }
        }
    }
}
