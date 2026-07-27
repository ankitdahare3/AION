package com.aion.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun memory(
    text: String,
    kind: MemoryKind = MemoryKind.FACT,
    created: Long = 0,
) = Memory(
    kind = kind,
    text = text,
    confidence = 1.0,
    provenance = "test",
    created = created,
    accessed = 0,
    decayScore = 1.0,
)

private fun storeOf(vararg memories: Memory) =
    object : MemoryStore {
        override suspend fun insert(memory: Memory) = 0L

        override suspend fun getAllActive(): List<Memory> = memories.toList()

        override suspend fun update(memory: Memory) {}

        override suspend fun softDelete(id: Long) {}

        // Deliberately NOT overridden — exercises the interface's own default (sort+take over
        // getAllActive()), same as every existing test fake in this codebase would get for free.
    }

class RelevantMemoriesTest {
    @Test
    fun `ranks by relevance to the query, not just recency`() =
        runTest {
            val store =
                storeOf(
                    memory("owner's flight leaves at 9am tomorrow", created = 1),
                    memory("owner likes extra spicy food", created = 100), // newer, irrelevant
                )

            val result = RelevantMemories.find(store, "when is my flight")

            assertEquals("owner's flight leaves at 9am tomorrow", result.first().text)
        }

    @Test
    fun `PROFILE memories are excluded — that's PlannerAgent's own known-apps grounding`() =
        runTest {
            val store =
                storeOf(
                    memory("App com.google.android.youtube: home feed", kind = MemoryKind.PROFILE),
                    memory("owner's flight leaves at 9am"),
                )

            val result = RelevantMemories.find(store, "flight")

            assertTrue(result.none { it.kind == MemoryKind.PROFILE })
        }

    @Test
    fun `a null memoryStore returns empty, not a crash`() =
        runTest {
            val result = RelevantMemories.find(null, "anything")

            assertTrue(result.isEmpty())
        }

    @Test
    fun `an empty store returns empty`() =
        runTest {
            val result = RelevantMemories.find(storeOf(), "anything")

            assertTrue(result.isEmpty())
        }

    @Test
    fun `caps at MAX_RESULTS even when more memories match`() =
        runTest {
            val many = (1..20).map { memory("owner mentioned coffee preference number $it") }
            val store = storeOf(*many.toTypedArray())

            val result = RelevantMemories.find(store, "coffee preference")

            assertEquals(RelevantMemories.MAX_RESULTS, result.size)
        }
}
