package com.aion.brain.plugins

import com.aion.brain.ActionExecutor
import com.aion.brain.AionPlugin
import com.aion.brain.DNAValidator
import com.aion.brain.DnaConfig
import com.aion.brain.PlanStep
import com.aion.brain.PluginManifest
import com.aion.brain.ToolCall
import com.aion.brain.ToolResult
import com.aion.brain.ToolSchema

/**
 * DOC-005 §5 v1 built-in — Calendar. `create_event` isn't wired for the same reason as Phone/SMS's
 * `send_sms`: needs typed event details, which [PlanStep] can't carry yet (BACKLOG.md).
 */
class CalendarPlugin(
    private val executor: ActionExecutor,
) : AionPlugin() {
    override val manifest =
        PluginManifest(
            id = "com.aion.plugin.calendar",
            name = "Calendar",
            version = "1.0.0",
            apiLevel = DNAValidator.SUPPORTED_API_LEVEL,
            permissions = listOf("CALENDAR"),
            tools =
                listOf(
                    ToolSchema(
                        "open_calendar",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Opens the Calendar app",
                    ),
                ),
            dna = DnaConfig(benchmark = "bench/calendar.yaml"),
        )

    override suspend fun execute(call: ToolCall): ToolResult {
        val step =
            when (call.name) {
                "open_calendar" -> PlanStep("launchApp", "com.android.calendar", "Calendar app open", false)
                else -> return ToolResult(success = false, resultJson = "{}", error = "unknown tool: ${call.name}")
            }
        val outcome = executor.execute(step)
        return ToolResult(
            success = outcome.success,
            resultJson = observationJson(outcome.observation),
            error = outcome.error,
        )
    }
}
