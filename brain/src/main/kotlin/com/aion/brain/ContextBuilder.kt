package com.aion.brain

/** Result of [ContextBuilder.build] — caller (T-035+) wraps this into a [BrainRequest]. */
data class BuiltContext(
    val system: String,
    val messages: List<Msg>,
    val estimatedTokens: Int,
    val turnsDropped: Int,
)

/**
 * DOC-004 §4 — v1 scope: persona+safety immutable prefix + last N=6 turns, hard budget 8K tokens.
 * Full spec (user-profile summary, vector-recalled memories, a11y tree, tool schemas) needs
 * systems that don't exist yet (Memory, the a11y reader T-040, PluginManager T-070+) — those slot
 * in as additional budgeted sections later without changing this shape; see BACKLOG.md.
 */
object ContextBuilder {
    const val DEFAULT_MAX_TOKENS = 8192
    const val HISTORY_TURNS = 6

    // No real tokenizer at v1 — ~4 chars/token is a standard, conservative English-biased estimate.
    private const val CHARS_PER_TOKEN = 4

    fun estimateTokens(text: String): Int = (text.length + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN

    /**
     * Assembles system (persona + safety rules, never trimmed — DOC-017: safety rules are
     * code-level, not editable) + up to the last [HISTORY_TURNS] of [history], dropping oldest
     * turns first until the whole assembly fits [maxTokens].
     *
     * @throws IllegalArgumentException if the persona+safety prefix alone exceeds [maxTokens] —
     *   that's a configuration error, not something safe to silently truncate.
     */
    fun build(
        persona: String,
        safetyRules: String,
        history: List<Msg>,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
    ): BuiltContext {
        val system = "$persona\n\n$safetyRules"
        val prefixTokens = estimateTokens(system)
        require(prefixTokens <= maxTokens) {
            "persona+safety prefix ($prefixTokens tok) alone exceeds the token budget ($maxTokens tok)"
        }

        var window = history.takeLast(HISTORY_TURNS)
        val windowedSize = window.size
        while (window.isNotEmpty() && prefixTokens + window.sumOf { estimateTokens(it.content) } > maxTokens) {
            window = window.drop(1)
        }

        return BuiltContext(
            system = system,
            messages = window,
            estimatedTokens = prefixTokens + window.sumOf { estimateTokens(it.content) },
            turnsDropped = windowedSize - window.size,
        )
    }
}
