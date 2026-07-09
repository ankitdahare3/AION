package com.aion.host.automation

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * T-041 AC — pure-logic timing test. Clock and sleep are faked (sleep just advances the fake
 * clock) so this asserts exact throttling behavior with zero real wall-clock waiting.
 */
class RateLimiterTest {
    private class FakeTime {
        var now = 0L
        val slept = mutableListOf<Long>()

        fun clock(): Long = now

        suspend fun sleep(ms: Long) {
            slept += ms
            now += ms
        }
    }

    @Test
    fun `first acquire never sleeps`() =
        runBlocking {
            val time = FakeTime()
            val limiter = RateLimiter(minIntervalMs = 300, clock = time::clock, sleep = time::sleep)

            limiter.acquire()

            assertEquals(emptyList<Long>(), time.slept)
        }

    @Test
    fun `second acquire immediately after sleeps the remaining interval`() =
        runBlocking {
            val time = FakeTime()
            val limiter = RateLimiter(minIntervalMs = 300, clock = time::clock, sleep = time::sleep)

            limiter.acquire()
            time.now += 50 // only 50ms of "real" time passed before the next action
            limiter.acquire()

            assertEquals(listOf(250L), time.slept)
        }

    @Test
    fun `acquire after the interval has already elapsed does not sleep`() =
        runBlocking {
            val time = FakeTime()
            val limiter = RateLimiter(minIntervalMs = 300, clock = time::clock, sleep = time::sleep)

            limiter.acquire()
            time.now += 500 // plenty of time passed
            limiter.acquire()

            assertEquals(emptyList<Long>(), time.slept)
        }

    @Test
    fun `three rapid acquires each throttle to the configured interval`() =
        runBlocking {
            val time = FakeTime()
            val limiter = RateLimiter(minIntervalMs = 300, clock = time::clock, sleep = time::sleep)

            limiter.acquire()
            limiter.acquire()
            limiter.acquire()

            assertEquals(listOf(300L, 300L), time.slept)
        }
}
