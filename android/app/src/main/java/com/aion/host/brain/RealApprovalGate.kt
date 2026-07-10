package com.aion.host.brain

import com.aion.brain.AgentState
import com.aion.brain.ApprovalGate
import com.aion.host.security.ApprovalGateService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-053 wiring — bridges the frozen `:brain` [ApprovalGate] contract to the real T-021
 * [ApprovalGateService]. A denial sets `done=true` with an explanation; [ExecutorAgent]'s own
 * KDoc documents that this is the exact convention it relies on (a denied step must never be
 * revisited).
 */
@Singleton
class RealApprovalGate
    @Inject
    constructor(
        private val service: ApprovalGateService,
    ) : ApprovalGate {
        override suspend fun await(s: AgentState): AgentState {
            val step = s.plan.getOrNull(s.currentStep) ?: return s
            val approved = service.requestApproval(voiceLine = "${step.action} ${step.target}?", detail = step.expected)
            return if (approved) {
                s
            } else {
                s.copy(done = true, failures = s.failures + "user denied approval for ${step.action} ${step.target}")
            }
        }
    }
