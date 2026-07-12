package com.aion.host.brain

import com.aion.brain.AgentState
import com.aion.brain.ApprovalGate
import com.aion.brain.ResponsePhrasing
import com.aion.host.security.ApprovalGateService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-053 wiring — bridges the frozen `:brain` [ApprovalGate] contract to the real T-021
 * [ApprovalGateService]. A denial sets `done=true` with an explanation; [ExecutorAgent]'s own
 * KDoc documents that this is the exact convention it relies on (a denied step must never be
 * revisited).
 *
 * Found during T-130's live chat-screen verification: `AionGraph`'s frozen `run()` loop checks
 * `done` BEFORE routing to the next node, so setting `done=true` here without also setting
 * `response` meant `ResponderAgent` never ran and the user saw a blank screen after denying —
 * same "whoever sets done=true must also phrase the response" convention `ReflectorAgent` already
 * follows (see [ResponsePhrasing]'s class doc), just missing here until now.
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
                s.copy(
                    done = true,
                    failures = s.failures + "user denied approval for ${step.action} ${step.target}",
                    response = ResponsePhrasing.forDenial(ResponsePhrasing.isHinglish(s.goal)),
                )
            }
        }
    }
