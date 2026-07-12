package com.aion.host.brain

import com.aion.brain.PluginRouteResult
import com.aion.brain.ToolCall
import com.aion.brain.plugins.UIAutomationPlugin
import com.aion.host.automation.ActionDispatcher
import com.aion.host.automation.DispatcherActionExecutor
import com.aion.host.automation.MlKitOcrEngine
import com.aion.host.security.ApprovalGateService
import com.aion.host.security.AuditLogger
import com.aion.host.security.FakeAuditDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-071..076/T-077/T-102/T-116 AC — the 8 named built-ins register (validated) and, per the owner's explicit 🧍HC-5 chat instruction (2026-07-12), are enabled; UIAutomation (the pre-existing baseline) is auto-enabled regardless. */
class BuiltInPluginRegistryTest {
    @Test
    fun `the 8 named built-ins register successfully and are enabled per the owner's explicit HC-5 approval`() =
        runTest {
            val auditLogger = AuditLogger(FakeAuditDao())
            val executor = DispatcherActionExecutor(ActionDispatcher(auditLogger), MlKitOcrEngine())
            val approvalGate = RealPluginApprovalGate(ApprovalGateService(auditLogger))

            val registry = BuiltInPluginRegistry(executor, approvalGate, "", "")

            val ids =
                listOf(
                    "com.aion.plugin.system",
                    "com.aion.plugin.contacts",
                    "com.aion.plugin.phone_sms",
                    "com.aion.plugin.calendar",
                    "com.aion.plugin.files",
                    "com.aion.plugin.browser",
                    "com.aion.plugin.gmail",
                    "com.aion.plugin.telegram",
                )
            for (id in ids) {
                assertTrue("$id should be enabled per the owner's explicit HC-5 approval", registry.manager.isEnabled(id))
                // A bogus tool name still gets rejected — but now for "no tool named", not "not
                // enabled" (PluginManager.route checks enabled state BEFORE tool lookup), proving
                // routing genuinely reaches past the enable gate rather than isEnabled() alone.
                val routed = registry.manager.route(id, ToolCall("definitely_not_a_real_tool", "{}", false))
                assertTrue(
                    "$id: expected an unknown-tool rejection now that it's enabled, got $routed",
                    routed is PluginRouteResult.Rejected && (routed as PluginRouteResult.Rejected).reason.contains("no tool named"),
                )
            }
        }

    @Test
    fun `UIAutomation is registered and auto-enabled, since it grants no new capability`() =
        runTest {
            val auditLogger = AuditLogger(FakeAuditDao())
            val executor = DispatcherActionExecutor(ActionDispatcher(auditLogger), MlKitOcrEngine())
            val approvalGate = RealPluginApprovalGate(ApprovalGateService(auditLogger))

            val registry = BuiltInPluginRegistry(executor, approvalGate, "", "")

            assertTrue(registry.manager.isEnabled(UIAutomationPlugin.ID))
        }
}
