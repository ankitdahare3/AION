package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressPhrasingTest {
    @Test
    fun `no plan yet reads as planning`() {
        val state = AgentState(goal = "wifi on karo")
        assertEquals("Planning...", ProgressPhrasing.describe(state, hinglish = false))
        assertEquals("Plan bana raha hoon...", ProgressPhrasing.describe(state, hinglish = true))
    }

    @Test
    fun `mid-plan reads as the current step, 1-indexed`() {
        val state =
            AgentState(
                goal = "g",
                plan = listOf(PlanStep("tap", "Wi-Fi", "Wi-Fi on", false), PlanStep("tap", "Done", "saved", false)),
                currentStep = 0,
            )
        val described = ProgressPhrasing.describe(state, hinglish = false)
        assertTrue(described.contains("1/2"))
        assertTrue(described.contains("tap"))
        assertTrue(described.contains("Wi-Fi"))
    }

    @Test
    fun `needsApproval takes priority over the step description`() {
        val state =
            AgentState(
                goal = "g",
                plan = listOf(PlanStep("send", "Ravi", "sent", true)),
                currentStep = 0,
                needsApproval = true,
            )
        assertEquals("Waiting for your approval...", ProgressPhrasing.describe(state, hinglish = false))
    }

    @Test
    fun `a fresh failure reads as retrying, not as a raw error string`() {
        val state =
            AgentState(
                goal = "g",
                plan = listOf(PlanStep("tap", "x", "y", false)),
                currentStep = 0,
                failures = listOf("could not resolve element: x"),
            )
        val described = ProgressPhrasing.describe(state, hinglish = false)
        assertEquals("That didn't work, trying a different way...", described)
        assertTrue("must never leak the raw failure string", !described.contains("resolve element"))
    }

    @Test
    fun `done overrides every other signal`() {
        val state =
            AgentState(
                goal = "g",
                plan = listOf(PlanStep("tap", "x", "y", false)),
                currentStep = 1,
                done = true,
                response = "Done! That's taken care of.",
            )
        assertEquals("Done.", ProgressPhrasing.describe(state, hinglish = false))
    }

    @Test
    fun `currentStep past the plan end falls back to finishing up`() {
        val state =
            AgentState(
                goal = "g",
                plan = listOf(PlanStep("tap", "x", "y", false)),
                currentStep = 1,
            )
        assertEquals("Finishing up...", ProgressPhrasing.describe(state, hinglish = false))
    }
}
