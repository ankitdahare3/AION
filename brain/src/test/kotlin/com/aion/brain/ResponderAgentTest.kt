package com.aion.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponderAgentTest {
    @Test
    fun `existing response is preserved`() =
        runTest {
            val result = ResponderAgent().step(AgentState(goal = "g", response = "already set", done = false))

            assertEquals("already set", result.response)
            assertTrue(result.done)
        }

    @Test
    fun `failures produce an honest error response when none was set`() =
        runTest {
            val result = ResponderAgent().step(AgentState(goal = "g", failures = listOf("E1_WRONG_ELEMENT: nope")))

            assertTrue(result.response!!.contains("nope"))
            assertTrue(result.done)
        }

    @Test
    fun `a clean run with no failures gets a default done response`() =
        runTest {
            val result = ResponderAgent().step(AgentState(goal = "turn on wifi"))

            assertTrue(result.response!!.contains("turn on wifi"))
            assertTrue(result.done)
        }
}

class MemoryWriterAgentTest {
    @Test
    fun `is an honest no-op stub that just marks the run done`() =
        runTest {
            val s = AgentState(goal = "g", response = "hi")

            val result = MemoryWriterAgent().step(s)

            assertEquals(s.response, result.response)
            assertTrue(result.done)
        }
}
