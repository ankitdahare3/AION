package com.aion.brain

import com.aion.brain.plugins.UIAutomationPlugin
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun managerWith(executor: ActionExecutor): PluginManager {
    val manager = PluginManager(PluginApprovalGate { _, _ -> true })
    manager.register(UIAutomationPlugin(executor))
    manager.enable(UIAutomationPlugin.ID)
    return manager
}

private fun skill(steps: List<SkillStep>) =
    Skill(
        id = "skill.test/v1",
        trigger = SkillTrigger(examples = listOf("do the thing")),
        steps = steps,
        provenance = Provenance(generatedBy = "SkillGenerator"),
    )

class SkillExecutorTest {
    @Test
    fun `all steps succeeding produces Success`() =
        runTest {
            val calls = mutableListOf<PlanStep>()
            val executor =
                SkillExecutor(
                    managerWith(
                        ActionExecutor { s ->
                            calls.add(s)
                            ExecutionOutcome(success = true, observation = "ok")
                        },
                    ),
                )
            val skill =
                skill(
                    listOf(
                        SkillStep(tool = "launchApp", args = mapOf("target" to "com.example")),
                        SkillStep(tool = "tap"),
                    ),
                )

            val result = executor.execute(skill, UIAutomationPlugin.ID)

            assertTrue(result is SkillRunResult.Success)
            assertEquals(2, calls.size)
        }

    @Test
    fun `a failing step returns Failed with its index and stops before later steps`() =
        runTest {
            val calls = mutableListOf<PlanStep>()
            val executor =
                SkillExecutor(
                    managerWith(
                        ActionExecutor { s ->
                            calls.add(s)
                            if (s.action == "tap") ExecutionOutcome(false, "", "boom") else ExecutionOutcome(true, "ok")
                        },
                    ),
                )
            val skill =
                skill(listOf(SkillStep(tool = "launchApp"), SkillStep(tool = "tap"), SkillStep(tool = "globalAction")))

            val result = executor.execute(skill, UIAutomationPlugin.ID)

            check(result is SkillRunResult.Failed)
            assertEquals(1, result.failedStep)
            assertEquals(2, calls.size) // launchApp + tap, globalAction never reached
        }

    @Test
    fun `an approval marker sets sideEffect true on the following step`() =
        runTest {
            val calls = mutableListOf<PlanStep>()
            val executor =
                SkillExecutor(
                    managerWith(
                        ActionExecutor { s ->
                            calls.add(s)
                            ExecutionOutcome(success = true, observation = "ok")
                        },
                    ),
                )
            val skill = skill(listOf(SkillStep(approval = "required"), SkillStep(tool = "tap")))

            val result = executor.execute(skill, UIAutomationPlugin.ID)

            assertTrue(result is SkillRunResult.Success)
            assertEquals(1, calls.size)
            assertTrue(calls.single().sideEffect)
        }

    @Test
    fun `a tool step not preceded by approval is not marked as a side effect`() =
        runTest {
            val calls = mutableListOf<PlanStep>()
            val executor =
                SkillExecutor(
                    managerWith(
                        ActionExecutor { s ->
                            calls.add(s)
                            ExecutionOutcome(success = true, observation = "ok")
                        },
                    ),
                )
            val skill = skill(listOf(SkillStep(tool = "tap")))

            executor.execute(skill, UIAutomationPlugin.ID)

            assertTrue(!calls.single().sideEffect)
        }
}
