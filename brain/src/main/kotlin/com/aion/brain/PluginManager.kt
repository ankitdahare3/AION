package com.aion.brain

/** DOC-005 §3's Kotlin plugin API. `benchmark`/`reflect` default to no-ops — nothing calls them yet (QAAgent/ReflectorAgent-plugin integration is future scope), but the shape matches the doc so a real plugin can override them later without a signature change. */
abstract class AionPlugin {
    abstract val manifest: PluginManifest

    open suspend fun onInstall() {}

    abstract suspend fun execute(call: ToolCall): ToolResult

    open suspend fun reflect(outcome: String): String? = null
}

/** DOC-005 §4 — "sideEffect:true tools ALWAYS hit ApprovalGate". One tool call, one yes/no. */
fun interface PluginApprovalGate {
    suspend fun approve(
        pluginId: String,
        call: ToolCall,
    ): Boolean
}

sealed class PluginRouteResult {
    data class Success(
        val result: ToolResult,
    ) : PluginRouteResult()

    data class Rejected(
        val reason: String,
    ) : PluginRouteResult()
}

/**
 * DOC-005 §4 lifecycle: discover → [register] (runs [DNAValidator]) → [enable] (models the
 * "user approval" step — a caller decides when to call this, e.g. after a QAAgent test suite that
 * doesn't exist yet) → [route] executes a [ToolCall] against an enabled plugin, gating every
 * `sideEffect:true` tool through [PluginApprovalGate] first. True classloader sandboxing (DOC-005
 * §4's isolation/CPU/mem-quota facade) is out of scope for T-070's "contract tests" AC — there are
 * zero real plugin implementations yet to sandbox; this is the routing/validation contract only.
 */
class PluginManager(
    private val approval: PluginApprovalGate,
) {
    private val validated = mutableMapOf<String, AionPlugin>()
    private val enabledIds = mutableSetOf<String>()

    fun register(plugin: AionPlugin): ValidationResult {
        val result = DNAValidator.validate(plugin.manifest)
        if (result is ValidationResult.Valid) validated[plugin.manifest.id] = plugin
        return result
    }

    /** No-op (returns false) for a plugin that was never registered or failed validation. */
    fun enable(pluginId: String): Boolean {
        if (pluginId !in validated) return false
        enabledIds += pluginId
        return true
    }

    fun disable(pluginId: String) {
        enabledIds -= pluginId
    }

    fun isEnabled(pluginId: String): Boolean = pluginId in enabledIds

    suspend fun route(
        pluginId: String,
        call: ToolCall,
    ): PluginRouteResult {
        val plugin =
            validated[pluginId]
                ?: return PluginRouteResult.Rejected("plugin not registered or failed validation: $pluginId")
        if (pluginId !in enabledIds) return PluginRouteResult.Rejected("plugin not enabled: $pluginId")
        val tool =
            plugin.manifest.tools.find { it.name == call.name }
                ?: return PluginRouteResult.Rejected("plugin $pluginId has no tool named '${call.name}'")

        if (tool.sideEffect && !approval.approve(pluginId, call)) {
            return PluginRouteResult.Rejected("side-effect tool '${call.name}' was denied approval")
        }

        return PluginRouteResult.Success(plugin.execute(call))
    }
}
