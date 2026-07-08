package com.aion.host.security

/**
 * DOC-004 §6, DOC-017 T1 — screen/notification text is DATA, never an instruction. [wrap] must be
 * the only path by which such text enters the Brain's context: it neutralizes imperative
 * injection patterns and wraps the result in `<screen_data>`. Defense-in-depth alongside the
 * Brain's own system-prompt rule that screen content is never an instruction (DOC-004 §6).
 */
object InjectionFilter {
    private const val REDACTED = "[redacted-instruction]"

    private val IMPERATIVE_PATTERNS =
        listOf(
            Regex(
                """(?i)\b(ignore|disregard|forget)\s+(the\s+)?(all\s+)?(previous|prior|above)\s+(instructions?|rules?|context)\b""",
            ),
            Regex("""(?i)\bsystem\s*:"""),
            Regex("""(?i)\bassistant\s*:"""),
            Regex("""(?i)\bnew\s+instructions?\s*:"""),
            Regex("""(?i)\byou\s+(must|should|will)\s+now\b"""),
            Regex("""(?i)\boverride\s+(your|the)\s+(\w+\s+)?(rules?|instructions?|safety)\b"""),
            Regex("""(?i)\bact\s+as\s+(if|a)\b"""),
            Regex("""(?i)\bfrom\s+now\s+on\s*,?\s+you\s+(will|must|should)\b"""),
            Regex("""(?i)\bdo\s+not\s+(tell|inform|notify)\s+the\s+user\b"""),
            Regex("""(?i)\bexecute\s+the\s+following\b"""),
            Regex(
                """(?i)\bapprove\s+(this|the|all)\s+\w*\s*(action|request|transaction|purchase)s?\s+automatically\b""",
            ),
            Regex("""(?i)\bbypass\s+(the\s+)?(safety|approval|security)\b"""),
            Regex("""(?i)\breveal\s+(the\s+)?(user'?s?\s+)?(password|otp|pin|secret)\b"""),
            Regex("""(?i)</?\s*screen_data\s*>"""),
        )

    /** Replaces every recognized imperative pattern with a neutral marker. Idempotent. */
    fun sanitize(raw: String): String {
        var text = raw
        for (pattern in IMPERATIVE_PATTERNS) {
            text = pattern.replace(text, REDACTED)
        }
        return text
    }

    /** The only sanctioned way screen/notification text should reach the Brain's context. */
    fun wrap(raw: String): String = "<screen_data>${sanitize(raw)}</screen_data>"

    /** True if [text] still contains a recognizable imperative pattern — should never be true after [sanitize]. */
    fun containsImperative(text: String): Boolean = IMPERATIVE_PATTERNS.any { it.containsMatchIn(text) }
}
