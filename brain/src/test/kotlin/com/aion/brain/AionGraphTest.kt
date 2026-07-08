package com.aion.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private val noopApproval = ApprovalGate { s -> s }
private val noopCheckpointer = Checkpointer { }

class AionGraphTest {
    @Test
    fun `constructor requires a planner node`() {
        val ex =
            assertThrows(IllegalArgumentException::class.java) {
                AionGraph(
                    nodes = mapOf("reflector" to Agent { s -> s }),
                    route = { _, _ -> AionGraph.END },
                    approval = noopApproval,
                    checkpoints = noopCheckpointer,
                )
            }
        assertTrue(ex.message!!.contains("planner"))
    }

    @Test
    fun `constructor requires a reflector node`() {
        val ex =
            assertThrows(IllegalArgumentException::class.java) {
                AionGraph(
                    nodes = mapOf("planner" to Agent { s -> s }),
                    route = { _, _ -> AionGraph.END },
                    approval = noopApproval,
                    checkpoints = noopCheckpointer,
                )
            }
        assertTrue(ex.message!!.contains("reflector"))
    }

    @Test
    fun `run terminates immediately when planner marks done`() =
        runTest {
            val graph =
                AionGraph(
                    nodes =
                        mapOf(
                            "planner" to Agent { s -> s.copy(done = true, response = "ok") },
                            "reflector" to Agent { s -> s },
                        ),
                    route = { _, _ -> AionGraph.END },
                    approval = noopApproval,
                    checkpoints = noopCheckpointer,
                )
            val result = graph.run(AgentState(goal = "test"))
            assertTrue(result.done)
            assertEquals("ok", result.response)
        }

    @Test
    fun `run falls back to reflector once maxSteps is exceeded`() =
        runTest {
            var reflectorInvoked = false
            val graph =
                AionGraph(
                    nodes =
                        mapOf(
                            "planner" to Agent { s -> s },
                            "reflector" to
                                Agent { s ->
                                    reflectorInvoked = true
                                    s.copy(done = true)
                                },
                        ),
                    route = { _, _ -> "planner" },
                    approval = noopApproval,
                    checkpoints = noopCheckpointer,
                    maxSteps = 1,
                )
            val result = graph.run(AgentState(goal = "test"))
            assertTrue(reflectorInvoked)
            assertTrue(result.done)
        }
}
