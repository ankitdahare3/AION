package com.aion.brain

/** DOC-007 §2 — FR-R02 failure taxonomy. UNKNOWN is a real outcome, not a bug: "AI never pretends". */
enum class FailureCause {
    E1_WRONG_ELEMENT,
    E2_UI_CHANGED,
    E3_VISION_MISREAD,
    E4_MODEL_PLAN_ERROR,
    E5_PERMISSION_BLOCKED,
    E6_TIMING_RACE,
    UNKNOWN,
}

/**
 * DOC-004 §5 / DOC-007 §2 — ReflectorAgent v0: classify the latest failure via a keyword heuristic
 * (grounded in this codebase's own real error strings — ActionDispatcher, StepVerifier,
 * DispatcherActionExecutor, PlannerAgent, ShizukuBridge), then either clear the plan for a fresh
 * replan attempt (recoverable causes) or abort with an honest explanation (unrecoverable/unknown).
 * Full DOC-007 scope — ElementMap patches, planner few-shot bank, provider re-scoring — is EPIC 8
 * (T-080+), not this. AionGraph's own `maxSteps` circuit breaker already bounds retry loops, so
 * this doesn't need its own retry counter.
 */
class ReflectorAgent : Agent {
    override suspend fun step(s: AgentState): AgentState {
        val latest =
            s.failures.lastOrNull() ?: return s.copy(done = true, response = s.response ?: "nothing to reflect on")
        val cause = classify(latest)
        return if (cause in RECOVERABLE) {
            // Clearing failures too, not just plan/currentStep: a fresh replan attempt should start
            // clean, so a subsequent success isn't misreported by ResponderAgent as still-failed
            // because of a stale failure message from the attempt being retried (T-082 recovery drill).
            s.copy(plan = emptyList(), currentStep = 0, done = false, failures = emptyList())
        } else {
            s.copy(done = true, response = "AION couldn't complete \"${s.goal}\": $latest (${cause.name})")
        }
    }

    companion object {
        private val RECOVERABLE =
            setOf(
                FailureCause.E1_WRONG_ELEMENT,
                FailureCause.E2_UI_CHANGED,
                FailureCause.E3_VISION_MISREAD,
                FailureCause.E6_TIMING_RACE,
            )

        fun classify(message: String): FailureCause {
            val m = message.lowercase()
            return when {
                listOf("ocr", "vision", "screenshot", "misread").any { it in m } -> FailureCause.E3_VISION_MISREAD
                listOf(
                    "could not resolve element",
                    "element not found",
                    "unresolved",
                ).any { it in m } -> FailureCause.E1_WRONG_ELEMENT
                listOf(
                    "callback never fired",
                    "returned false",
                    "timed out",
                    "cancelled",
                    "did not change",
                    "debounce",
                ).any {
                    it in
                        m
                } ->
                    FailureCause.E6_TIMING_RACE
                listOf(
                    "not connected",
                    "no launch intent",
                    "permission",
                    "unavailable",
                    "not yet supported",
                    "not installed",
                ).any { it in m } -> FailureCause.E5_PERMISSION_BLOCKED
                listOf("not valid json", "failed to produce a valid", "invalid plan", "schema").any { it in m } ->
                    FailureCause.E4_MODEL_PLAN_ERROR
                listOf("expected text not found", "screen changed but", "unexpected screen").any {
                    it in m
                } -> FailureCause.E2_UI_CHANGED
                else -> FailureCause.UNKNOWN
            }
        }
    }
}
