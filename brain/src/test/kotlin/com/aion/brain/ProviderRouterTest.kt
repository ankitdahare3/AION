package com.aion.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

private val noopScoreStore =
    object : ScoreStore {
        override fun taskScore(
            id: String,
            t: TaskType,
        ) = 0.5

        override fun latencyNorm(id: String) = 0.5

        override fun notInCooldown(id: String) = true

        override fun recordSuccess(
            id: String,
            t: TaskType,
            latencyMs: Long,
            cost: Double,
        ) {}

        override fun recordFailure(
            id: String,
            t: TaskType,
            e: ProviderFailure,
        ) {}
    }

private val req = BrainRequest(taskType = TaskType.CHAT, system = "sys", messages = emptyList())

private fun fakeProvider(
    id: String,
    tier: Tier,
    result: BrainResult,
) = object : Provider {
    override val id = id
    override val tier = tier
    override val caps = ProviderCaps()

    override suspend fun complete(req: BrainRequest) = result
}

class ProviderRouterTest {
    @Test
    fun `routes to the only candidate on success`() =
        runTest {
            val result = BrainResult(text = "hi", provider = "local1", latencyMs = 10, costUsd = 0.0)
            val router =
                ProviderRouter(
                    registry = listOf(fakeProvider("local1", Tier.LOCAL, result)),
                    scores = noopScoreStore,
                    budget =
                        object : BudgetGuard {
                            override fun canSpend(req: BrainRequest) = true

                            override fun record(cost: Double) {}
                        },
                )
            assertEquals("local1", router.route(req).provider)
        }

    @Test
    fun `fails with a diagnosable message when all candidates are budget-exhausted`() =
        runTest {
            val result = BrainResult(text = "hi", provider = "paid1", latencyMs = 10, costUsd = 1.0)
            val router =
                ProviderRouter(
                    registry = listOf(fakeProvider("paid1", Tier.PAID, result)),
                    scores = noopScoreStore,
                    budget =
                        object : BudgetGuard {
                            override fun canSpend(req: BrainRequest) = false

                            override fun record(cost: Double) {}
                        },
                )
            try {
                router.route(req)
                fail("expected IllegalStateException")
            } catch (e: IllegalStateException) {
                assertTrue(e.message!!.contains("budget-exhausted"))
            }
        }
}
