package com.aion.brain

import com.aion.brain.plugins.UIAutomationPlugin

/** What Executor needs from the platform layer — real a11y dispatch lives in :android:app (T-041). */
fun interface ActionExecutor {
    suspend fun execute(step: PlanStep): ExecutionOutcome
}

data class ExecutionOutcome(
    val success: Boolean,
    val observation: String,
    val error: String? = null,
)

/**
 * DOC-004 §5 — one plan step -> [PluginManager] exclusively (T-077: no direct [ActionExecutor]
 * dependency here anymore — that only lives inside [UIAutomationPlugin] now). For a side-effecting
 * step, sets `needsApproval` and waits for [AionGraph]'s `ApprovalGate` first (SR-01).
 *
 * The frozen [AgentState] has no per-step "approved" flag, so this relies on [AionGraph.run]'s
 * actual mechanics: after `needsApproval=true` is returned, the graph blocks on `ApprovalGate.await`
 * and unconditionally clears `needsApproval` before routing again. If the step was denied, the
 * `ApprovalGate` implementation is expected to set `done=true` (the graph loop then ends before
 * revisiting this node); otherwise routing sends the *same* `currentStep` back to this node, which
 * we recognize via [pendingApprovalForStep] as "already asked, safe to execute now" rather than
 * asking again. One `ExecutorAgent` instance is assumed to belong to a single graph run.
 *
 * The [ToolCall] built here always has `sideEffect = false`: the per-call side-effect judgment
 * already happened above (the graph-level approval gate), and [UIAutomationPlugin]'s own tools are
 * declared `sideEffect = false` in its manifest for the same reason — see its KDoc for why a second
 * gate at the plugin level would be redundant here.
 */
class ExecutorAgent(
    private val pluginManager: PluginManager,
    private val uiAutomationPluginId: String = UIAutomationPlugin.ID,
) : Agent {
    private var pendingApprovalForStep = -1

    override suspend fun step(s: AgentState): AgentState {
        val step =
            s.plan.getOrNull(s.currentStep)
                ?: return s.copy(done = true, response = s.response ?: "plan complete")

        if (step.sideEffect && pendingApprovalForStep != s.currentStep) {
            pendingApprovalForStep = s.currentStep
            return s.copy(needsApproval = true)
        }

        val call = ToolCall(name = step.action, argsJson = UIAutomationPlugin.argsJsonFor(step), sideEffect = false)
        val routed = pluginManager.route(uiAutomationPluginId, call)
        pendingApprovalForStep = -1

        val (success, observation, error) =
            when (routed) {
                is PluginRouteResult.Success ->
                    Triple(
                        routed.result.success,
                        routed.result.resultJson,
                        routed.result.error,
                    )
                is PluginRouteResult.Rejected -> Triple(false, "", routed.reason)
            }
        return s.copy(
            currentStep = s.currentStep + 1,
            toolResults = s.toolResults + observation,
            failures =
                if (success) {
                    s.failures
                } else {
                    s.failures + (error ?: "step ${s.currentStep} (${step.action}) failed")
                },
        )
    }
}
