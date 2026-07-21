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
 * replan attempt (recoverable causes) or abort with an honest, natural-language explanation
 * (unrecoverable/unknown) — phrased via [ResponsePhrasing], never as a raw error string or a
 * `(${cause.name})`-style code. This — not [ResponderAgent] — is the one that actually authors most
 * abort messages: `AionGraph`'s frozen `run()` loop is `while (node != END && !s.done)`, checked
 * BEFORE each node runs, so the instant this sets `done = true` the loop exits and returns
 * immediately — `route()`'s own "reflector -> responder" edge is computed but never taken, and
 * ResponderAgent never gets a turn. Verified by tracing `run()` directly rather than assumed;
 * routing the abort case through Responder first isn't possible without changing that loop, which
 * lives in the frozen `AionGraph.kt` (CLAUDE.md: no signature/behavior changes there without an
 * ADR). Full DOC-007 scope — ElementMap patches, provider re-scoring — is EPIC 8 (T-080+), not
 * this.
 *
 * [fewShotBank] (T-081/T-090, optional — same "wired only where a real caller exists" pattern as
 * `PlannerAgent`'s own optional dependencies) — found 2026-07-13 during T-158's live 30-step-loop
 * investigation: `FewShotBank` was fully built and unit-tested (`PlannerFewShotHarnessTest`) but
 * never instantiated anywhere in real app code, so `PlannerAgent` never actually received one and
 * every ReflectorAgent-triggered replan started completely blind to the attempt that just failed —
 * a real, plausible root cause for goals that loop without ever converging: nothing ever told the
 * planner "don't repeat that." Recording a [CounterExample] here, right before clearing state for a
 * fresh replan, is what makes T-081's whole mechanism actually reachable during a live run.
 *
 * [maxRecoverableRetries] (T-162, BACKLOG.md angle 2) — wiring [fewShotBank] in (above) genuinely
 * helps but doesn't fully close the loop: a live re-test of T-158's exact scenario still didn't
 * converge. `AionGraph`'s own `maxSteps=30` DOES already bound every run, but each planner→
 * executor→reflector cycle costs 3 `stepCount` and is dominated by a real ~60-90s LLM call, so a
 * genuinely stuck goal can grind for ~10-15 minutes before that hard ceiling even fires — a real
 * latency problem independent of whether it ever converges. This instance-scoped counter (one
 * `ReflectorAgent` belongs to exactly one graph run, same assumption `ExecutorAgent`'s own
 * `pendingApprovalForStep` already makes) fails fast and honest well before the hard ceiling,
 * instead of silently waiting for it.
 */
class ReflectorAgent(
    private val fewShotBank: FewShotBank? = null,
    private val maxRecoverableRetries: Int = 5,
) : Agent {
    private var recoverableRetryCount = 0

    override suspend fun step(s: AgentState): AgentState {
        val latest =
            s.failures.lastOrNull()
                ?: return s.copy(
                    done = true,
                    response =
                        s.response
                            ?: ResponsePhrasing.forFailure(FailureCause.UNKNOWN, ResponsePhrasing.isHinglish(s.goal)),
                )
        val cause = classify(latest)
        if (cause !in RECOVERABLE) {
            return s.copy(
                done = true,
                response = ResponsePhrasing.forFailure(cause, ResponsePhrasing.isHinglish(s.goal)),
            )
        }
        recoverableRetryCount++
        if (recoverableRetryCount > maxRecoverableRetries) {
            return s.copy(
                done = true,
                response = ResponsePhrasing.forFailure(FailureCause.UNKNOWN, ResponsePhrasing.isHinglish(s.goal)),
            )
        }
        fewShotBank?.add(CounterExample(goal = s.goal, badPlanJson = s.plan.toString(), reason = latest))
        // Clearing failures too, not just plan/currentStep: a fresh replan attempt should start
        // clean, so a subsequent success isn't misreported by ResponderAgent as still-failed
        // because of a stale failure message from the attempt being retried (T-082 recovery drill).
        return s.copy(plan = emptyList(), currentStep = 0, done = false, failures = emptyList())
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
