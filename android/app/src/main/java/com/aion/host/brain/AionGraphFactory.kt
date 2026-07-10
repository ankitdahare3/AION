package com.aion.host.brain

import com.aion.brain.ActionExecutor
import com.aion.brain.AionGraph
import com.aion.brain.ExecutorAgent
import com.aion.brain.MemoryWriterAgent
import com.aion.brain.PlannerAgent
import com.aion.brain.ProviderRouter
import com.aion.brain.ReflectorAgent
import com.aion.brain.ResponderAgent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-053 — assembles a real [AionGraph] per DOC-004 §2's standard flow: planner → executor →
 * (fail → reflector → planner) → responder → memory_writer → END. `router`/`executor` are passed
 * in per-call rather than Hilt-injected: a real [ProviderRouter] needs live provider API keys
 * (T-031 🧍HC-3, not entered yet) and a real [ActionExecutor] (`DispatcherActionExecutor`) needs a
 * live `AionAccessibilityService`. Everything else here — approval, checkpointing — is real today.
 * Voice input (EPIC 2, not built) is out of scope: a goal from `VoiceSessionManager` would just be
 * another `String` passed to [create]'s resulting graph, nothing here needs to change for that.
 *
 * DOC-004's diagram also names a separate "verifier" node; that's folded into `executor` instead —
 * verification needs the exact before/after screen text the executor already captures, so bouncing
 * through an extra graph hop just to re-run a comparison on the same two strings has no benefit.
 */
@Singleton
class AionGraphFactory
    @Inject
    constructor(
        private val approvalGate: RealApprovalGate,
        private val checkpointer: RoomCheckpointer,
    ) {
        fun create(
            router: ProviderRouter,
            executor: ActionExecutor,
        ): AionGraph {
            var lastFailureCount = 0
            return AionGraph(
                nodes =
                    mapOf(
                        "planner" to PlannerAgent(router),
                        "executor" to ExecutorAgent(executor),
                        "reflector" to ReflectorAgent(),
                        "responder" to ResponderAgent(),
                        "memory_writer" to MemoryWriterAgent(),
                    ),
                route = { node, s ->
                    when (node) {
                        "planner" -> "executor"
                        "executor" ->
                            when {
                                s.failures.size > lastFailureCount -> {
                                    lastFailureCount = s.failures.size
                                    "reflector"
                                }
                                s.currentStep >= s.plan.size -> "responder"
                                else -> "executor"
                            }
                        "reflector" -> if (s.done) AionGraph.END else "planner"
                        "responder" -> "memory_writer"
                        else -> AionGraph.END
                    }
                },
                approval = approvalGate,
                checkpoints = checkpointer,
            )
        }
    }
