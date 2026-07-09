package com.aion.host.brain

import com.aion.brain.ProviderFailure
import com.aion.brain.TaskType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-030 AC — real ScoreStore behavior: cooldowns and failover-relevant scoring, Room-backed. */
class RoomScoreStoreTest {
    @Test
    fun `new provider gets the neutral default score and is not in cooldown`() {
        val store = RoomScoreStore(FakeProviderStatsDao())
        assertEquals(0.5, store.taskScore("groq", TaskType.CHAT), 1e-9)
        assertTrue(store.notInCooldown("groq"))
    }

    @Test
    fun `recordSuccess raises taskScore toward 1 and persists to the dao`() =
        runTest {
            val dao = FakeProviderStatsDao()
            val store = RoomScoreStore(dao)
            store.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

            store.recordSuccess("groq", TaskType.CHAT, latencyMs = 500, cost = 0.001)
            testScheduler.advanceUntilIdle()

            assertTrue(store.taskScore("groq", TaskType.CHAT) > 0.5)
            assertTrue(dao.stats.containsKey("groq" to "CHAT"))
        }

    @Test
    fun `auth failure puts the provider into cooldown and is persisted`() =
        runTest {
            val dao = FakeProviderStatsDao()
            val store = RoomScoreStore(dao)
            store.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

            assertTrue(store.notInCooldown("openai"))
            store.recordFailure("openai", TaskType.CHAT, ProviderFailure.Auth("bad key"))
            testScheduler.advanceUntilIdle()

            assertFalse(store.notInCooldown("openai"))
            assertTrue(dao.cooldowns.containsKey("openai"))
        }

    @Test
    fun `timeout failure lowers score but does not trigger cooldown`() {
        val store = RoomScoreStore(FakeProviderStatsDao())
        store.recordFailure("groq", TaskType.CHAT, ProviderFailure.Timeout("slow"))
        assertTrue(store.notInCooldown("groq"))
        assertTrue(store.taskScore("groq", TaskType.CHAT) < 0.5)
    }

    @Test
    fun `repeated failures drop taskScore enough to enable failover to a healthier candidate`() {
        val store = RoomScoreStore(FakeProviderStatsDao())
        repeat(5) { store.recordFailure("flaky", TaskType.CHAT, ProviderFailure.Server("500")) }
        val flakyScore = store.taskScore("flaky", TaskType.CHAT)
        val freshScore = store.taskScore("healthy", TaskType.CHAT)
        assertTrue(flakyScore < freshScore)
    }

    @Test
    fun `load restores stats and cooldowns from the dao`() =
        runTest {
            val dao = FakeProviderStatsDao()
            dao.upsertStat(ProviderTaskStatEntity("groq", "CHAT", 0.9, 400.0, 0.001, 123L))
            dao.upsertCooldown(ProviderCooldownEntity("openai", System.currentTimeMillis() + 60_000))

            val store = RoomScoreStore(dao)
            store.load()

            assertEquals(0.9, store.taskScore("groq", TaskType.CHAT), 1e-9)
            assertFalse(store.notInCooldown("openai"))
        }

    @Test
    fun `latencyNorm reflects recorded latency, faster providers score lower`() {
        val store = RoomScoreStore(FakeProviderStatsDao())
        store.recordSuccess("fast", TaskType.CHAT, latencyMs = 100, cost = 0.0)
        store.recordSuccess("slow", TaskType.CHAT, latencyMs = 4000, cost = 0.0)
        assertTrue(store.latencyNorm("fast") < store.latencyNorm("slow"))
    }
}
