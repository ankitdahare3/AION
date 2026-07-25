package com.aion.brain

/**
 * 2026 backend upgrade — the real local-LLM classifier DOC-004 §3 always specified, in the same
 * "define the contract in :brain, implement it with real Android/native code in :android:app"
 * shape as [MemoryStore]/[ScoreStore]/[BudgetGuard]. Returns null — never throws — whenever no
 * real classification is available (model not present, device can't run it, unparseable output);
 * null is a legitimate, expected outcome, not an error state. [IntentRoutingAgent] treats it that
 * way: an implementation is entirely optional, and the existing keyword [IntentClassifier] is the
 * fallback for both "no implementation was wired in" and "the implementation returned null."
 */
fun interface LlmIntentClassifier {
    suspend fun classify(utterance: String): Intent?
}

/** Pure prompt-building/response-parsing for [LlmIntentClassifier] — no Android/native dependency, unit-tested directly. */
object LlmIntentClassification {
    val SYSTEM_INSTRUCTION =
        """
        You are an intent classifier for a personal voice assistant. Classify the user's message
        into EXACTLY ONE of these five categories and reply with ONLY that single word, nothing else:
        CHAT - small talk, greetings, feelings, opinions, general conversation, asking how you are
        SIMPLE_ACTION - a single device action (open an app, play music, send a message, set an alarm)
        MULTI_STEP - two or more chained actions in one request
        INFO_QUERY - a factual question, including ones only the device itself can answer (battery, time, location)
        SYSTEM - a command about you (AION) yourself: stop listening, show audit log, change your settings
        Reply with exactly one of: CHAT, SIMPLE_ACTION, MULTI_STEP, INFO_QUERY, SYSTEM
        """.trimIndent()

    /** Robust against the model wrapping its answer in punctuation/extra words it was told not to add. */
    fun parseLabel(raw: String): Intent? {
        val cleaned = raw.trim().uppercase().trim('.', '!', '"', '\'', ' ')
        return Intent.entries.firstOrNull { cleaned == it.name }
            ?: Intent.entries.firstOrNull { cleaned.startsWith(it.name) }
    }
}
