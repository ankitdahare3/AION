package com.aion.brain

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed class SkillRunResult {
    data class Success(
        val skill: Skill,
    ) : SkillRunResult()

    data class Failed(
        val skill: Skill,
        val failedStep: Int,
        val reason: String,
    ) : SkillRunResult()
}

/**
 * T-093 — executes a matched [Skill]'s steps directly through [PluginManager], not through
 * [PlannerAgent]/[ExecutorAgent] — that's the whole point of "skill-first" (DOC-006 §4). An
 * `approval: required` marker step isn't a real tool call; it's a flag that the NEXT tool step's
 * [ToolCall.sideEffect] should be `true`, which [PluginManager.route] already gates through its own
 * [PluginApprovalGate] — reusing that mechanism rather than building a second one.
 */
class SkillExecutor(
    private val pluginManager: PluginManager,
) {
    suspend fun execute(
        skill: Skill,
        pluginId: String,
    ): SkillRunResult {
        skill.steps.forEachIndexed { i, step ->
            if (step.isApprovalGate) return@forEachIndexed
            val tool =
                step.tool ?: return SkillRunResult.Failed(skill, i, "step $i has neither a tool nor an approval marker")
            val precededByApproval = i > 0 && skill.steps[i - 1].isApprovalGate
            val call = ToolCall(name = tool, argsJson = argsToJson(step.args), sideEffect = precededByApproval)

            when (val routed = pluginManager.route(pluginId, call)) {
                is PluginRouteResult.Success ->
                    if (!routed.result.success) {
                        return SkillRunResult.Failed(skill, i, routed.result.error ?: "step $i ($tool) failed")
                    }
                is PluginRouteResult.Rejected -> return SkillRunResult.Failed(skill, i, routed.reason)
            }
        }
        return SkillRunResult.Success(skill)
    }

    private fun argsToJson(args: Map<String, String>): String =
        JsonObject(
            args.mapValues {
                JsonPrimitive(it.value)
            },
        ).toString()
}
