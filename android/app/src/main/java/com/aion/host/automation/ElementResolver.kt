package com.aion.host.automation

/** What the caller is looking for: a previously-seen stable id, or a natural-language description. */
sealed class ResolveQuery {
    data class ById(
        val id: String,
    ) : ResolveQuery()

    data class ByText(
        val text: String,
    ) : ResolveQuery()
}

enum class ResolveMethod { EXACT_ID, FUZZY_TEXT }

data class ResolvedElement(
    val element: ElementRef,
    val confidence: Double,
    val method: ResolveMethod,
)

/**
 * DOC-009 §2 — exact-id → fuzzy-text chain. Vision fallback (DOC-012) isn't built yet; a caller
 * that gets `null` back (nothing resolved with sufficient confidence) is exactly the "hand off to
 * vision" signal once that system exists — tracked in BACKLOG.md, not stubbed here.
 */
object ElementResolver {
    private const val FUZZY_THRESHOLD = 0.5

    fun resolve(
        elements: List<ElementRef>,
        query: ResolveQuery,
    ): ResolvedElement? =
        when (query) {
            is ResolveQuery.ById -> resolveById(elements, query.id)
            is ResolveQuery.ByText -> resolveByText(elements, query.text)
        }

    private fun resolveById(
        elements: List<ElementRef>,
        id: String,
    ): ResolvedElement? = elements.find { it.id == id }?.let { ResolvedElement(it, 1.0, ResolveMethod.EXACT_ID) }

    private fun resolveByText(
        elements: List<ElementRef>,
        text: String,
    ): ResolvedElement? {
        val best =
            elements
                .filter { it.text.isNotBlank() }
                .map { it to similarity(text, it.text) }
                .maxByOrNull { it.second }
                ?: return null
        return if (best.second >=
            FUZZY_THRESHOLD
        ) {
            ResolvedElement(best.first, best.second, ResolveMethod.FUZZY_TEXT)
        } else {
            null
        }
    }

    /**
     * Normalized-equality → squashed-equality (ignores spacing/punctuation, e.g. "wifi" ~ "Wi-Fi")
     * → substring containment (candidate-contains-query scores higher than the reverse, since a
     * short candidate coincidentally inside a long query is a weaker signal) → token-set overlap
     * as the final fallback. Pure string math, no Android dependency — testable on the plain JVM.
     */
    private fun similarity(
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
