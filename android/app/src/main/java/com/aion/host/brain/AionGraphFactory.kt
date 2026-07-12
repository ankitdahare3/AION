package com.aion.host.brain

import com.aion.brain.AionGraph
import com.aion.brain.ApprovalGate
import com.aion.brain.ExecutorAgent
import com.aion.brain.MemoryStore
import com.aion.brain.MemoryWriterAgent
import com.aion.brain.PlannerAgent
import com.aion.brain.PluginManager
import com.aion.brain.ProviderRouter
import com.aion.brain.ReflectorAgent
import com.aion.brain.ResponderAgent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-053/T-077 — assembles a real [AionGraph] per DOC-004 §2's standard flow: planner → executor →
 * (fail → reflector → planner) → responder → memory_writer → END. `router`/`pluginManager`/
 * `approval` are all passed in per-call rather than Hilt-injected: a real [ProviderRouter] needs
 * live provider API keys (T-031 🧍HC-3), `pluginManager` must already have
 * `com.aion.plugin.uiautomation` registered+enabled (see [BuiltInPluginRegistry]) — a real
 * `DispatcherActionExecutor` needs a live `AionAccessibilityService` — and `approval` varies by
 * caller: [RealApprovalGate] for a live run, a scripted one for an unattended benchmark (T-121)
 * where nobody's tapping the approval sheet. Voice input (EPIC 2, not built) is out of scope: a
 * goal from `VoiceSessionManager` would just be another `String` passed to [create]'s resulting
 * graph, nothing here needs to change for that.
 *
 * DOC-004's diagram also names a separate "verifier" node; that's folded into the executor path
 * instead — verification needs the exact before/after screen text `DispatcherActionExecutor`
 * already captures, so bouncing through an extra graph hop just to re-run a comparison on the same
 * two strings has no benefit. `ExecutorAgent` itself (T-077) no longer calls any dispatcher
 * directly — it routes exclusively through [PluginManager].
 */
@Singleton
class AionGraphFactory
    @Inject
    constructor(
        private val checkpointer: RoomCheckpointer,
        private val memoryStore: MemoryStore,
    ) {
        fun create(
            router: ProviderRouter,
            pluginManager: PluginManager,
            approval: ApprovalGate,
        ): AionGraph {
            var lastFailureCount = 0
            return AionGraph(
                nodes =
                    mapOf(
                        "planner" to PlannerAgent(router, memoryStore = memoryStore),
                        "executor" to ExecutorAgent(pluginManager),
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
                approval = approval,
                checkpoints = checkpointer,
            )
        }
    }
