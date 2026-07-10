package com.aion.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun step(
    action: String,
    sideEffect: Boolean,
) = PlanStep(action = action, target = "t-$action", expected = "e-$action", sideEffect = sideEffect)

class ExecutorAgentTest {
    @Test
    fun `non-side-effect step executes immediately without requesting approval`() =
        runTest {
            val calls = mutableListOf<PlanStep>()
            val agent =
                ExecutorAgent(
                    ActionExecutor { s ->
                        calls.add(s)
                        ExecutionOutcome(success = true, observation = "ok")
                    },
                )

            val result = agent.step(AgentState(goal = "g", plan = listOf(step("tap", sideEffect = false))))

            assertFalse(result.needsApproval)
            assertEquals(1, calls.size)
            assertEquals(1, result.currentStep)
        }

    @Test
    fun `side-effect step requests approval first without executing`() =
        runTest {
            val calls = mutableListOf<PlanStep>()
            val agent =
                ExecutorAgent(
                    ActionExecutor { s ->
                        calls.add(s)
                        ExecutionOutcome(success = true, observation = "ok")
                    },
                )

            val result = agent.step(AgentState(goal = "g", plan = listOf(step("send", sideEffect = true))))

            assertTrue(result.needsApproval)
            assertEquals(0, calls.size)
            assertEquals(0, result.currentStep)
        }

    @Test
    fun `side-effect step executes on the second visit after approval is granted`() =
        runTest {
            val calls = mutableListOf<PlanStep>()
            val agent =
                ExecutorAgent(
                    ActionExecutor { s ->
                        calls.add(s)
                        ExecutionOutcome(success = true, observation = "sent")
                    },
                )
            val plan = listOf(step("send", sideEffect = true))

            val afterRequest = agent.step(AgentState(goal = "g", plan = plan))
            // AionGraph.run() clears needsApproval unconditionally right after ApprovalGate.await().
            val afterApproval = agent.step(afterRequest.copy(needsApproval = false))

            assertEquals(1, calls.size)
            assertEquals(1, afterApproval.currentStep)
            assertTrue(afterApproval.toolResults.contains("sent"))
        }

    @Test
    fun `approval fires again for a second, different side-effect step`() =
        runTest {
            val calls = mutableListOf<PlanStep>()
            val agent =
                ExecutorAgent(
                    ActionExecutor { s ->
                        calls.add(s)
                        ExecutionOutcome(success = true, observation = "ok")
                    },
                )
            val plan = listOf(step("send1", sideEffect = true), step("send2", sideEffect = true))

            var s = agent.step(AgentState(goal = "g", plan = plan))
            assertTrue(s.needsApproval) // step 0 asks
            s = agent.step(s.copy(needsApproval = false)) // step 0 executes, advances to step 1
            assertFalse(s.needsApproval)
            assertEquals(1, s.currentStep)

            s = agent.step(s)
            assertTrue(s.needsApproval) // step 1 asks again — a fresh side-effect step, not reusing step 0's approval
            s = agent.step(s.copy(needsApproval = false)) // step 1 executes

            assertEquals(2, calls.size)
            assertEquals(2, s.currentStep)
        }

    @Test
    fun `failed execution is recorded without crashing the step`() =
        runTest {
            val agent =
                ExecutorAgent(ActionExecutor { ExecutionOutcome(success = false, observation = "", error = "boom") })

            val result = agent.step(AgentState(goal = "g", plan = listOf(step("tap", sideEffect = false))))

            assertEquals(1, result.currentStep)
            assertTrue(result.failures.any { it.contains("boom") })
        }

    @Test
    fun `plan exhaustion marks the run done`() =
        runTest {
            val agent = ExecutorAgent(ActionExecutor { ExecutionOutcome(success = true, observation = "ok") })

            val result = agent.step(AgentState(goal = "g", plan = emptyList(), currentStep = 0))

            assertTrue(result.done)
        }

    // Strongest proof of the AC: a real AionGraph run, mixed side-effect/non-side-effect steps,
    // asserting the fake ApprovalGate fires exactly once per side-effect step, never for the others.
    @Test
    fun `a real AionGraph run requests approval exactly once per side-effect step, in order`() =
        runTest {
            val executed = mutableListOf<String>()
            val approvalRequestsFor = mutableListOf<Int>()

            val executor =
                ActionExecutor { s ->
                    executed.add(s.action)
                    ExecutionOutcome(success = true, observation = s.action)
                }
            val executorAgent = ExecutorAgent(executor)

            val plan =
                listOf(
                    step("readScreen", sideEffect = false),
                    step("sendMessage", sideEffect = true),
                    step("checkResult", sideEffect = false),
                    step("deleteFile", sideEffect = true),
                )

            val graph =
                AionGraph(
                    nodes =
                        mapOf(
                            "planner" to Agent { it },
                            "executor" to executorAgent,
                            "reflector" to Agent { it.copy(done = true) },
                        ),
                    route = { node, s ->
                        when {
                            node == "planner" -> "executor"
                            s.done || s.currentStep >= s.plan.size -> AionGraph.END
                            else -> "executor"
                        }
                    },
                    approval =
                        ApprovalGate { s ->
                            approvalRequestsFor.add(s.currentStep)
                            s // approved: AionGraph clears needsApproval itself right after this
                        },
                    checkpoints = Checkpointer { },
                )

            val result = graph.run(AgentState(goal = "do stuff", plan = plan))

            assertEquals(listOf("readScreen", "sendMessage", "checkResult", "deleteFile"), executed)
            assertEquals(listOf(1, 3), approvalRequestsFor) // only the two sideEffect=true step indices
            assertTrue(result.failures.isEmpty())
        }
}
