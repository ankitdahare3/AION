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
 * DOC-005 §5 v1 built-in — Browser. `search_web`/`open_url` aren't wired: need typed query/URL
 * text, which [PlanStep] can't carry yet (BACKLOG.md).
 */
class BrowserPlugin(
    private val executor: ActionExecutor,
) : AionPlugin() {
    override val manifest =
        PluginManifest(
            id = "com.aion.plugin.browser",
            name = "Browser",
            version = "1.0.0",
            apiLevel = DNAValidator.SUPPORTED_API_LEVEL,
            permissions = listOf("BROWSER", "INTERNET"),
            tools =
                listOf(
                    ToolSchema(
                        "open_browser",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Opens the default browser app",
                    ),
                ),
            dna = DnaConfig(benchmark = "bench/browser.yaml"),
        )

    override suspend fun execute(call: ToolCall): ToolResult {
        val step =
            when (call.name) {
                "open_browser" -> PlanStep("launchApp", "com.android.chrome", "Browser app open", false)
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
