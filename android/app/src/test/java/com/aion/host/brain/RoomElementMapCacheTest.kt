package com.aion.host.brain

import com.aion.brain.FailureCause
import com.aion.host.memory.ElementMapDao
import com.aion.host.memory.ElementMapEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeElementMapDao : ElementMapDao {
    val rows = mutableMapOf<Triple<String, String, String>, ElementMapEntity>()

    override suspend fun upsert(elementMap: ElementMapEntity) {
        rows[Triple(elementMap.appPkg, elementMap.appVersion, elementMap.screenHash)] = elementMap
    }

    override suspend fun getForAppVersion(
        appPkg: String,
        appVersion: String,
    ): List<ElementMapEntity> = rows.values.filter { it.appPkg == appPkg && it.appVersion == appVersion }

    override suspend fun getOne(
        appPkg: String,
        appVersion: String,
        screenHash: String,
    ): ElementMapEntity? = rows[Triple(appPkg, appVersion, screenHash)]
}

/** T-080 AC — E2 (UI changed) auto-invalidates the cached selector. */
class RoomElementMapCacheTest {
    @Test
    fun `an unknown screen has no cached selector`() =
        runTest {
            val cache = RoomElementMapCache(FakeElementMapDao())

            assertNull(cache.get("com.example.app", "1.0", "screen1"))
        }

    @Test
    fun `success rewards confidence via EMA toward 1_0`() =
        runTest {
            val cache = RoomElementMapCache(FakeElementMapDao())

            cache.recordOutcome("com.example.app", "1.0", "screen1", """{"id":"btn1"}""", cause = null)
            val first = cache.get("com.example.app", "1.0", "screen1")!!.confidence

            cache.recordOutcome("com.example.app", "1.0", "screen1", """{"id":"btn1"}""", cause = null)
            val second = cache.get("com.example.app", "1.0", "screen1")!!.confidence

            assertTrue("confidence should climb toward 1.0 with repeated success: $first -> $second", second > first)
        }

    @Test
    fun `E2 UI-changed invalidates the cached selector to zero confidence`() =
        runTest {
            val cache = RoomElementMapCache(FakeElementMapDao())
            cache.recordOutcome("com.example.app", "1.0", "screen1", """{"id":"btn1"}""", cause = null)
            cache.recordOutcome("com.example.app", "1.0", "screen1", """{"id":"btn1"}""", cause = null)
            val beforeInvalidation = cache.get("com.example.app", "1.0", "screen1")!!.confidence
            assertTrue(beforeInvalidation > 0.0)

            cache.recordOutcome(
                "com.example.app",
                "1.0",
                "screen1",
                """{"id":"btn1"}""",
                cause = FailureCause.E2_UI_CHANGED,
            )

            assertEquals(0.0, cache.get("com.example.app", "1.0", "screen1")!!.confidence, 0.0001)
        }

    @Test
    fun `a non-E2 failure leaves the cached selector untouched`() =
        runTest {
            val cache = RoomElementMapCache(FakeElementMapDao())
            cache.recordOutcome("com.example.app", "1.0", "screen1", """{"id":"btn1"}""", cause = null)
            val before = cache.get("com.example.app", "1.0", "screen1")!!.confidence

            cache.recordOutcome(
                "com.example.app",
                "1.0",
                "screen1",
                """{"id":"btn1"}""",
                cause = FailureCause.E5_PERMISSION_BLOCKED,
            )

            assertEquals(before, cache.get("com.example.app", "1.0", "screen1")!!.confidence, 0.0001)
        }

    @Test
    fun `different screens are cached independently`() =
        runTest {
            val cache = RoomElementMapCache(FakeElementMapDao())
            cache.recordOutcome("com.example.app", "1.0", "screen1", """{"id":"btn1"}""", cause = null)
            cache.recordOutcome(
                "com.example.app",
                "1.0",
                "screen2",
                """{"id":"btn2"}""",
                cause = FailureCause.E2_UI_CHANGED,
            )

            assertTrue(cache.get("com.example.app", "1.0", "screen1")!!.confidence > 0.0)
            assertEquals(0.0, cache.get("com.example.app", "1.0", "screen2")!!.confidence, 0.0001)
        }
}
