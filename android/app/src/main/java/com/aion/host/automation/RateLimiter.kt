package com.aion.host.automation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * DOC-009 §6 — throttles [ActionDispatcher] to at most one action per [minIntervalMs]. Dependency-free
 * from Android so it's plain-JVM unit-testable; [clock]/[sleep] are swappable for deterministic tests.
 */
class RateLimiter(
    private val minIntervalMs: Long = 300L,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sleep: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
) {
    private val mutex = Mutex()
    private var lastActionAt: Long? = null

    /** Suspends until at least [minIntervalMs] has passed since the previous [acquire] call. */
    suspend fun acquire() {
        mutex.withLock {
            val now = clock()
            val last = lastActionAt
            if (last != null) {
                val elapsed = now - last
                if (elapsed < minIntervalMs) {
                    sleep(minIntervalMs - elapsed)
                }
            }
            lastActionAt = clock()
        }
    }
}
