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
 * DOC-005 §5 v1 built-in — Phone/SMS. `call_contact`/`send_sms` aren't wired: actually placing a
 * call or composing a text needs typed input (a number or message body), which [PlanStep] can't
 * carry yet (BACKLOG.md) — only the achievable-today navigation tools are declared, rather than
 * tools that would always fail.
 */
class PhoneSmsPlugin(
    private val executor: ActionExecutor,
) : AionPlugin() {
    override val manifest =
        PluginManifest(
            id = "com.aion.plugin.phone_sms",
            name = "Phone/SMS",
            version = "1.0.0",
            apiLevel = DNAValidator.SUPPORTED_API_LEVEL,
            permissions = listOf("PHONE", "SMS"),
            tools =
                listOf(
                    ToolSchema(
                        "open_dialer",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Opens the Phone/dialer app",
                    ),
                    ToolSchema(
                        "open_messages",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Opens the Messages app",
                    ),
                ),
            dna = DnaConfig(benchmark = "bench/phone_sms.yaml"),
        )

    override suspend fun execute(call: ToolCall): ToolResult {
        val step =
            when (call.name) {
                "open_dialer" -> PlanStep("launchApp", "com.android.dialer", "Dialer app open", false)
                "open_messages" -> PlanStep("launchApp", "com.android.messaging", "Messages app open", false)
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
