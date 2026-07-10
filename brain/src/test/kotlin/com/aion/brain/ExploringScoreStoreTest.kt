package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Test

private val fakeDelegate =
    object : ScoreStore {
        override fun taskScore(
            id: String,
            t: TaskType,
        ) = 0.3

        override fun latencyNorm(id: String) = 0.4

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

class ExploringScoreStoreTest {
    @Test
    fun `below epsilon returns the max score instead of the delegate's real score`() {
        val store = ExploringScoreStore(fakeDelegate, epsilon = 0.05, random = { 0.01 })

        assertEquals(1.0, store.taskScore("p1", TaskType.CHAT), 1e-9)
    }

    @Test
    fun `at or above epsilon returns the delegate's real score unchanged`() {
        val store = ExploringScoreStore(fakeDelegate, epsilon = 0.05, random = { 0.05 })

        assertEquals(0.3, store.taskScore("p1", TaskType.CHAT), 1e-9)
    }

    @Test
    fun `latencyNorm and notInCooldown always delegate, exploration never touches them`() {
        val store = ExploringScoreStore(fakeDelegate, epsilon = 0.05, random = { 0.0 })

        assertEquals(0.4, store.latencyNorm("p1"), 1e-9)
        assertEquals(true, store.notInCooldown("p1"))
    }

    @Test
    fun `recordSuccess and recordFailure always pass through to the delegate`() {
        val delegate =
            object : ScoreStore {
                var successCalls = 0
                var failureCalls = 0

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
                ) {
                    successCalls++
                }

                override fun recordFailure(
                    id: String,
                    t: TaskType,
                    e: ProviderFailure,
                ) {
                    failureCalls++
                }
            }
        val store = ExploringScoreStore(delegate, epsilon = 0.05, random = { 0.5 })

        store.recordSuccess("p1", TaskType.CHAT, 100, 0.0)
        store.recordFailure("p1", TaskType.CHAT, ProviderFailure.Server("boom"))

        assertEquals(1, delegate.successCalls)
        assertEquals(1, delegate.failureCalls)
    }
}
