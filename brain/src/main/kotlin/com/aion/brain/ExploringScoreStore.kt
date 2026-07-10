package com.aion.brain

/**
 * DOC-008 §4 — "5% ε-greedy to keep testing alternatives." [ProviderRouter] is a frozen contract
 * (CLAUDE.md: no signature changes without an ADR) — this adds exploration entirely at the
 * [ScoreStore] layer instead, the extension point the interface already exists for (`RoomScoreStore`
 * is itself just one swappable implementation). On each [taskScore] call, with probability [epsilon]
 * the real score is replaced with the maximum possible value (1.0), giving that provider a real
 * chance to win [ProviderRouter]'s existing `sortedByDescending` selection this round — without
 * [ProviderRouter] itself ever being touched or needing to know exploration exists.
 *
 * Per-call rather than per-`route()` randomness (a true textbook ε-greedy picks ONE uniform-random
 * arm per decision) is a deliberate, honestly-noted simplification: [ScoreStore]'s frozen shape
 * gives no way to correlate multiple [taskScore] calls as belonging to the same routing decision.
 * The practical effect is the same as the doc's stated goal — a currently-losing provider
 * periodically gets boosted enough to be tried again instead of being starved forever — just
 * implemented as independent per-provider exploration rolls rather than one shared per-round pick.
 */
class ExploringScoreStore(
    private val delegate: ScoreStore,
    private val epsilon: Double = DEFAULT_EPSILON,
    private val random: () -> Double = Math::random,
) : ScoreStore {
    override fun taskScore(
        id: String,
        t: TaskType,
    ): Double = if (random() < epsilon) MAX_SCORE else delegate.taskScore(id, t)

    override fun latencyNorm(id: String): Double = delegate.latencyNorm(id)

    override fun notInCooldown(id: String): Boolean = delegate.notInCooldown(id)

    override fun recordSuccess(
        id: String,
        t: TaskType,
        latencyMs: Long,
        cost: Double,
    ) = delegate.recordSuccess(id, t, latencyMs, cost)

    override fun recordFailure(
        id: String,
        t: TaskType,
        e: ProviderFailure,
    ) = delegate.recordFailure(id, t, e)

    companion object {
        const val DEFAULT_EPSILON = 0.05
        private const val MAX_SCORE = 1.0
    }
}
