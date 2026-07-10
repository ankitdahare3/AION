package com.aion.host.brain

import com.aion.brain.FailureCause
import com.aion.brain.ReflectionRecord
import com.aion.brain.TaskOutcome
import com.aion.host.memory.EpisodeDao
import com.aion.host.memory.EpisodeEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeEpisodeDao : EpisodeDao {
    val rows = mutableListOf<EpisodeEntity>()

    override suspend fun insert(episode: EpisodeEntity) {
        rows += episode
    }

    override suspend fun getAll(): List<EpisodeEntity> = rows.sortedByDescending { it.ts }
}

class RoomReflectionRecordStoreTest {
    @Test
    fun `a recorded success round-trips with no failure cause`() =
        runTest {
            val store = RoomReflectionRecordStore(FakeEpisodeDao())
            store.record(
                ReflectionRecord(
                    goal = "wifi on karo",
                    planJson = "[]",
                    outcome = TaskOutcome.SUCCESS,
                    failureCause = null,
                    latencyMs = 500,
                    costUsd = 0.0,
                    appPkg = "com.android.settings",
                ),
            )

            val recent = store.recent(10)

            assertEquals(1, recent.size)
            assertEquals(TaskOutcome.SUCCESS, recent.single().outcome)
            assertNull(recent.single().failureCause)
        }

    @Test
    fun `a recorded failure round-trips its classified cause`() =
        runTest {
            val store = RoomReflectionRecordStore(FakeEpisodeDao())
            store.record(
                ReflectionRecord(
                    goal = "open settings",
                    planJson = "[]",
                    outcome = TaskOutcome.FAILURE,
                    failureCause = FailureCause.E2_UI_CHANGED,
                    latencyMs = 200,
                    costUsd = 0.0,
                    appPkg = "com.android.settings",
                ),
            )

            assertEquals(FailureCause.E2_UI_CHANGED, store.recent(10).single().failureCause)
        }

    @Test
    fun `recent respects the limit`() =
        runTest {
            val dao = FakeEpisodeDao()
            val store = RoomReflectionRecordStore(dao)
            repeat(5) {
                store.record(
                    ReflectionRecord("g$it", "[]", TaskOutcome.SUCCESS, null, 0, 0.0, null),
                )
            }

            assertEquals(3, store.recent(3).size)
        }
}
