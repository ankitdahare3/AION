package com.aion.brain

/**
 * Pure string-similarity math shared by [com.aion.host.automation.ElementResolver] (T-042, element
 * text matching) and [SkillMatcher] (T-090, skill trigger matching) — the same "how close are these
 * two short strings" problem in both cases. Lives in `:brain` (Android-independent) so both a
 * `:android:app` caller and a `:brain` caller can use it without a cross-module dependency the
 * wrong way round.
 */
object TextSimilarity {
    /**
     * Normalized-equality → squashed-equality (ignores spacing/punctuation, e.g. "wifi" ~ "Wi-Fi")
     * → substring containment (candidate-contains-query scores higher than the reverse, since a
     * short candidate coincidentally inside a long query is a weaker signal) → token-set overlap
     * as the final fallback.
     */
    fun similarity(
        query: String,
        candidateText: String,
    ): Double {
        val q = query.trim().lowercase()
        val c = candidateText.trim().lowercase()
        if (q.isEmpty() || c.isEmpty()) return 0.0
        if (q == c) return 1.0

        val qSquashed = q.filter { it.isLetterOrDigit() }
        val cSquashed = c.filter { it.isLetterOrDigit() }
        if (qSquashed.isNotEmpty() && qSquashed == cSquashed) return 0.95

        if (c.contains(q)) return 0.7 + 0.25 * (q.length.toDouble() / c.length)
        if (q.contains(c)) return 0.55 + 0.15 * (c.length.toDouble() / q.length)

        if (qSquashed.isNotEmpty() && cSquashed.contains(qSquashed)) {
            return 0.65 + 0.2 * (qSquashed.length.toDouble() / cSquashed.length)
        }
        if (cSquashed.isNotEmpty() && qSquashed.contains(cSquashed)) {
            return 0.5 + 0.1 * (cSquashed.length.toDouble() / qSquashed.length)
        }

        val qTokens = q.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()
        val cTokens = c.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()
        if (qTokens.isEmpty() || cTokens.isEmpty()) return 0.0
        val union = qTokens.union(cTokens).size
        return if (union == 0) 0.0 else qTokens.intersect(cTokens).size.toDouble() / union
    }
}
