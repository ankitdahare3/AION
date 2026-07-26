package com.aion.brain.plugins

import com.aion.brain.ActionExecutor
import com.aion.brain.ExecutionOutcome
import com.aion.brain.PlanStep
import com.aion.brain.ToolCall
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PalmClaw-inspired direct-device-tool actions (callContact/sendSms/openUrl/searchWeb) round-trip
 * through [UIAutomationPlugin.argsJsonFor] -> [ExecutorAgent]'s [ToolCall.argsJson] -> here -> a
 * [PlanStep] the real [ActionExecutor] (DispatcherActionExecutor in :android:app) executes. The
 * one field that's new and easy to silently drop across that round trip is [PlanStep.extra]
 * (sendSms's message body) — these tests exist specifically to catch that.
 */
class UIAutomationPluginTest {
    @Test
    fun `argsJsonFor and execute round-trip extra for sendSms`() =
        runTest {
            var received: PlanStep? = null
            val plugin =
                UIAutomationPlugin(
                    ActionExecutor { step ->
                        received = step
                        ExecutionOutcome(success = true, observation = "ok")
                    },
                )
            val original = PlanStep("sendSms", "+911234567890", "Messages open", false, "running late, be there in 10")

            plugin.execute(
                ToolCall(name = "sendSms", argsJson = UIAutomationPlugin.argsJsonFor(original), sideEffect = false),
            )

            assertEquals("sendSms", received?.action)
            assertEquals("+911234567890", received?.target)
            assertEquals("running late, be there in 10", received?.extra)
        }

    @Test
    fun `a step with no extra round-trips as null, not an empty string`() =
        runTest {
            var received: PlanStep? = null
            val plugin =
                UIAutomationPlugin(
                    ActionExecutor { step ->
                        received = step
                        ExecutionOutcome(success = true, observation = "ok")
                    },
                )
            val original = PlanStep("openUrl", "https://example.com", "browser open", false)

            plugin.execute(
                ToolCall(name = "openUrl", argsJson = UIAutomationPlugin.argsJsonFor(original), sideEffect = false),
            )

            assertEquals("https://example.com", received?.target)
            assertNull(received?.extra)
        }

    @Test
    fun `the new direct-tool action names are declared in the manifest, not just handled ad-hoc`() {
        val plugin = UIAutomationPlugin(ActionExecutor { ExecutionOutcome(success = true, observation = "") })
        val names = plugin.manifest.tools.map { it.name }

        assertTrue(names.containsAll(listOf("callContact", "sendSms", "openUrl", "searchWeb")))
    }
}
