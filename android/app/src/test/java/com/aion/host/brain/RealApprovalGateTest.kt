package com.aion.host.brain

import com.aion.brain.AgentState
import com.aion.brain.PlanStep
import com.aion.brain.ResponsePhrasing
import com.aion.host.security.ApprovalGateService
import com.aion.host.security.AuditLogger
import com.aion.host.security.FakeAuditDao
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealApprovalGateTest {
    @Test
    fun `approval leaves state unchanged for AionGraph to clear needsApproval itself`() =
        runTest {
            val service = ApprovalGateService(AuditLogger(FakeAuditDao()))
            val gate = RealApprovalGate(service)
            val s =
                AgentState(
                    goal = "g",
                    plan = listOf(PlanStep("send", "Ravi", "sent", true)),
                    currentStep = 0,
                    needsApproval = true,
                )

            val job = launch { }
            val resultJob =
                launch {
                    val result = gate.await(s)
                    assertEquals(s, result)
                }
            testScheduler.runCurrent()
            service.resolve(service.pending.value!!.id, true)
            resultJob.join()
            job.join()
        }

    @Test
    fun `denial aborts with an honest explanation instead of retrying silently`() =
        runTest {
            val service = ApprovalGateService(AuditLogger(FakeAuditDao()))
            val gate = RealApprovalGate(service)
            val s =
                AgentState(
                    goal = "g",
                    plan = listOf(PlanStep("send", "Ravi", "sent", true)),
                    currentStep = 0,
                    needsApproval = true,
                )

            var result: AgentState? = null
            val job = launch { result = gate.await(s) }
            testScheduler.runCurrent()
            service.resolve(service.pending.value!!.id, false)
            job.join()

            assertTrue(result!!.done)
            assertFalse(result!!.failures.isEmpty())
            assertTrue(result!!.failures.last().contains("send"))
            assertEquals(ResponsePhrasing.forDenial(hinglish = false), result!!.response)
        }

    @Test
    fun `no current step to approve returns the state untouched`() =
        runTest {
            val service = ApprovalGateService(AuditLogger(FakeAuditDao()))
            val gate = RealApprovalGate(service)
            val s = AgentState(goal = "g", plan = emptyList(), currentStep = 0)

            val result = gate.await(s)

            assertEquals(s, result)
        }
}
