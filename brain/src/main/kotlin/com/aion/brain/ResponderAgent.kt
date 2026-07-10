package com.aion.brain

/** DOC-004 §2 — terminal-before-memory_writer node: guarantees a user-facing response always exists. */
class ResponderAgent : Agent {
    override suspend fun step(s: AgentState): AgentState =
        when {
            s.response != null -> s.copy(done = true)
            s.failures.isNotEmpty() -> s.copy(done = true, response = "Something went wrong: ${s.failures.last()}")
            else -> s.copy(done = true, response = "Done: ${s.goal}")
        }
}

/**
 * DOC-004 §2 — stub. Real episodic-memory writing is EPIC 6 (T-060+), which doesn't exist yet;
 * an honest no-op rather than fabricating persistence that isn't real (DOC-007 §5 honesty rule).
 */
class MemoryWriterAgent : Agent {
    override suspend fun step(s: AgentState): AgentState = s.copy(done = true)
}
