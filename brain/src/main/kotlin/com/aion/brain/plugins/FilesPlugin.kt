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
 * DOC-005 §5 v1 built-in — Files. `search_file` isn't wired: needs typed query text, which
 * [PlanStep] can't carry yet (BACKLOG.md).
 */
class FilesPlugin(
    private val executor: ActionExecutor,
) : AionPlugin() {
    override val manifest =
        PluginManifest(
            id = "com.aion.plugin.files",
            name = "Files",
            version = "1.0.0",
            apiLevel = DNAValidator.SUPPORTED_API_LEVEL,
            permissions = listOf("FILES"),
            tools =
                listOf(
                    ToolSchema(
                        "open_files",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Opens the Files app",
                    ),
                ),
            dna = DnaConfig(benchmark = "bench/files.yaml"),
        )

    override suspend fun execute(call: ToolCall): ToolResult {
        val step =
            when (call.name) {
                "open_files" -> PlanStep("launchApp", "com.android.documentsui", "Files app open", false)
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
