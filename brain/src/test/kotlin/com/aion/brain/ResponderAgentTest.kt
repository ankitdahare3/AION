package com.aion.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `failures produce a natural reply, never the raw failure string`() =
        runTest {
            val result = ResponderAgent().step(AgentState(goal = "g", failures = listOf("could not resolve element: Save button")))

            assertTrue(result.done)
            assertEquals(ResponsePhrasing.forFailure(FailureCause.E1_WRONG_ELEMENT, hinglish = false), result.response)
            assertFalse(result.response!!.contains("Save button"))
            assertFalse(result.response!!.contains("E1_WRONG_ELEMENT"))
        }

    @Test
    fun `a Hinglish goal's failure gets a Hinglish reply, matching the language it was asked in`() =
        runTest {
            val result = ResponderAgent().step(AgentState(goal = "wifi on karo", failures = listOf("could not resolve element: Wi-Fi toggle")))

            assertEquals(ResponsePhrasing.forFailure(FailureCause.E1_WRONG_ELEMENT, hinglish = true), result.response)
        }

    @Test
    fun `a clean run with no failures gets a natural done response, not a raw goal echo`() =
        runTest {
            val result = ResponderAgent().step(AgentState(goal = "turn on wifi"))

            assertTrue(result.done)
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
    fun `is an honest no-op stub that just marks the run done`() =
        runTest {
            val s = AgentState(goal = "g", response = "hi")

            val result = MemoryWriterAgent().step(s)

            assertEquals(s.response, result.response)
            assertTrue(result.done)
        }
}
