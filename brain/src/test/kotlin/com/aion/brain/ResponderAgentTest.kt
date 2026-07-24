package com.aion.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponderAgentTest {
    @Test
    fun `existing response is preserved, done is left for memory_writer to set`() =
        runTest {
            val result = ResponderAgent().step(AgentState(goal = "g", response = "already set", done = false))

            assertEquals("already set", result.response)
            assertFalse("responder must not set done — memory_writer is the real terminal node", result.done)
        }

    @Test
    fun `failures produce a natural reply, never the raw failure string`() =
        runTest {
            val result =
                ResponderAgent().step(
                    AgentState(goal = "g", failures = listOf("could not resolve element: Save button")),
                )

            assertFalse(result.done)
            assertEquals(ResponsePhrasing.forFailure(FailureCause.E1_WRONG_ELEMENT, hinglish = false), result.response)
            assertFalse(result.response!!.contains("Save button"))
            assertFalse(result.response!!.contains("E1_WRONG_ELEMENT"))
        }

    @Test
    fun `a Hinglish goal's failure gets a Hinglish reply, matching the language it was asked in`() =
        runTest {
            val result =
                ResponderAgent().step(
                    AgentState(goal = "wifi on karo", failures = listOf("could not resolve element: Wi-Fi toggle")),
                )

            assertEquals(ResponsePhrasing.forFailure(FailureCause.E1_WRONG_ELEMENT, hinglish = true), result.response)
        }

    @Test
    fun `a clean run with no failures gets a natural done response, not a raw goal echo`() =
        runTest {
            val result = ResponderAgent().step(AgentState(goal = "turn on wifi"))

            assertFalse(result.done)
            assertEquals(ResponsePhrasing.forSuccess(hinglish = false), result.response)
            assertFalse(result.response!!.contains("turn on wifi"))
        }

    @Test
    fun `a clean Hinglish-goal run gets a Hinglish done response`() =
        runTest {
            val result = ResponderAgent().step(AgentState(goal = "wifi on karo"))

            assertEquals(ResponsePhrasing.forSuccess(hinglish = true), result.response)
        }
}

class MemoryWriterAgentTest {
    @Test
    fun `with no memoryStore, is an honest no-op that just marks the run done`() =
        runTest {
            val s = AgentState(goal = "g", response = "hi")

            val result = MemoryWriterAgent().step(s)

            assertEquals(s.response, result.response)
            assertTrue(result.done)
        }

    @Test
    fun `with a real memoryStore, writes a real Memory row and marks the run done`() =
        runTest {
            val store = FakeMemoryStore()
            val s = AgentState(goal = "wifi on karo", response = "Ho gaya! Kaam ho gaya.")

            val result = MemoryWriterAgent(store).step(s)

            assertTrue(result.done)
            assertEquals(1, store.inserted.size)
            val memory = store.inserted.single()
            assertEquals(MemoryKind.FACT, memory.kind)
            assertEquals(MemoryWriterAgent.PROVENANCE, memory.provenance)
            assertTrue(memory.text.contains("wifi on karo"))
            assertTrue(memory.text.contains("Ho gaya"))
        }

    @Test
    fun `a run that recovered from failures along the way is remembered with lower confidence`() =
        runTest {
            val store = FakeMemoryStore()
            val s = AgentState(goal = "g", response = "done", failures = listOf("could not resolve element: x"))

            MemoryWriterAgent(store).step(s)

            assertEquals(0.6, store.inserted.single().confidence, 0.0001)
        }

    @Test
    fun `buildMemory is pure and deterministic given the same timestamp`() {
        val s = AgentState(goal = "g", response = "done")
        val memory = MemoryWriterAgent.buildMemory(s, nowMs = 1_000L)

        assertEquals(1_000L, memory.created)
        assertEquals(1_000L, memory.accessed)
        assertEquals(0.9, memory.confidence, 0.0001)
    }
}

private class FakeMemoryStore : MemoryStore {
    val inserted = mutableListOf<Memory>()

    override suspend fun insert(memory: Memory): Long {
        inserted += memory
        return inserted.size.toLong()
    }

    override suspend fun getAllActive(): List<Memory> = inserted

    override suspend fun update(memory: Memory) {}

    override suspend fun softDelete(id: Long) {}
}
