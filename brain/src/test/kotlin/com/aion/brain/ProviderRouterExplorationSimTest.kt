package com.aion.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * T-112 AC — "score shifts measurable in sim." Uses the real, unmodified (frozen-contract)
 * [ProviderRouter] against a "good" provider (always succeeds) and a "bad" one (always fails), both
 * pinned to identical tier/latency so the ONLY thing that can ever separate them is the taskScore
 * term [ExploringScoreStore] touches — isolating exploration's effect from unrelated scoring-formula
 * confounds rather than hoping they wash out.
 */
class ProviderRouterExplorationSimTest {
    private val req = BrainRequest(taskType = TaskType.CHAT, system = "sys", messages = emptyList())
    private val unlimitedBudget =
        object : BudgetGuard {
            override fun canSpend(req: BrainRequest) = true

            override fun record(cost: Double) {}
        }

    private class SimScoreStore : ScoreStore {
        private val successEma = mutableMapOf<String, Double>()
        val failureCount = mutableMapOf<String, Int>()

        override fun taskScore(
            id: String,
            t: TaskType,
        ) = successEma[id] ?: ScoringMath.DEFAULT_SUCCESS_SCORE

        // Pinned to the default for both providers — see class doc: isolates the taskScore effect.
        override fun latencyNorm(id: String) = ScoringMath.latencyNorm(ScoringMath.DEFAULT_LATENCY_MS)

        override fun notInCooldown(id: String) = true

        override fun recordSuccess(
            id: String,
            t: TaskType,
            latencyMs: Long,
            cost: Double,
        ) {
            successEma[id] = ScoringMath.ema(successEma[id] ?: ScoringMath.DEFAULT_SUCCESS_SCORE, 1.0)
        }

        override fun recordFailure(
            id: String,
            t: TaskType,
            e: ProviderFailure,
        ) {
            successEma[id] = ScoringMath.ema(successEma[id] ?: ScoringMath.DEFAULT_SUCCESS_SCORE, 0.0)
            failureCount[id] = (failureCount[id] ?: 0) + 1
        }
    }

    private fun goodProvider() =
        object : Provider {
            override val id = "good"
            override val tier = Tier.LOCAL
            override val caps = ProviderCaps()

            override suspend fun complete(req: BrainRequest) =
                BrainResult(
                    text = "ok",
                    provider = "good",
                    latencyMs = ScoringMath.DEFAULT_LATENCY_MS.toLong(),
                    costUsd = 0.0,
                )
        }

    private fun badProvider() =
        object : Provider {
            override val id = "bad"
            override val tier = Tier.LOCAL
            override val caps = ProviderCaps()

            override suspend fun complete(req: BrainRequest): BrainResult = throw ProviderFailure.Server("boom")
        }

    @Test
    fun `without exploration, the failing provider is never tried once its score falls behind`() =
        runTest {
            val scores = SimScoreStore()
            val router = ProviderRouter(listOf(goodProvider(), badProvider()), scores, unlimitedBudget)

            repeat(ROUNDS) { router.route(req) }

            assertEquals(
                "pure-greedy should never try 'bad' once 'good' ties or leads",
                0,
                scores.failureCount["bad"] ?: 0,
            )
        }

    @Test
    fun `with 5 percent exploration, the failing provider keeps getting tried across many rounds`() =
        runTest {
            val scores = SimScoreStore()
            val exploring = ExploringScoreStore(scores, epsilon = 0.05, random = seededRandom())
            val router = ProviderRouter(listOf(goodProvider(), badProvider()), exploring, unlimitedBudget)

            repeat(ROUNDS) { router.route(req) }

            val badAttempts = scores.failureCount["bad"] ?: 0
            assertTrue(
                "expected exploration to keep testing 'bad' repeatedly across $ROUNDS rounds, got only $badAttempts",
                badAttempts >= 15,
            )
        }

    private fun seededRandom(seed: Long = 42): () -> Double {
        val r = Random(seed)
        return { r.nextDouble() }
    }

    private companion object {
        const val ROUNDS = 500
    }
}
