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
 * DOC-005 §5 v1 built-in — System. A thin adapter: every tool maps to a [PlanStep] and delegates
 * to the same [ActionExecutor] `ExecutorAgent` already uses (T-051), rather than a parallel
 * system-control mechanism. Tools are scoped to what's actually achievable today (tap/launchApp/
 * globalAction) — nothing here needs typed text, since [PlanStep] has no field for it yet
 * (BACKLOG.md, filed at T-051).
 */
class SystemPlugin(
    private val executor: ActionExecutor,
) : AionPlugin() {
    override val manifest =
        PluginManifest(
            id = "com.aion.plugin.system",
            name = "System",
            version = "1.0.0",
            apiLevel = DNAValidator.SUPPORTED_API_LEVEL,
            permissions = emptyList(),
            tools =
                listOf(
                    ToolSchema(
                        "open_settings",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Opens the Settings app",
                    ),
                    ToolSchema(
                        "toggle_wifi",
                        sideEffect = true,
                        inputSchema = "{}",
                        description = "Toggles Wi-Fi on/off via Settings",
                    ),
                    ToolSchema(
                        "toggle_bluetooth",
                        sideEffect = true,
                        inputSchema = "{}",
                        description = "Toggles Bluetooth on/off via Settings",
                    ),
                    ToolSchema(
                        "toggle_dark_mode",
                        sideEffect = true,
                        inputSchema = "{}",
                        description = "Toggles Dark theme via Settings > Display",
                    ),
                    ToolSchema(
                        "go_home",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Returns to the home screen",
                    ),
                    ToolSchema(
                        "go_back",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Presses the system Back action",
                    ),
                ),
            dna = DnaConfig(learn = false, reflect = false, benchmark = "bench/system.yaml", update = false),
        )

    override suspend fun execute(call: ToolCall): ToolResult {
        val step =
            when (call.name) {
                "open_settings" -> PlanStep("launchApp", "com.android.settings", "Settings home screen", false)
                "toggle_wifi" -> PlanStep("tap", "Wi-Fi", "Wi-Fi toggled", true)
                "toggle_bluetooth" -> PlanStep("tap", "Bluetooth", "Bluetooth toggled", true)
                "toggle_dark_mode" -> PlanStep("tap", "Dark theme", "Dark mode toggled", true)
                "go_home" -> PlanStep("globalAction", "HOME", "returned to home screen", false)
                "go_back" -> PlanStep("globalAction", "BACK", "navigated back", false)
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
