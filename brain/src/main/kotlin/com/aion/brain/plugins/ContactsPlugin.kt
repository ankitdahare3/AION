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

/** DOC-005 §5 v1 built-in — Contacts. Same thin-adapter-over-[ActionExecutor] pattern as [SystemPlugin]. */
class ContactsPlugin(
    private val executor: ActionExecutor,
) : AionPlugin() {
    override val manifest =
        PluginManifest(
            id = "com.aion.plugin.contacts",
            name = "Contacts",
            version = "1.0.0",
            apiLevel = DNAValidator.SUPPORTED_API_LEVEL,
            permissions = listOf("CONTACTS"),
            tools =
                listOf(
                    ToolSchema(
                        "open_contacts",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Opens the Contacts app",
                    ),
                    ToolSchema(
                        "start_new_contact",
                        sideEffect = false,
                        inputSchema = "{}",
                        description = "Opens the new-contact form (does not fill it in)",
                    ),
                ),
            dna = DnaConfig(benchmark = "bench/contacts.yaml"),
        )

    override suspend fun execute(call: ToolCall): ToolResult {
        val step =
            when (call.name) {
                "open_contacts" -> PlanStep("launchApp", "com.android.contacts", "Contacts app open", false)
                "start_new_contact" ->
                    PlanStep("tap", "Create new contact", "new contact form open", false)
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
