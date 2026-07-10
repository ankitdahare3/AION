package com.aion.brain.plugins

import com.aion.brain.ActionExecutor
import com.aion.brain.AionPlugin
import com.aion.brain.DNAValidator
import com.aion.brain.ExecutionOutcome
import com.aion.brain.PlanStep
import com.aion.brain.ToolCall
import com.aion.brain.ValidationResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-071..076 AC — each built-in ships a bench yaml file (see the bench directory) and a QA scenario proving its tools actually dispatch. */
class BuiltInPluginsTest {
    private fun recordingExecutor(calls: MutableList<PlanStep>) =
        ActionExecutor { step ->
            calls += step
            ExecutionOutcome(success = true, observation = "ok:${step.action}:${step.target}")
        }

    private fun plugins(executor: ActionExecutor): List<AionPlugin> =
        listOf(
            SystemPlugin(executor),
            ContactsPlugin(executor),
            PhoneSmsPlugin(executor),
            CalendarPlugin(executor),
            FilesPlugin(executor),
            BrowserPlugin(executor),
        )

    @Test
    fun `every built-in manifest passes DNA validation`() {
        val calls = mutableListOf<PlanStep>()
        for (plugin in plugins(recordingExecutor(calls))) {
            val result = DNAValidator.validate(plugin.manifest)
            assertTrue("${plugin.manifest.id} failed validation: $result", result is ValidationResult.Valid)
        }
    }

    @Test
    fun `every declared tool dispatches a PlanStep matching its declared sideEffect flag`() =
        runTest {
            val calls = mutableListOf<PlanStep>()
            for (plugin in plugins(recordingExecutor(calls))) {
                for (tool in plugin.manifest.tools) {
                    calls.clear()
                    val result = plugin.execute(ToolCall(tool.name, "{}", tool.sideEffect))

                    assertTrue("${plugin.manifest.id}.${tool.name} did not succeed: $result", result.success)
                    assertEquals("${plugin.manifest.id}.${tool.name} dispatched wrong number of steps", 1, calls.size)
                    assertEquals(
                        "${plugin.manifest.id}.${tool.name}'s PlanStep.sideEffect must match the tool's declared sideEffect",
                        tool.sideEffect,
                        calls.single().sideEffect,
                    )
                }
            }
        }

    @Test
    fun `an unknown tool name on any built-in is rejected, not silently ignored`() =
        runTest {
            val calls = mutableListOf<PlanStep>()
            for (plugin in plugins(recordingExecutor(calls))) {
                calls.clear()
                val result = plugin.execute(ToolCall("definitely_not_a_real_tool", "{}", false))

                assertFalse("${plugin.manifest.id} should have rejected an unknown tool", result.success)
                assertTrue(calls.isEmpty())
                assertTrue(result.error?.contains("definitely_not_a_real_tool") == true)
            }
        }

    @Test
    fun `system plugin toggle_wifi taps the Wi-Fi element as a side-effect step`() =
        runTest {
            val calls = mutableListOf<PlanStep>()
            SystemPlugin(recordingExecutor(calls)).execute(ToolCall("toggle_wifi", "{}", true))

            val step = calls.single()
            assertEquals("tap", step.action)
            assertEquals("Wi-Fi", step.target)
            assertTrue(step.sideEffect)
        }

    @Test
    fun `contacts plugin start_new_contact requires contacts to already be open first`() =
        runTest {
            val calls = mutableListOf<PlanStep>()
            ContactsPlugin(recordingExecutor(calls)).execute(ToolCall("start_new_contact", "{}", false))

            assertEquals("Create new contact", calls.single().target)
        }
}
