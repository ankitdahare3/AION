package com.aion.host.brain

import com.aion.brain.PluginApprovalGate
import com.aion.brain.ToolCall
import com.aion.host.security.ApprovalGateService
import javax.inject.Inject
import javax.inject.Singleton

/** T-071..076 wiring — bridges the frozen `:brain` [PluginApprovalGate] to the real [ApprovalGateService], same pattern as [RealApprovalGate]. */
@Singleton
class RealPluginApprovalGate
    @Inject
    constructor(
        private val service: ApprovalGateService,
    ) : PluginApprovalGate {
        override suspend fun approve(
            pluginId: String,
            call: ToolCall,
        ): Boolean = service.requestApproval(voiceLine = "$pluginId: ${call.name}?", detail = call.argsJson)
    }
