package com.aion.brain

import com.aion.brain.plugins.UIAutomationPlugin
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

private fun fixedPlanProvider() =
    object : Provider {
        override val id = "fixed"
        override val tier = Tier.LOCAL
        override val caps = ProviderCaps()

        override suspend fun complete(req: BrainRequest): BrainResult {
            val plan = """[{"action":"tap","target":"Wi-Fi","expected":"done","sideEffect":false}]"""
            return BrainResult(text = plan, provider = id, latencyMs = 1, costUsd = 0.0)
        }
    }

/** Fails with [failureMessage] on the first call, succeeds on every call after — simulates "the replan's retry actually works". */
private class FlakyOnceExecutor(
    private val failureMessage: String,
) : ActionExecutor {
    private var attempts = 0

    override suspend fun execute(step: PlanStep): ExecutionOutcome {
        attempts++
        return if (attempts == 1) {
            ExecutionOutcome(success = false, observation = "", error = failureMessage)
        } else {
            ExecutionOutcome(success = true, observation = "recovered")
        }
    }
}

private fun assembleGraph(executor: ActionExecutor): AionGraph {
    val manager = PluginManager(PluginApprovalGate { _, _ -> true })
    manager.register(UIAutomationPlugin(executor))
    manager.enable(UIAutomationPlugin.ID)
    val router = ProviderRouter(listOf(fixedPlanProvider()), noopScoreStore, alwaysCanSpend)

    var lastFailureCount = 0
    return AionGraph(
        nodes =
            mapOf(
                "planner" to PlannerAgent(router),
                "executor" to ExecutorAgent(manager),
                "reflector" to ReflectorAgent(),
                "responder" to ResponderAgent(),
                "memory_writer" to MemoryWriterAgent(),
            ),
        route = { node, s ->
            when (node) {
                "planner" -> "executor"
                "executor" ->
                    when {
                        s.failures.size > lastFailureCount -> {
                            lastFailureCount = s.failures.size
                            "reflector"
                        }
                        s.currentStep >= s.plan.size -> "responder"
                        else -> "executor"
                    }
                "reflector" -> if (s.done) AionGraph.END else "planner"
                "responder" -> "memory_writer"
                else -> AionGraph.END
            }
        },
        approval = ApprovalGate { it },
        checkpoints = Checkpointer { },
    )
}

/**
 * T-082 AC — recovery success >50% across an induced-failure suite. One induced message per
 * FR-R02 category (same real error strings as [ReflectorAgentTest]'s induced set), each wrapped in
 * a [FlakyOnceExecutor] that fails once then succeeds — simulating "the replan's retry works" for
 * recoverable causes. Recoverable causes (E1/E2/E3/E6, per [ReflectorAgent]) should reach a real
 * success response; unrecoverable ones (E4/E5/UNKNOWN) should honestly abort, not silently retry
 * forever. 4 of the 7 induced categories are recoverable by [ReflectorAgent]'s own design, so a
 * correctly-working recovery loop should land at 4/7 ≈ 57% — comfortably over the 50% bar without
 * being suspiciously close to 100%, which would suggest the drill wasn't inducing real failures.
 */
class RecoveryDrillTest {
    private val inducedFailures =
        listOf(
            "could not resolve element: Wi-Fi toggle", // E1 — recoverable
            "screen changed but expected text not found", // E2 — recoverable
            "OCR failed to read the button label", // E3 — recoverable
            "planner: failed to produce a valid JSON plan after retry", // E4 — unrecoverable
            "accessibility service not connected", // E5 — unrecoverable
            "gesture callback never fired within 5000ms", // E6 — recoverable
            "java.lang.NullPointerException at line 42", // UNKNOWN — unrecoverable
        )

    @Test
    fun `recovery success rate across the induced-failure suite exceeds 50 percent`() =
        runTest {
            var recovered = 0
            val outcomes = mutableListOf<Pair<String, Boolean>>()

            for (failureMessage in inducedFailures) {
                val graph = assembleGraph(FlakyOnceExecutor(failureMessage))
                val result = graph.run(AgentState(goal = "wifi on karo"))

                val didRecover =
                    result.done &&
                        result.failures.isEmpty() &&
                        result.response == ResponsePhrasing.forSuccess(hinglish = true)
                outcomes += failureMessage to didRecover
                if (didRecover) recovered++
            }

            val rate = recovered.toDouble() / inducedFailures.size
            assertTrue(
                "recovery rate $rate ($recovered/${inducedFailures.size}) should exceed 50%%; outcomes:\n" +
                    outcomes.joinToString("\n") { (msg, ok) -> "$msg -> recovered=$ok" },
                rate > 0.5,
            )
        }

    @Test
    fun `an unrecoverable induced failure never falsely reports success`() =
        runTest {
            val graph = assembleGraph(FlakyOnceExecutor("accessibility service not connected")) // E5

            val result = graph.run(AgentState(goal = "wifi on karo"))

            assertTrue(result.done)
            assertEquals(
                ResponsePhrasing.forFailure(FailureCause.E5_PERMISSION_BLOCKED, hinglish = true),
                result.response,
            )
            assertFalse(result.response!!.contains("accessibility service"))
        }
}
