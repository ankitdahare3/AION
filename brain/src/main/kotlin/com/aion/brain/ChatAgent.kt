package com.aion.brain

/**
 * T-150 (BACKLOG.md, found during T-130/T-137) — the conversational path that never existed:
 * every goal used to route straight through [PlannerAgent], so a plain greeting ("Namaste AION")
 * got planned as a device-automation task. Answers via a real [ProviderRouter] CHAT call and ends
 * the run — per AionGraph's frozen `run()` loop, whoever sets `done=true` must also author
 * `response` (same rule ReflectorAgent/PlannerAgent already follow).
 */
class ChatAgent(
    private val router: ProviderRouter,
) : Agent {
    override suspend fun step(s: AgentState): AgentState {
        val req =
            BrainRequest(
                taskType = TaskType.CHAT,
                system = PERSONA,
                messages = listOf(Msg("user", s.goal)),
            )
        return try {
            val text = router.route(req).text.trim()
            if (text.isBlank()) {
                s.copy(
                    done = true,
                    failures = s.failures + "chat: provider returned a blank response",
                    response = ResponsePhrasing.forFailure(FailureCause.UNKNOWN, ResponsePhrasing.isHinglish(s.goal)),
                )
            } else {
                s.copy(done = true, response = text)
            }
        } catch (e: Exception) {
            s.copy(
                done = true,
                failures = s.failures + "chat: routing failed: ${e.message ?: e.javaClass.simpleName}",
                response = ResponsePhrasing.forFailure(FailureCause.UNKNOWN, ResponsePhrasing.isHinglish(s.goal)),
            )
        }
    }

    private companion object {
        // Mirrors the bilingual detect-don't-dictate rule every other surface follows
        // (ResponsePhrasing, DOC-011 §3): reply in the user's own language.
        val PERSONA =
            """
            You are AION, a personal voice-first assistant on the owner's own Android phone.
            Reply naturally and briefly (1-3 sentences) — this is spoken conversation, not an essay.
            Mirror the user's language: Hindi in -> Hindi out, English in -> English out, Hinglish in -> Hinglish out.
            Never pretend you did something on the device — this is a chat reply, no actions were taken.
            """.trimIndent()
    }
}

/**
 * T-150 — sits in the graph's "planner" slot (AionGraph's frozen `run()` always starts there):
 * classifies the goal and delegates CHAT goals to [ChatAgent], everything else to the real
 * [PlannerAgent]. Only CHAT is diverted for now — INFO_QUERY deliberately stays on the automation
 * path, since "battery kitni hai" is answerable by reading the real device screen but NOT from an
 * LLM's own knowledge; diverting it would trade a working (if slow) real answer for a confidently
 * wrong one.
 *
 * 2026 backend upgrade — [llmClassifier] is tried first when present; the keyword [IntentClassifier]
 * is the fallback both when no real classifier was wired in (the default) AND when the real one
 * returns null or throws for this particular utterance (model unavailable on this device, or an
 * unparseable reply) — a device that can't run the local LLM never loses functionality, it just
 * doesn't gain the extra accuracy. Found via a real user-reported bug: "kaisa hai" (a casual "how
 * are you") fell through the keyword classifier's greeting whitelist into INFO_QUERY, routing to
 * the real automation planner instead of a chat reply — patched at the keyword layer too, but a
 * real classifier is a structurally better fix for the whole class of "phrasing nobody thought to
 * list" bugs than an ever-growing keyword whitelist.
 */
class IntentRoutingAgent(
    private val chat: Agent,
    private val planner: Agent,
    private val llmClassifier: LlmIntentClassifier? = null,
) : Agent {
    override suspend fun step(s: AgentState): AgentState {
        val llmIntent = llmClassifier?.let { runCatching { it.classify(s.goal) }.getOrNull() }
        val intent = llmIntent ?: IntentClassifier.classify(s.goal)
        return when (intent) {
            Intent.CHAT -> chat.step(s)
            else -> planner.step(s)
        }
    }
}
