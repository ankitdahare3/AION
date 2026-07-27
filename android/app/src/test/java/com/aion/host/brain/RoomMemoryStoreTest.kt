package com.aion.host.brain

import com.aion.brain.Memory
import com.aion.brain.MemoryKind
import com.aion.host.memory.MemoryDao
import com.aion.host.memory.MemoryEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeMemoryDao : MemoryDao {
    val rows = mutableListOf<MemoryEntity>()
    private var nextId = 1L

    override suspend fun insert(memory: MemoryEntity): Long {
        val withId = memory.copy(id = nextId++)
        rows += withId
        return withId.id
    }

    override suspend fun getAllActive(): List<MemoryEntity> =
        rows
            .filterNot {
                it.deletedSoft
            }.sortedByDescending { it.created }

    override suspend fun getRecentActive(limit: Int): List<MemoryEntity> = getAllActive().take(limit)

    override suspend fun update(memory: MemoryEntity) {
        val i = rows.indexOfFirst { it.id == memory.id }
        rows[i] = memory
    }

    override suspend fun softDelete(id: Long) {
        val i = rows.indexOfFirst { it.id == id }
        rows[i] = rows[i].copy(deletedSoft = true)
    }
}

class RoomMemoryStoreTest {
    @Test
    fun `an inserted memory round-trips through getAllActive`() =
        runTest {
            val store = RoomMemoryStore(FakeMemoryDao())
            store.insert(
                Memory(
                    kind = MemoryKind.FACT,
                    text = "user's name is Ankit",
                    confidence = 0.8,
                    provenance = "chat",
                    piiTags = listOf("name"),
                    created = 100,
                    accessed = 100,
                    decayScore = 1.0,
                ),
            )

            val active = store.getAllActive()

            assertEquals(1, active.size)
            assertEquals("user's name is Ankit", active.single().text)
            assertEquals(listOf("name"), active.single().piiTags)
        }

    @Test
    fun `update persists changes made by consolidation`() =
        runTest {
            val dao = FakeMemoryDao()
            val store = RoomMemoryStore(dao)
            val id =
                store.insert(
                    Memory(
                        kind = MemoryKind.PREF,
                        text = "formal tone",
                        confidence = 0.5,
                        provenance = "chat",
                        created = 0,
                        accessed = 0,
                        decayScore = 1.0,
                    ),
                )

            store.update(store.getAllActive().single().copy(confidence = 0.9))

            assertEquals(0.9, store.getAllActive().single().confidence, 1e-9)
            assertEquals(id, store.getAllActive().single().id)
        }

    @Test
    fun `soft-deleted memories drop out of getAllActive`() =
        runTest {
            val store = RoomMemoryStore(FakeMemoryDao())
            val id =
                store.insert(
                    Memory(
                        kind = MemoryKind.FACT,
                        text = "wifi password is hunter2",
                        confidence = 0.9,
                        provenance = "chat",
                        created = 0,
                        accessed = 0,
                        decayScore = 1.0,
                    ),
                )

            store.softDelete(id)

            assertTrue(store.getAllActive().isEmpty())
        }

    // T-178 — getRecentActive is meant to be a real bound at the query layer (BACKLOG.md's T-173
    // finding), not getAllActive() + in-app truncation, so it must actually cap the result size.
    @Test
    fun `getRecentActive returns at most the requested limit, newest first`() =
        runTest {
            val store = RoomMemoryStore(FakeMemoryDao())
            repeat(5) { i ->
                store.insert(
                    Memory(
                        kind = MemoryKind.FACT,
                        text = "memory $i",
                        confidence = 1.0,
                        provenance = "chat",
                        created = i.toLong(),
                        accessed = 0,
                        decayScore = 1.0,
                    ),
                )
            }

            val recent = store.getRecentActive(2)

            assertEquals(2, recent.size)
            assertEquals("memory 4", recent[0].text)
            assertEquals("memory 3", recent[1].text)
        }
}
