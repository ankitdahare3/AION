package com.aion.host.mockup

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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

private val MOCK_HABITS =
    listOf(
        "Morning Exercise" to 12,
        "Read 20 Pages" to 7,
        "Drink 3L Water" to 5,
        "No Sugar" to 3,
        "Learn Python" to 18,
    )

/** Mockup "Habits & Goals" — no habit-tracking feature exists in AION; local checkbox state only. */
@Composable
fun HabitsGoalsScreen(modifier: Modifier = Modifier) {
    IllustrativeScreen(
        title = "Habits & Goals",
        note = "Illustrative — no habit-tracking feature is built; these checkboxes aren't persisted.",
        modifier = modifier,
    ) {
        MOCK_HABITS.forEach { (name, streak) ->
            var done by remember { mutableStateOf(false) }
            GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)) {
                    Checkbox(checked = done, onCheckedChange = { done = it })
                    Text(
                        "$name — streak $streak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AionColors.OnBackground,
                        modifier = Modifier.weight(1f).padding(top = 12.dp),
                    )
                }
            }
        }
    }
}
