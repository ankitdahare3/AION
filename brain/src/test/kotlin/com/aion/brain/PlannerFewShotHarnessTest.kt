package com.aion.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
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

private val alwaysCanSpend =
    object : BudgetGuard {
        override fun canSpend(req: BrainRequest) = true

        override fun record(cost: Double) {}
    }

private const val BAD_TARGET = "WRONG_TARGET"
private const val GOOD_TARGET = "correct"

/** Simulates an LLM that "learns" from few-shot counter-examples in its own system prompt. */
private fun learningProvider() =
    object : Provider {
        override val id = "learning"
        override val tier = Tier.LOCAL
        override val caps = ProviderCaps()

        override suspend fun complete(req: BrainRequest): BrainResult {
            val target = if (req.system.contains("Previous mistakes")) GOOD_TARGET else BAD_TARGET
            val text = """[{"action":"tap","target":"$target","expected":"done","sideEffect":false}]"""
            return BrainResult(text = text, provider = id, latencyMs = 1, costUsd = 0.0)
        }
    }

/** T-081 AC — repeat-failure rate drops in a test harness once counter-examples are recorded. */
class PlannerFewShotHarnessTest {
    @Test
    fun `repeat-failure rate drops to zero once counter-examples are recorded for the same goals`() =
        runTest {
            val bank = FewShotBank()
            val router = ProviderRouter(listOf(learningProvider()), noopScoreStore, alwaysCanSpend)
            val goals = (1..20).map { "goal $it" }

            var round1Failures = 0
            for (goal in goals) {
                val agent = PlannerAgent(router, bank)
                val result = agent.step(AgentState(goal = goal))
                val failed = result.plan.any { it.target == BAD_TARGET }
                if (failed) {
                    round1Failures++
                    bank.add(CounterExample(goal, result.plan.toString(), "resolved the wrong target"))
                }
            }

            var round2Failures = 0
            for (goal in goals) {
                val agent = PlannerAgent(router, bank)
                val result = agent.step(AgentState(goal = goal))
                if (result.plan.any { it.target == BAD_TARGET }) round2Failures++
            }

            assertTrue("round 1 should have failed without any counter-examples yet", round1Failures == goals.size)
            assertTrue(
                "repeat-failure rate should drop once counter-examples exist: round1=$round1Failures round2=$round2Failures",
                round2Failures < round1Failures,
            )
            assertTrue(
                "with a counter-example recorded for every goal, round 2 should have zero repeats",
                round2Failures == 0,
            )
        }

    @Test
    fun `a goal with no counter-example is unaffected by the bank`() =
        runTest {
            val bank = FewShotBank()
            bank.add(CounterExample("some other goal", "[]", "irrelevant"))
            val router = ProviderRouter(listOf(learningProvider()), noopScoreStore, alwaysCanSpend)

            val result = PlannerAgent(router, bank).step(AgentState(goal = "a goal never seen before"))

            assertTrue(result.plan.any { it.target == BAD_TARGET })
        }
}
