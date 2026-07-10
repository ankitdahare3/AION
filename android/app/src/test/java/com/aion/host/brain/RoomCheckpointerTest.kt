package com.aion.host.brain

import com.aion.brain.AgentState
import com.aion.brain.PlanStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-053 AC — graph checkpoints persisted. */
class RoomCheckpointerTest {
    @Test
    fun `save persists a snapshot of the state`() =
        runTest {
            val dao = FakeGraphCheckpointDao()
            val checkpointer = RoomCheckpointer(dao)
            checkpointer.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

            val state =
                AgentState(
                    goal = "wifi on karo",
                    plan = listOf(PlanStep("tap", "Wi-Fi", "Wi-Fi on", false)),
                    currentStep = 1,
                    toolResults = listOf("ok"),
                    stepCount = 1,
                )
            checkpointer.save(state)
            testScheduler.advanceUntilIdle()

            assertEquals(1, dao.saved.size)
            val saved = dao.saved.single()
            assertEquals("wifi on karo", saved.goal)
            assertEquals(1, saved.currentStep)
            assertTrue(saved.planSummary.contains("tap:Wi-Fi"))
            assertEquals(1, saved.toolResultsCount)
        }

    @Test
    fun `multiple saves for the same goal accumulate a history`() =
        runTest {
            val dao = FakeGraphCheckpointDao()
            val checkpointer = RoomCheckpointer(dao)
            checkpointer.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

            checkpointer.save(AgentState(goal = "g", currentStep = 0))
            checkpointer.save(AgentState(goal = "g", currentStep = 1))
            checkpointer.save(AgentState(goal = "other", currentStep = 0))
            testScheduler.advanceUntilIdle()

            assertEquals(2, dao.getForGoal("g").size)
            assertEquals(1, dao.getForGoal("other").size)
        }
}
