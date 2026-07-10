package com.aion.host.automation

import com.aion.brain.TextSimilarity

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
                .map { it to TextSimilarity.similarity(text, it.text) }
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
}
