package com.aion.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectorAgentTest {
    // Induced (hand-written, realistic) failure messages -> ground-truth FR-R02 category. T-052 AC:
    // >=80% classified correctly. Deliberately includes some genuinely ambiguous/novel messages
    // (real systems produce those) so the bar is meaningful rather than trivially 100%.
    private val induced =
        listOf(
            // E1 — wrong element resolved
            "could not resolve element: Wi-Fi toggle" to FailureCause.E1_WRONG_ELEMENT,
            "element not found on current screen" to FailureCause.E1_WRONG_ELEMENT,
            "ElementResolver returned unresolved for target 'Send button'" to FailureCause.E1_WRONG_ELEMENT,
            "tap failed: target element vanished before dispatch" to FailureCause.E1_WRONG_ELEMENT,
            "resolution confidence below threshold, no match found" to FailureCause.E1_WRONG_ELEMENT,
            "no element matched query 'Settings icon'" to FailureCause.E1_WRONG_ELEMENT,
            // E2 — UI changed
            "screen changed but expected text not found" to FailureCause.E2_UI_CHANGED,
            "expected text not found in post-action screen" to FailureCause.E2_UI_CHANGED,
            "unexpected screen after action, layout differs from last known state" to FailureCause.E2_UI_CHANGED,
            "UI layout appears different from the recorded reference" to FailureCause.E2_UI_CHANGED,
            "app updated, selectors no longer match" to FailureCause.E2_UI_CHANGED,
            // E3 — OCR/vision misread
            "OCR failed to read the button label" to FailureCause.E3_VISION_MISREAD,
            "vision fallback misread the screen text" to FailureCause.E3_VISION_MISREAD,
            "screenshot analysis returned garbled text" to FailureCause.E3_VISION_MISREAD,
            "ML Kit OCR confidence too low" to FailureCause.E3_VISION_MISREAD,
            // E4 — model/plan error
            "planner: failed to produce a valid JSON plan after retry" to FailureCause.E4_MODEL_PLAN_ERROR,
            "response was not valid JSON matching the schema" to FailureCause.E4_MODEL_PLAN_ERROR,
            "invalid plan: missing required field 'target'" to FailureCause.E4_MODEL_PLAN_ERROR,
            "model output did not match the expected schema" to FailureCause.E4_MODEL_PLAN_ERROR,
            // E5 — permission/platform block
            "accessibility service not connected" to FailureCause.E5_PERMISSION_BLOCKED,
            "no launch intent for com.example.app" to FailureCause.E5_PERMISSION_BLOCKED,
            "Shizuku permission denied" to FailureCause.E5_PERMISSION_BLOCKED,
            "Shizuku unavailable" to FailureCause.E5_PERMISSION_BLOCKED,
            "app not installed on this device" to FailureCause.E5_PERMISSION_BLOCKED,
            "action not yet supported by DispatcherActionExecutor: type" to FailureCause.E5_PERMISSION_BLOCKED,
            // E6 — timing/race
            "gesture callback never fired within 5000ms" to FailureCause.E6_TIMING_RACE,
            "dispatchGesture returned false" to FailureCause.E6_TIMING_RACE,
            "performGlobalAction returned false" to FailureCause.E6_TIMING_RACE,
            "screen did not change after the action" to FailureCause.E6_TIMING_RACE,
            "action timed out waiting for response" to FailureCause.E6_TIMING_RACE,
            "gesture cancelled" to FailureCause.E6_TIMING_RACE,
            // genuinely novel/unclassifiable — real systems produce these too
            "java.lang.NullPointerException at line 42" to FailureCause.UNKNOWN,
            "unexpected error occurred" to FailureCause.UNKNOWN,
            "connection reset by peer" to FailureCause.UNKNOWN,
            "out of memory" to FailureCause.UNKNOWN,
        )

    @Test
    fun `induced failures classify correctly at least 80 percent of the time`() {
        val misses = induced.filter { (msg, expected) -> ReflectorAgent.classify(msg) != expected }
        val rate = (induced.size - misses.size).toDouble() / induced.size

        assertTrue(
            "accuracy $rate below 80%% (${induced.size - misses.size}/${induced.size}); misses:\n" +
                misses.joinToString(
                    "\n",
                ) { (msg, expected) -> "\"$msg\" expected $expected, got ${ReflectorAgent.classify(msg)}" },
            rate >= 0.80,
        )
    }

    @Test
    fun `unknown cause is never fabricated as a specific category`() {
        assertEquals(FailureCause.UNKNOWN, ReflectorAgent.classify("42"))
        assertEquals(FailureCause.UNKNOWN, ReflectorAgent.classify(""))
    }

    @Test
    fun `recoverable cause clears the plan for a fresh replan attempt`() =
        runTest {
            val s =
                AgentState(
                    goal = "g",
                    plan = listOf(PlanStep("tap", "x", "y", false)),
                    currentStep = 1,
                    failures = listOf("element not found on current screen"),
                )

            val result = ReflectorAgent().step(s)

            assertTrue(result.plan.isEmpty())
            assertEquals(0, result.currentStep)
            assertFalse(result.done)
            assertTrue(
                "failures must be cleared too, or a later success gets misreported as still-failed",
                result.failures.isEmpty(),
            )
        }

    @Test
    fun `unrecoverable cause aborts with a natural explanation, never a raw error code`() =
        runTest {
            val s = AgentState(goal = "send the report", failures = listOf("Shizuku permission denied"))

            val result = ReflectorAgent().step(s)

            assertTrue(result.done)
            assertEquals(ResponsePhrasing.forFailure(FailureCause.E5_PERMISSION_BLOCKED, hinglish = false), result.response)
            assertFalse(result.response!!.contains("E5_PERMISSION_BLOCKED"))
            assertFalse(result.response!!.contains("Shizuku"))
        }

    @Test
    fun `an unrecoverable abort for a Hinglish goal replies in Hinglish`() =
        runTest {
            val s = AgentState(goal = "WhatsApp khol ke naya message bhejo", failures = listOf("no launch intent for com.whatsapp"))

            val result = ReflectorAgent().step(s)

            assertEquals(ResponsePhrasing.forFailure(FailureCause.E5_PERMISSION_BLOCKED, hinglish = true), result.response)
        }

    @Test
    fun `T-162 - a recoverable failure records a counter-example when a FewShotBank is provided`() =
        runTest {
            val bank = FewShotBank()
            val s =
                AgentState(
                    goal = "wrong element goal",
                    plan = listOf(PlanStep("tap", "WRONG_ELEMENT", "y", false)),
                    currentStep = 1,
                    failures = listOf("could not resolve element: WRONG_ELEMENT"),
                )

            ReflectorAgent(bank).step(s)

            val examples = bank.examplesFor("wrong element goal")
            assertEquals(1, examples.size)
            assertTrue(examples[0].badPlanJson.contains("WRONG_ELEMENT"))
            assertEquals("could not resolve element: WRONG_ELEMENT", examples[0].reason)
        }

    @Test
    fun `T-162 - an unrecoverable failure does not record a counter-example (nothing to retry)`() =
        runTest {
            val bank = FewShotBank()
            val s = AgentState(goal = "send the report", failures = listOf("Shizuku permission denied"))

            ReflectorAgent(bank).step(s)

            assertTrue(bank.examplesFor("send the report").isEmpty())
        }

    @Test
    fun `no failures to reflect on ends the run rather than looping, with a natural response`() =
        runTest {
            val result = ReflectorAgent().step(AgentState(goal = "g"))

            assertTrue(result.done)
            assertEquals(ResponsePhrasing.forFailure(FailureCause.UNKNOWN, hinglish = false), result.response)
        }
}
