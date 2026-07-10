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

enum class ResolveMethod { EXACT_ID, FUZZY_TEXT, VISION_OCR }

data class ResolvedElement(
    val element: ElementRef,
    val confidence: Double,
    val method: ResolveMethod,
)

/**
 * DOC-009 §2 — exact-id → fuzzy-text chain, with DOC-012 §3's vision merge as the last resort:
 * a11y always wins on conflict (tried first; vision is only consulted when a11y found nothing),
 * matching DOC-012 §1's "vision is the fallback" role for cases like canvas apps/games where the
 * a11y tree is empty or partial. OCR blocks have no stable id, so [ResolveQuery.ById] never falls
 * through to vision — only [ResolveQuery.ByText] can.
 */
object ElementResolver {
    private const val FUZZY_THRESHOLD = 0.5

    fun resolve(
        elements: List<ElementRef>,
        query: ResolveQuery,
        vision: VisionObservation? = null,
    ): ResolvedElement? {
        val fromA11y =
            when (query) {
                is ResolveQuery.ById -> resolveById(elements, query.id)
                is ResolveQuery.ByText -> resolveByText(elements, query.text)
            }
        if (fromA11y != null || vision == null || query !is ResolveQuery.ByText) return fromA11y
        val ocrElements = vision.ocrBlocks.mapIndexed { i, block -> ocrToElement(block, i) }
        return resolveByText(ocrElements, query.text)?.copy(method = ResolveMethod.VISION_OCR)
    }

    private fun ocrToElement(
        block: OcrBlock,
        index: Int,
    ): ElementRef =
        ElementRef(
            id = "ocr:$index",
            role = "text",
            text = block.text,
            bounds = block.bounds,
            states = emptyList(),
        )

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
