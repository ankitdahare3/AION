package com.aion.brain

/**
 * DOC-004 §2 — terminal-before-memory_writer node: guarantees a user-facing response always
 * exists, phrased as something a person would actually say — not a raw error string, error code,
 * or `"Something went wrong: <technical detail>"` template. [ResponsePhrasing] is the single shared
 * phrasing table this and [ReflectorAgent] both use (see ReflectorAgent's own doc comment for why
 * abort messages usually come from there instead of here), so a raw failure string never reaches
 * the user from either site — it's still kept, unmodified, in `failures` for internal
 * logging/classification (`BenchmarkHarnessTest` already logs the full state).
 */
class ResponderAgent : Agent {
    override suspend fun step(s: AgentState): AgentState {
        if (s.response != null) return s.copy(done = true)
        val hinglish = ResponsePhrasing.isHinglish(s.goal)
        val text =
            s.failures.lastOrNull()?.let { ResponsePhrasing.forFailure(ReflectorAgent.classify(it), hinglish) }
                ?: ResponsePhrasing.forSuccess(hinglish)
        return s.copy(done = true, response = text)
    }
}

/**
 * DOC-004 §2 — stub. Real episodic-memory writing is EPIC 6 (T-060+), which doesn't exist yet;
 * an honest no-op rather than fabricating persistence that isn't real (DOC-007 §5 honesty rule).
 */
class MemoryWriterAgent : Agent {
    override suspend fun step(s: AgentState): AgentState = s.copy(done = true)
}
