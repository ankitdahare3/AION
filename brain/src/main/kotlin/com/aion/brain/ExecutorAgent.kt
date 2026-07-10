package com.aion.brain

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
 * DOC-004 §5 — one plan step -> [ActionExecutor], or (for side-effecting steps) sets
 * `needsApproval` and waits for [AionGraph]'s `ApprovalGate` instead of executing directly (SR-01).
 *
 * The frozen [AgentState] has no per-step "approved" flag, so this relies on [AionGraph.run]'s
 * actual mechanics: after `needsApproval=true` is returned, the graph blocks on `ApprovalGate.await`
 * and unconditionally clears `needsApproval` before routing again. If the step was denied, the
 * `ApprovalGate` implementation is expected to set `done=true` (the graph loop then ends before
 * revisiting this node); otherwise routing sends the *same* `currentStep` back to this node, which
 * we recognize via [pendingApprovalForStep] as "already asked, safe to execute now" rather than
 * asking again. One `ExecutorAgent` instance is assumed to belong to a single graph run.
 */
class ExecutorAgent(
    private val executor: ActionExecutor,
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

        val outcome = executor.execute(step)
        pendingApprovalForStep = -1
        return s.copy(
            currentStep = s.currentStep + 1,
            toolResults = s.toolResults + outcome.observation,
            failures =
                if (outcome.success) {
                    s.failures
                } else {
                    s.failures +
                        (outcome.error ?: "step ${s.currentStep} (${step.action}) failed")
                },
        )
    }
}
