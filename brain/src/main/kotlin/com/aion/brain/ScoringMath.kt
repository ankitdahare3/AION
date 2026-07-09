package com.aion.brain

/**
 * DOC-013 §2/§4 — pure math behind ScoreStore/BudgetGuard implementations. Kept dependency-free
 * (no Room/Android) so it's unit-testable without an emulator; the Room-backed implementations
 * (android/app) are thin cache+persistence wrappers that delegate here.
 */
object ScoringMath {
    private const val EMA_ALPHA = 0.3

    const val DEFAULT_SUCCESS_SCORE = 0.5
    const val DEFAULT_LATENCY_MS = 1500.0
    const val LATENCY_NORM_CEILING_MS = 5000.0

    // DOC-013 §2: QUOTA/AUTH -> cooldown 6h; RATE_LIMIT -> backoff.
    const val QUOTA_AUTH_COOLDOWN_MS = 6 * 60 * 60 * 1000L
    const val RATE_LIMIT_BACKOFF_MS = 30_000L

    // DOC-013 §4: daily cloud budget default $1, per-task ceiling $0.15 (NFR-06).
    const val DAILY_BUDGET_USD = 1.0
    const val PER_TASK_CEILING_USD = 0.15

    fun ema(
        previous: Double,
        sample: Double,
    ): Double = EMA_ALPHA * sample + (1 - EMA_ALPHA) * previous

    /** Normalizes an EMA latency (ms) to [0,1], where 1.0 = at-or-past the ceiling (worst). */
    fun latencyNorm(latencyEmaMs: Double): Double = (latencyEmaMs / LATENCY_NORM_CEILING_MS).coerceIn(0.0, 1.0)

    /** Cooldown duration in ms for a given failure, per DOC-013 §2. Zero means no cooldown. */
    fun cooldownDurationMs(failure: ProviderFailure): Long =
        when (failure) {
            is ProviderFailure.Auth, is ProviderFailure.Quota -> QUOTA_AUTH_COOLDOWN_MS
            is ProviderFailure.RateLimit -> RATE_LIMIT_BACKOFF_MS
            else -> 0L
        }

    /** True if spending up to the per-task ceiling would still fit inside today's remaining budget. */
    fun canSpend(spentTodayUsd: Double): Boolean = spentTodayUsd + PER_TASK_CEILING_USD <= DAILY_BUDGET_USD
}
