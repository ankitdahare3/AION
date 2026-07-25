package com.aion.brain

enum class Intent { CHAT, SIMPLE_ACTION, MULTI_STEP, INFO_QUERY, SYSTEM }

/**
 * DOC-004 §3 — v1 rule-based stand-in for the local-LLM classifier DOC-004 actually specifies
 * ("Local 4B model classifies..."). T-032 (llama.cpp JNI) is deliberately deferred (a native-build
 * wall on this dev machine, owner's call); this bilingual keyword/pattern classifier unblocks
 * T-034/T-035 now. Replacing it with the real local-LLM classifier once T-032 lands is filed in
 * BACKLOG.md, not silently forgotten.
 */
object IntentClassifier {
    // Idiomatic Hindi greetings that grammatically look like a question ("how are you") but are
    // CHAT, not INFO_QUERY — checked before every other rule. Real user-reported bug: "kaisa hai"/
    // "kaisa ho" (the masculine-singular conjugation of this exact same idiom — arguably the MOST
    // common form in casual Hinglish, e.g. asking AION itself "kaisa hai") was missing entirely, so
    // it fell through to INFO_QUERY (matches "kaisa" in infoWords + "?"/question shape) and got
    // routed by IntentRoutingAgent to the real device-automation PlannerAgent instead of ChatAgent
    // — which then tried to plan a device action to "answer" small talk, surfacing an approval
    // prompt (e.g. "open ...") instead of a normal reply.
    private val greetingOverrides = listOf("kaise ho", "kaisi ho", "kaise hain", "kaisa hai", "kaisa ho")

    private val systemTriggerWords = listOf("aion", "audit log", "kill switch", "wake word", "api key")
    private val systemControlWords =
        listOf(
            "stop",
            "band",
            "mute",
            "setting",
            "dikhao",
            "dabao",
            "set kar",
            "change kar",
            "change",
            "update",
            "show",
            "version",
            "model",
            "permission",
            "listening",
        )

    private val actionVerbs =
        listOf(
            "khol",
            "bajao",
            "bhej",
            "likho",
            "laga do",
            "nikal",
            "search kar",
            "dhund",
            "book kar",
            "add kar",
            "khinch",
            "on kar",
            "off kar",
            "badha do",
            "ghata do",
            "save kar",
            "send",
            "play",
            "open",
            "call",
            "set",
            "turn on",
            "turn off",
            "screenshot",
            "share kar",
            "mute",
            "mail kar",
            "padh",
            "reply kar",
            "bhar",
            "submit kar",
            // T-166 (BACKLOG.md, found via a real T-121 benchmark re-run): "check karo"/"dikhao"/
            // "batao"/"band karo"/"badhao" are all common Hindi command verbs that had NO entry
            // here at all — every goal using only one of these fell all the way through to the
            // final `return Intent.CHAT`, diverting a real device-read/action goal to ChatAgent,
            // which then fabricated a plausible-sounding answer instead of ever attempting the
            // real automation path (e.g. "battery percentage batao", "current location check
            // karo", "wallet balance dikhao" all got made-up answers). `dikhao`/`band` already
            // exist in systemControlWords for AION-specific SYSTEM goals, but SYSTEM classification
            // requires a systemTriggerWord too and short-circuits before this list is ever
            // consulted, so adding them here doesn't affect those cases.
            "check kar",
            "dikhao",
            "batao",
            "band kar",
            "badhao",
        )

    private val infoWords =
        listOf(
            "kya",
            "kaun",
            "kaunsa",
            "kab",
            "kahan",
            "kyun",
            "kaise",
            "kaisa",
            "kaisi",
            "kitna",
            "kitne",
            "kitni",
            "what",
            "who",
            "when",
            "where",
            "why",
            "how much",
            "how many",
            "which",
        )

    private val multiStepConnectors = listOf(" aur ", " phir ", " uske baad ", " and then ", " then ")

    fun classify(utterance: String): Intent {
        val text = utterance.trim().lowercase()

        if (greetingOverrides.any { text.contains(it) }) return Intent.CHAT

        val hasSystemTrigger = systemTriggerWords.any { text.contains(it) }
        val hasSystemControl = systemControlWords.any { text.contains(it) }
        if (hasSystemTrigger && hasSystemControl) return Intent.SYSTEM

        val verbHits = actionVerbs.count { text.contains(it) }
        val hasConnector = multiStepConnectors.any { text.contains(it) }
        if (verbHits >= 2 || (hasConnector && verbHits >= 1)) return Intent.MULTI_STEP

        val looksLikeQuestion =
            text.endsWith("?") ||
                infoWords.any { text.startsWith("$it ") || text.contains(" $it ") || text.endsWith(" $it") }
        if (looksLikeQuestion && verbHits == 0) return Intent.INFO_QUERY

        if (verbHits >= 1) return Intent.SIMPLE_ACTION

        return Intent.CHAT
    }
}
