package com.aion.host.brain

import com.aion.brain.BrainRequest
import com.aion.brain.TaskType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val req = BrainRequest(taskType = TaskType.CHAT, system = "sys", messages = emptyList())

/** T-030 AC — real BudgetGuard behavior: daily budget vs. per-task ceiling, Room-backed. */
class RoomBudgetGuardTest {
    @Test
    fun `a fresh guard allows spending`() {
        val guard = RoomBudgetGuard(FakeBudgetDao())
        assertTrue(guard.canSpend(req))
    }

    @Test
    fun `recording spend near the daily ceiling blocks further paid calls`() =
        runTest {
            val dao = FakeBudgetDao()
            val guard = RoomBudgetGuard(dao)
            guard.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

            // $0.90 spent + $0.15 per-task ceiling > $1.00 daily default (DOC-013 §4).
            guard.record(0.90)
            testScheduler.advanceUntilIdle()

            assertFalse(guard.canSpend(req))
            assertTrue(dao.days.isNotEmpty())
        }

    @Test
    fun `small spends stay within budget`() {
        val guard = RoomBudgetGuard(FakeBudgetDao())
        guard.record(0.10)
        assertTrue(guard.canSpend(req))
    }

    @Test
    fun `load restores today's spend from the dao`() =
        runTest {
            val dao = FakeBudgetDao()
            val today = System.currentTimeMillis() / 86_400_000L
            dao.upsert(BudgetDayEntity(today, 0.95))

            val guard = RoomBudgetGuard(dao)
            guard.load()

            assertFalse(guard.canSpend(req))
        }
}
