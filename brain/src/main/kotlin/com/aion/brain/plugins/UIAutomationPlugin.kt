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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * DOC-005 §5 v1 built-in — "UIAutomation(generic fallback)", DOC-009 §1's last-resort routing tier
 * (Official API > AppFunctions/MCP > UI Automation). T-077: this is the ONLY plugin that still
 * touches [ActionExecutor] directly — every other caller (namely [com.aion.brain.ExecutorAgent])
 * now goes through [com.aion.brain.PluginManager] exclusively.
 *
 * Every tool here declares `sideEffect = false` at the manifest level on purpose: whether a given
 * `tap`/`launchApp`/etc. call is dangerous depends on the specific step (a tap on "Search" vs. a
 * tap on "Delete account"), not the generic action name — that per-call judgment already happened
 * upstream (the Planner set [PlanStep.sideEffect], `ExecutorAgent` already gated it through
 * `AionGraph`'s `ApprovalGate` before ever building the [ToolCall]). If this plugin's tools also
 * declared `sideEffect = true`, [com.aion.brain.PluginManager.route] would ask for approval a
 * second time for the same action.
 */
class UIAutomationPlugin(
    private val executor: ActionExecutor,
) : AionPlugin() {
    override val manifest =
        PluginManifest(
            id = ID,
            name = "UI Automation",
            version = "1.0.0",
            apiLevel = DNAValidator.SUPPORTED_API_LEVEL,
            permissions = emptyList(),
            tools =
                listOf(
                    ToolSchema(
                        "tap",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Taps a resolved on-screen element",
                    ),
                    ToolSchema(
                        "longPress",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Long-presses a resolved on-screen element",
                    ),
                    ToolSchema(
                        "swipe",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Swipes within a resolved region",
                    ),
                    ToolSchema(
                        "scrollTo",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Scrolls a resolved container",
                    ),
                    ToolSchema(
                        "type",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Types text into a resolved field",
                    ),
                    ToolSchema(
                        "launchApp",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Launches an app by package name",
                    ),
                    ToolSchema(
                        "globalAction",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Performs a system-level navigation action (BACK/HOME/RECENTS)",
                    ),
                ),
            dna = DnaConfig(benchmark = "bench/uiautomation.yaml"),
        )

    override suspend fun execute(call: ToolCall): ToolResult {
        val args =
            try {
                Json.parseToJsonElement(call.argsJson).jsonObject
            } catch (e: Exception) {
                return ToolResult(
                    success = false,
                    resultJson = "{}",
                    error = "malformed argsJson for ${call.name}: ${e.message}",
                )
            }
        val target = args["target"]?.jsonPrimitive?.content ?: ""
        val expected = args["expected"]?.jsonPrimitive?.content ?: ""
        val step = PlanStep(call.name, target, expected, call.sideEffect)

        val outcome = executor.execute(step)
        return ToolResult(
            success = outcome.success,
            resultJson = observationJson(outcome.observation),
            error = outcome.error,
        )
    }

    companion object {
        const val ID = "com.aion.plugin.uiautomation"

        /** Matches how [UIAutomationPlugin.execute] parses `argsJson` back out. */
        fun argsJsonFor(step: PlanStep): String =
            JsonObject(
                mapOf("target" to JsonPrimitive(step.target), "expected" to JsonPrimitive(step.expected)),
            ).toString()
    }
}
