package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringMathTest {
    @Test
    fun `ema moves toward the sample without jumping all the way`() {
        val updated = ScoringMath.ema(previous = 0.5, sample = 1.0)
        assertTrue(updated > 0.5)
        assertTrue(updated < 1.0)
        assertEquals(0.65, updated, 1e-9)
    }

    @Test
    fun `latencyNorm clamps to 0 and 1 at the extremes`() {
        assertEquals(0.0, ScoringMath.latencyNorm(0.0), 1e-9)
        assertEquals(1.0, ScoringMath.latencyNorm(ScoringMath.LATENCY_NORM_CEILING_MS * 10), 1e-9)
        assertEquals(0.5, ScoringMath.latencyNorm(ScoringMath.LATENCY_NORM_CEILING_MS / 2), 1e-9)
    }

    @Test
    fun `auth and quota failures get the 6h cooldown`() {
        assertEquals(
            ScoringMath.QUOTA_AUTH_COOLDOWN_MS,
            ScoringMath.cooldownDurationMs(ProviderFailure.Auth("bad key")),
        )
        assertEquals(
            ScoringMath.QUOTA_AUTH_COOLDOWN_MS,
            ScoringMath.cooldownDurationMs(ProviderFailure.Quota("exceeded")),
        )
    }

    @Test
    fun `rate limit gets a short backoff, not the full cooldown`() {
        val backoff = ScoringMath.cooldownDurationMs(ProviderFailure.RateLimit("slow down"))
        assertEquals(ScoringMath.RATE_LIMIT_BACKOFF_MS, backoff)
        assertTrue(backoff < ScoringMath.QUOTA_AUTH_COOLDOWN_MS)
    }

    @Test
    fun `timeout, server, and bad-output failures get no cooldown`() {
        assertEquals(0L, ScoringMath.cooldownDurationMs(ProviderFailure.Timeout("slow")))
        assertEquals(0L, ScoringMath.cooldownDurationMs(ProviderFailure.Server("500")))
        assertEquals(0L, ScoringMath.cooldownDurationMs(ProviderFailure.BadOutput("bad json")))
    }

    @Test
    fun `canSpend allows requests while today's spend plus the per-task ceiling still fits the daily budget`() {
        assertTrue(ScoringMath.canSpend(spentTodayUsd = 0.0))
        assertTrue(
            ScoringMath.canSpend(spentTodayUsd = ScoringMath.DAILY_BUDGET_USD - ScoringMath.PER_TASK_CEILING_USD),
        )
    }

    @Test
    fun `canSpend blocks once the per-task ceiling would push past the daily budget`() {
        assertFalse(
            ScoringMath.canSpend(
                spentTodayUsd = ScoringMath.DAILY_BUDGET_USD - ScoringMath.PER_TASK_CEILING_USD + 0.01,
            ),
        )
        assertFalse(ScoringMath.canSpend(spentTodayUsd = ScoringMath.DAILY_BUDGET_USD))
    }
}
