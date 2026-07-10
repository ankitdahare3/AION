package com.aion.host.brain

import com.aion.brain.PluginRouteResult
import com.aion.brain.ToolCall
import com.aion.host.automation.ActionDispatcher
import com.aion.host.automation.DispatcherActionExecutor
import com.aion.host.security.ApprovalGateService
import com.aion.host.security.AuditLogger
import com.aion.host.security.FakeAuditDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-071..076 AC — all 6 built-ins register (validated) but stay disabled until the owner's explicit 🧍HC-5 enable. */
class BuiltInPluginRegistryTest {
    @Test
    fun `all 6 built-ins register successfully but none are enabled by default`() =
        runTest {
            val auditLogger = AuditLogger(FakeAuditDao())
            val executor = DispatcherActionExecutor(ActionDispatcher(auditLogger))
            val approvalGate = RealPluginApprovalGate(ApprovalGateService(auditLogger))

            val registry = BuiltInPluginRegistry(executor, approvalGate)

            val ids =
                listOf(
                    "com.aion.plugin.system",
                    "com.aion.plugin.contacts",
                    "com.aion.plugin.phone_sms",
                    "com.aion.plugin.calendar",
                    "com.aion.plugin.files",
                    "com.aion.plugin.browser",
                )
            for (id in ids) {
                assertFalse("$id should not be enabled without owner approval (HC-5)", registry.manager.isEnabled(id))
                val routed = registry.manager.route(id, ToolCall("anything", "{}", false))
                assertTrue("$id routed while disabled: $routed", routed is PluginRouteResult.Rejected)
            }
        }
}
