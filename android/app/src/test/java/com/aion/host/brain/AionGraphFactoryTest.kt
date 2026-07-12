package com.aion.host.brain

import com.aion.brain.ActionExecutor
import com.aion.brain.BrainRequest
import com.aion.brain.BrainResult
import com.aion.brain.BudgetGuard
import com.aion.brain.ExecutionOutcome
import com.aion.brain.Memory
import com.aion.brain.MemoryStore
import com.aion.brain.PlanStep
import com.aion.brain.PluginApprovalGate
import com.aion.brain.PluginManager
import com.aion.brain.Provider
import com.aion.brain.ProviderCaps
import com.aion.brain.ProviderFailure
import com.aion.brain.ProviderRouter
import com.aion.brain.ResponsePhrasing
import com.aion.brain.ScoreStore
import com.aion.brain.TaskType
import com.aion.brain.Tier
import com.aion.brain.plugins.UIAutomationPlugin
import com.aion.host.security.ApprovalGateService
import com.aion.host.security.AuditLogger
import com.aion.host.security.FakeAuditDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

private fun scriptedProvider(text: String) =
    object : Provider {
        override val id = "scripted"
        override val tier = Tier.LOCAL
        override val caps = ProviderCaps()

        override suspend fun complete(req: BrainRequest) =
            BrainResult(text = text, provider = id, latencyMs = 1, costUsd = 0.0)
    }

/** T-117: PlannerAgent's optional device-profile context — no memories needed for these graph-wiring tests. */
private val emptyMemoryStore =
    object : MemoryStore {
        override suspend fun insert(memory: Memory) = 0L

        override suspend fun getAllActive(): List<Memory> = emptyList()

        override suspend fun update(memory: Memory) {}

        override suspend fun softDelete(id: Long) {}
    }

/** T-077: ExecutorAgent routes through PluginManager exclusively now — wrap the fake ActionExecutor in the real UIAutomationPlugin. */
private fun pluginManagerWith(executor: ActionExecutor): PluginManager {
    val manager = PluginManager(PluginApprovalGate { _, _ -> true })
    manager.register(UIAutomationPlugin(executor))
    manager.enable(UIAutomationPlugin.ID)
    return manager
}

/** T-053 AC — a real, fully-assembled AionGraph run with checkpoints persisted. */
class AionGraphFactoryTest {
    @Test
    fun `a full graph run persists checkpoints and fires approval for the side-effect step`() =
        runTest {
            val checkpointDao = FakeGraphCheckpointDao()
            val checkpointer = RoomCheckpointer(checkpointDao)
            checkpointer.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

            val approvalService = ApprovalGateService(AuditLogger(FakeAuditDao()))
            val approvalGate = RealApprovalGate(approvalService)
            val autoApprove =
                launch {
                    approvalService.pending.collect { req -> if (req != null) approvalService.resolve(req.id, true) }
                }

            val planJson =
                """[{"action":"tap","target":"Wi-Fi","expected":"Wi-Fi on","sideEffect":false},""" +
                    """{"action":"launchApp","target":"com.android.settings","expected":"Settings open","sideEffect":true}]"""
            val router = ProviderRouter(listOf(scriptedProvider(planJson)), noopScoreStore, alwaysCanSpend)

            val executed = mutableListOf<String>()
            val executor =
                ActionExecutor { step: PlanStep ->
                    executed += step.action
                    ExecutionOutcome(success = true, observation = "ok:${step.action}")
                }

            val factory = AionGraphFactory(checkpointer, emptyMemoryStore)
            val graph = factory.create(router, pluginManagerWith(executor), approvalGate)

            val result = graph.run(com.aion.brain.AgentState(goal = "wifi on karo"))
            testScheduler.advanceUntilIdle()
            autoApprove.cancel()

            assertEquals(listOf("tap", "launchApp"), executed)
            assertTrue(result.done)
            assertTrue(result.response != null)
            assertTrue(
                "expected checkpoints for the run's goal to be persisted",
                checkpointDao.getForGoal("wifi on karo").isNotEmpty(),
            )
        }

    @Test
    fun `a denied side-effect step aborts the run without executing it`() =
        runTest {
            val checkpointer = RoomCheckpointer(FakeGraphCheckpointDao())
            checkpointer.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

            val approvalService = ApprovalGateService(AuditLogger(FakeAuditDao()))
            val approvalGate = RealApprovalGate(approvalService)
            val autoDeny =
                launch {
                    approvalService.pending.collect { req -> if (req != null) approvalService.resolve(req.id, false) }
                }

            val planJson =
                """[{"action":"launchApp","target":"com.android.settings","expected":"Settings open","sideEffect":true}]"""
            val router = ProviderRouter(listOf(scriptedProvider(planJson)), noopScoreStore, alwaysCanSpend)

            val executed = mutableListOf<String>()
            val executor =
                ActionExecutor { step: PlanStep ->
                    executed += step.action
                    ExecutionOutcome(success = true, observation = "ok")
                }

            val factory = AionGraphFactory(checkpointer, emptyMemoryStore)
            val graph = factory.create(router, pluginManagerWith(executor), approvalGate)

            val result = graph.run(com.aion.brain.AgentState(goal = "open settings"))
            testScheduler.advanceUntilIdle()
            autoDeny.cancel()

            assertEquals(emptyList<String>(), executed)
            assertTrue(result.done)
            assertTrue(result.failures.any { it.contains("denied") })
        }

    @Test
    fun `T-133 - the same recoverable failure recurring gets a reflector retry every time, not just the first`() =
        runTest {
            val checkpointer = RoomCheckpointer(FakeGraphCheckpointDao())
            checkpointer.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

            val approvalGate = RealApprovalGate(ApprovalGateService(AuditLogger(FakeAuditDao())))

            val planJson =
                """[{"action":"tap","target":"Wi-Fi toggle","expected":"Wi-Fi on","sideEffect":false}]"""
            val router = ProviderRouter(listOf(scriptedProvider(planJson)), noopScoreStore, alwaysCanSpend)

            // Fails with the exact same E1_WRONG_ELEMENT-classifiable message twice in a row, then
            // succeeds on the third attempt. Under the old lastFailureCount-vs-list-size bug, the
            // SECOND occurrence would never re-trigger ReflectorAgent (failures.size resets to 1
            // both times, never exceeding the stale watermark left by the first retry) — the graph
            // would fall through to "responder" after only 2 calls, with a raw unresolved failure,
            // and the 3rd (successful) attempt would never happen.
            var callCount = 0
            val executor =
                ActionExecutor { step: PlanStep ->
                    callCount++
                    if (callCount <= 2) {
                        ExecutionOutcome(success = false, observation = "", error = "could not resolve element: Wi-Fi toggle")
                    } else {
                        ExecutionOutcome(success = true, observation = "ok:${step.action}")
                    }
                }

            val factory = AionGraphFactory(checkpointer, emptyMemoryStore)
            val graph = factory.create(router, pluginManagerWith(executor), approvalGate)

            val result = graph.run(com.aion.brain.AgentState(goal = "turn on wifi"))
            testScheduler.advanceUntilIdle()

            assertEquals("expected 2 failing attempts + 1 successful retry", 3, callCount)
            assertTrue(result.done)
            assertEquals(ResponsePhrasing.forSuccess(hinglish = false), result.response)
        }
}
