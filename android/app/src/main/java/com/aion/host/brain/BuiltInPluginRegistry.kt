package com.aion.host.brain

import com.aion.brain.PluginManager
import com.aion.brain.plugins.BrowserPlugin
import com.aion.brain.plugins.CalendarPlugin
import com.aion.brain.plugins.ContactsPlugin
import com.aion.brain.plugins.FilesPlugin
import com.aion.brain.plugins.GmailPlugin
import com.aion.brain.plugins.PhoneSmsPlugin
import com.aion.brain.plugins.SystemPlugin
import com.aion.brain.plugins.TelegramPlugin
import com.aion.brain.plugins.UIAutomationPlugin
import com.aion.host.automation.DispatcherActionExecutor
import com.aion.host.di.GmailToken
import com.aion.host.di.TelegramToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-071..076/T-077 — registers the 6 v1 named built-ins plus [UIAutomationPlugin] against the real
 * [DispatcherActionExecutor], validated via [PluginManager.register] at construction time.
 *
 * [UIAutomationPlugin] is auto-`enable()`d: it's the generic-fallback baseline `ExecutorAgent`
 * already used directly before T-077 (dispatching gestures via the accessibility service) — routing
 * it through `PluginManager` doesn't grant any *new* capability, so gating it behind 🧍HC-5 the same
 * way as the other 8 would've been friction with no actual safety benefit.
 *
 * The 8 named built-ins (System, Contacts, Phone/SMS, Calendar, Files, Browser, Gmail, Telegram)
 * are now enabled too — 🧍HC-5, done directly by the owner (chat instruction, 2026-07-12), not
 * self-certified. Enabling only makes a plugin reachable via [PluginManager.route]: every
 * `sideEffect:true` tool call still goes through [RealPluginApprovalGate] individually regardless
 * of enable state (DOC-005 §4), and all 6 non-Gmail/Telegram plugins only ever translate a tool
 * call into a [com.aion.brain.PlanStep] dispatched through the same accessibility-service
 * `DispatcherActionExecutor` every other action already goes through (T-071..076: "tap/launchApp/
 * globalAction only") — no new Android permission is required by this change. Gmail/Telegram
 * remain safe to enable with a blank/missing token: every real API call just fails gracefully with
 * a normal `ToolResult` error, same as `ShizukuBridge` degrading cleanly when Shizuku isn't
 * installed (T-102).
 */
@Singleton
class BuiltInPluginRegistry
    @Inject
    constructor(
        executor: DispatcherActionExecutor,
        approvalGate: RealPluginApprovalGate,
        @GmailToken gmailToken: String,
        @TelegramToken telegramToken: String,
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
                GmailPlugin(gmailToken),
                TelegramPlugin(telegramToken),
            ).forEach {
                manager.register(it)
                manager.enable(it.manifest.id)
            }

            manager.register(UIAutomationPlugin(executor))
            manager.enable(UIAutomationPlugin.ID)
        }
    }
