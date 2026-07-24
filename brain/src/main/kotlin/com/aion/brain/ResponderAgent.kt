package com.aion.brain

/**
 * DOC-004 §2 — authors the user-facing response, phrased as something a person would actually say
 * — not a raw error string, error code, or `"Something went wrong: <technical detail>"` template.
 * [ResponsePhrasing] is the single shared phrasing table this and [ReflectorAgent] both use (see
 * ReflectorAgent's own doc comment for why abort messages usually come from there instead of
 * here), so a raw failure string never reaches the user from either site — it's still kept,
 * unmodified, in `failures` for internal logging/classification (`BenchmarkHarnessTest` already
 * logs the full state).
 *
 * Deliberately does NOT set `done = true` (2026 backend upgrade, found while wiring real episodic
 * memory) — [AionGraph.run]'s loop is `while (node != END && !s.done)`, checked BEFORE each node
 * runs. This used to set `done` here, which meant the loop exited the instant this node ran,
 * before `route("responder", s)`'s own "responder" -> "memory_writer" hop was ever taken —
 * [MemoryWriterAgent] was wired into the graph but structurally unreachable, silently, for every
 * goal that ever ran. [MemoryWriterAgent] is now the real terminal node instead.
 */
class ResponderAgent : Agent {
    override suspend fun step(s: AgentState): AgentState {
        if (s.response != null) return s
        val hinglish = ResponsePhrasing.isHinglish(s.goal)
        val text =
            s.failures.lastOrNull()?.let { ResponsePhrasing.forFailure(ReflectorAgent.classify(it), hinglish) }
                ?: ResponsePhrasing.forSuccess(hinglish)
        return s.copy(response = text)
    }
}

/**
 * DOC-004 §2 / DOC-010 §3 — the real terminal node (see [ResponderAgent]'s doc comment for why).
 * [memoryStore] is optional, same "wired only where a real caller exists" pattern as
 * [PlannerAgent]'s own optional dependencies — a benchmark harness with no real [MemoryStore]
 * still gets an honest no-op rather than a crash.
 *
 * Only ever reached after a full plan genuinely ran to completion via "executor" -> "responder"
 * -> "memory_writer" — a goal that aborted early (PlannerAgent's own parse-failure path,
 * ReflectorAgent's abort path, ChatAgent) sets `done = true` directly and the graph's loop exits
 * before ever reaching this node, same as before this change. That's a deliberate, conservative
 * first step: only genuinely-completed runs get remembered, not ambiguous partial ones — widening
 * this to abort paths too is a real follow-up, not assumed safe here.
 */
class MemoryWriterAgent(
    private val memoryStore: MemoryStore? = null,
) : Agent {
    override suspend fun step(s: AgentState): AgentState {
        memoryStore?.insert(buildMemory(s, System.currentTimeMillis()))
        return s.copy(done = true)
    }

    companion object {
        const val PROVENANCE = "memory_writer"

        /** `Memory.confidence` is lower for a run that recovered from failures along the way — same "trust it a bit less" reasoning [NotificationIngestion][com.aion.host.svc.NotificationIngestion] already applies to a different, less-verified source. */
        fun buildMemory(
            s: AgentState,
            nowMs: Long,
        ): Memory =
            Memory(
                kind = MemoryKind.FACT,
                text = "Goal \"${s.goal}\": ${s.response.orEmpty()}",
                confidence = if (s.failures.isEmpty()) 0.9 else 0.6,
                provenance = PROVENANCE,
                created = nowMs,
                accessed = nowMs,
                decayScore = 1.0,
            )
    }
}
