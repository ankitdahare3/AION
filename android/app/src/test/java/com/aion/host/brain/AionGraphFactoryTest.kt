package com.aion.host.brain

import com.aion.brain.ActionExecutor
import com.aion.brain.BrainRequest
import com.aion.brain.BrainResult
import com.aion.brain.BudgetGuard
import com.aion.brain.ExecutionOutcome
import com.aion.brain.PlanStep
import com.aion.brain.PluginApprovalGate
import com.aion.brain.PluginManager
import com.aion.brain.Provider
import com.aion.brain.ProviderCaps
import com.aion.brain.ProviderFailure
import com.aion.brain.ProviderRouter
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

            val factory = AionGraphFactory(checkpointer)
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

            val factory = AionGraphFactory(checkpointer)
            val graph = factory.create(router, pluginManagerWith(executor), approvalGate)

            val result = graph.run(com.aion.brain.AgentState(goal = "open settings"))
            testScheduler.advanceUntilIdle()
            autoDeny.cancel()

            assertEquals(emptyList<String>(), executed)
            assertTrue(result.done)
            assertTrue(result.failures.any { it.contains("denied") })
        }
}
