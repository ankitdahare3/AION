package com.aion.host.brain

import com.aion.brain.PluginManager
import com.aion.brain.plugins.BrowserPlugin
import com.aion.brain.plugins.CalendarPlugin
import com.aion.brain.plugins.ContactsPlugin
import com.aion.brain.plugins.FilesPlugin
import com.aion.brain.plugins.PhoneSmsPlugin
import com.aion.brain.plugins.SystemPlugin
import com.aion.brain.plugins.UIAutomationPlugin
import com.aion.host.automation.DispatcherActionExecutor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-071..076/T-077 — registers the 6 v1 named built-ins plus [UIAutomationPlugin] against the real
 * [DispatcherActionExecutor], validated via [PluginManager.register] at construction time.
 *
 * [UIAutomationPlugin] is auto-`enable()`d: it's the generic-fallback baseline `ExecutorAgent`
 * already used directly before T-077 (dispatching gestures via the accessibility service) — routing
 * it through `PluginManager` doesn't grant any *new* capability, so gating it behind 🧍HC-5 the same
 * way as the other 6 would just be friction with no actual safety benefit. The named built-ins
 * (System, Contacts, ...) stay registered-but-disabled: those genuinely are new, curated
 * capabilities, and enabling them is the owner's explicit call, never made on their behalf.
 */
@Singleton
class BuiltInPluginRegistry
    @Inject
    constructor(
        executor: DispatcherActionExecutor,
        approvalGate: RealPluginApprovalGate,
    ) {
        val manager = PluginManager(approvalGate)

        init {
            listOf(
                SystemPlugin(executor),
                ContactsPlugin(executor),
                PhoneSmsPlugin(executor),
                CalendarPlugin(executor),
                FilesPlugin(executor),
                BrowserPlugin(executor),
            ).forEach { manager.register(it) }

            manager.register(UIAutomationPlugin(executor))
            manager.enable(UIAutomationPlugin.ID)
        }
    }
