package com.aion.host.brain

import com.aion.brain.PluginManager
import com.aion.brain.plugins.BrowserPlugin
import com.aion.brain.plugins.CalendarPlugin
import com.aion.brain.plugins.ContactsPlugin
import com.aion.brain.plugins.FilesPlugin
import com.aion.brain.plugins.PhoneSmsPlugin
import com.aion.brain.plugins.SystemPlugin
import com.aion.host.automation.DispatcherActionExecutor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-071..076 — registers the 6 v1 built-ins against the real [DispatcherActionExecutor], validated
 * via [PluginManager.register] at construction time. Deliberately does NOT call `enable()` on any
 * of them — that's 🧍HC-5, the owner's explicit per-plugin approval, never done on their behalf.
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
        }
    }
