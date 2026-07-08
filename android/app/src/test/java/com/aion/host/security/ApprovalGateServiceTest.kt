package com.aion.host.security

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovalGateServiceTest {
    @Test
    fun `requestApproval blocks until resolve is called, then returns the decision`() =
        runTest {
            val dao = FakeAuditDao()
            val service = ApprovalGateService(AuditLogger(dao))

            var result: Boolean? = null
            val job = launch { result = service.requestApproval("Send the email?", "to: boss@example.com") }
            testScheduler.runCurrent()

            // Still suspended: nothing has resolved the request yet.
            assertNull(result)
            val pending = service.pending.value
            assertNotNull(pending)
            assertEquals("Send the email?", pending!!.voiceLine)

            service.resolve(pending.id, true)
            job.join()

            assertEquals(true, result)
        }

    @Test
    fun `an approved decision is audited`() =
        runTest {
            val dao = FakeAuditDao()
            val service = ApprovalGateService(AuditLogger(dao))

            val job = launch { service.requestApproval("Approve this?", "detail") }
            testScheduler.runCurrent()
            service.resolve(service.pending.value!!.id, true)
            job.join()

            val logged = dao.getAllOrdered()
            assertEquals(1, logged.size)
            assertEquals("approval.decision", logged[0].action)
            assertTrue(logged[0].payloadJson.contains("\"approved\":true"))
        }

    @Test
    fun `a denied decision is audited and pending clears`() =
        runTest {
            val dao = FakeAuditDao()
            val service = ApprovalGateService(AuditLogger(dao))

            var result: Boolean? = null
            val job = launch { result = service.requestApproval("Approve this?", "detail") }
            testScheduler.runCurrent()
            service.resolve(service.pending.value!!.id, false)
            job.join()

            assertEquals(false, result)
            assertNull(service.pending.value)
            val logged = dao.getAllOrdered()
            assertTrue(logged[0].payloadJson.contains("\"approved\":false"))
        }
}
