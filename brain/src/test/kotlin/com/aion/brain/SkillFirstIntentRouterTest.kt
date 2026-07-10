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

private fun skill(
    id: String,
    trigger: String,
    steps: List<SkillStep>,
) = Skill(
    id = id,
    trigger = SkillTrigger(examples = listOf(trigger)),
    steps = steps,
    provenance = Provenance(generatedBy = "SkillGenerator"),
)

/** T-093 AC — matched skill skips planner; failure falls back. */
class SkillFirstIntentRouterTest {
    @Test
    fun `a matched, successful skill skips the planner entirely`() =
        runTest {
            var plannerCalls = 0
            val router =
                SkillFirstIntentRouter(
                    SkillExecutor(managerWith(ActionExecutor { ExecutionOutcome(success = true, observation = "ok") })),
                    UIAutomationPlugin.ID,
                )
            val skills = listOf(skill("skill.wifi", "wifi on karo", listOf(SkillStep(tool = "tap"))))

            val outcome =
                router.run("wifi on karo", skills) {
                    plannerCalls++
                    AgentState(goal = "wifi on karo")
                }

            assertTrue(outcome is SkillFirstOutcome.SkilledDone)
            assertEquals(0, plannerCalls)
            assertTrue((outcome as SkillFirstOutcome.SkilledDone).state.done)
        }

    @Test
    fun `no matching skill falls back to the planner`() =
        runTest {
            var plannerCalls = 0
            val router =
                SkillFirstIntentRouter(
                    SkillExecutor(managerWith(ActionExecutor { ExecutionOutcome(success = true, observation = "ok") })),
                    UIAutomationPlugin.ID,
                )

            val outcome =
                router.run("something totally unrelated", emptyList()) {
                    plannerCalls++
                    AgentState(goal = "x")
                }

            assertTrue(outcome is SkillFirstOutcome.FellBackToPlanner)
            assertEquals(1, plannerCalls)
        }

    @Test
    fun `a skill that fails partway falls back to the planner`() =
        runTest {
            var plannerCalls = 0
            val router =
                SkillFirstIntentRouter(
                    SkillExecutor(
                        managerWith(
                            ActionExecutor { ExecutionOutcome(success = false, observation = "", error = "boom") },
                        ),
                    ),
                    UIAutomationPlugin.ID,
                )
            val skills = listOf(skill("skill.wifi", "wifi on karo", listOf(SkillStep(tool = "tap"))))

            val outcome =
                router.run("wifi on karo", skills) {
                    plannerCalls++
                    AgentState(goal = "wifi on karo", response = "planner ran")
                }

            check(outcome is SkillFirstOutcome.FellBackToPlanner)
            assertEquals(1, plannerCalls)
            assertTrue(outcome.reason.contains("boom"))
            assertEquals("planner ran", outcome.state.response)
        }
}
