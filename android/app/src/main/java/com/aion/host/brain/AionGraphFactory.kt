package com.aion.host.brain

import com.aion.brain.AionGraph
import com.aion.brain.ApprovalGate
import com.aion.brain.ChatAgent
import com.aion.brain.ExecutorAgent
import com.aion.brain.FewShotBank
import com.aion.brain.IntentRoutingAgent
import com.aion.brain.MemoryStore
import com.aion.brain.MemoryWriterAgent
import com.aion.brain.PlannerAgent
import com.aion.brain.PluginManager
import com.aion.brain.ProviderRouter
import com.aion.brain.ReflectorAgent
import com.aion.brain.ResponderAgent
import com.aion.brain.ScreenSnapshotProvider
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
 *
 * T-150 (BACKLOG.md, found during T-130/T-137) — the "planner" node is now [IntentRoutingAgent],
 * not a bare [PlannerAgent]: it classifies the goal first and diverts CHAT-intent goals (a plain
 * greeting, small talk) to a real [ChatAgent] conversational reply instead of planning a device
 * action for them. Safe against `AionGraph`'s frozen `run()` loop without touching `route()` at
 * all — [ChatAgent] sets `done=true` on its very first step, and `run()`'s `while (... && !s.done)`
 * check exits before `route("planner", s)`'s unconditional "executor" next-hop is ever used.
 *
 * T-162 (BACKLOG.md, found live during T-158's 30-step-loop investigation) — [fewShotBank] is now
 * shared between [PlannerAgent] and [ReflectorAgent], not left at its default-null. T-081/T-090
 * built the whole "don't repeat this mistake" mechanism and unit-tested it in isolation, but no
 * real caller ever constructed a [FewShotBank] or passed one in here — so every live replan was
 * blind to the attempt that just failed, a real, plausible driver of goals that loop instead of
 * converging or failing fast.
 *
 * 2026 backend upgrade (found while wiring real episodic memory) — `memory_writer` now gets the
 * real [memoryStore] instead of a null default. It was ALREADY wired into the graph correctly, but
 * `ResponderAgent` used to set `done=true` itself, which — combined with `AionGraph.run()`'s frozen
 * `while (... && !s.done)` loop — meant the graph always ended one node too early and
 * `MemoryWriterAgent` never actually ran, for any goal, ever. Fixed on the `ResponderAgent`/
 * `MemoryWriterAgent` side (see their own doc comments) rather than touching the frozen graph.
 */
@Singleton
class AionGraphFactory
    @Inject
    constructor(
        private val checkpointer: RoomCheckpointer,
        private val memoryStore: MemoryStore,
        private val screenSnapshotProvider: ScreenSnapshotProvider,
        private val fewShotBank: FewShotBank,
    ) {
        fun create(
            router: ProviderRouter,
            pluginManager: PluginManager,
            approval: ApprovalGate,
        ): AionGraph =
            AionGraph(
                nodes =
                    mapOf(
                        "planner" to
                            IntentRoutingAgent(
                                chat = ChatAgent(router),
                                planner =
                                    PlannerAgent(
                                        router,
                                        fewShotBank = fewShotBank,
                                        memoryStore = memoryStore,
                                        screenSnapshotProvider = screenSnapshotProvider,
                                    ),
                            ),
                        "executor" to ExecutorAgent(pluginManager),
                        "reflector" to ReflectorAgent(fewShotBank),
                        "responder" to ResponderAgent(),
                        "memory_writer" to MemoryWriterAgent(memoryStore),
                    ),
                route = { node, s ->
                    when (node) {
                        "planner" -> "executor"
                        "executor" ->
                            // No stateful counter needed: ReflectorAgent always clears s.failures
                            // back to emptyList() before any retry (its own KDoc), and ExecutorAgent
                            // only ever appends to it on the exact call that just failed — so a
                            // non-empty list here always means a fresh failure to hand off, whether
                            // it's the first one for this goal or a recurrence of the same one.
                            // A prior version tracked `lastFailureCount` against the post-clear list
                            // SIZE, which broke on the second occurrence of an identical recoverable
                            // failure (size goes 0->1 again, never exceeding the stale watermark) —
                            // found via T-121's real benchmark data (BACKLOG.md).
                            when {
                                s.failures.isNotEmpty() -> "reflector"
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
