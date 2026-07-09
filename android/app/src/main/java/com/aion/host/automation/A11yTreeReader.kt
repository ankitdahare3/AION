package com.aion.host.automation

import com.aion.brain.ContextBuilder

/**
 * DOC-009 §2 — compresses an [A11yNode] tree to interactive-only structured text, ≤2000 tokens.
 * Kept dependency-free from `android.view.accessibility` (only the caller, the real accessibility
 * service, touches that) so this is unit-testable on the plain JVM against golden fixtures.
 */
object A11yTreeReader {
    private const val MAX_TOKENS = 2000
    private const val BUCKET_PX = 50

    /** Walks [root], keeping only interactive nodes (DOC-009 §2), in document order. */
    fun read(root: A11yNode): List<ElementRef> {
        val elements = mutableListOf<ElementRef>()
        collectInteractive(root, elements)
        return elements
    }

    /** Renders [elements] as one line each, truncating (whole lines only) once the token budget is hit. */
    fun toStructuredText(elements: List<ElementRef>): String {
        val sb = StringBuilder()
        for (el in elements) {
            val statesText = el.states.joinToString(",")
            val line = "[${el.id}] ${el.role} \"${el.text}\"" + if (statesText.isEmpty()) "" else " $statesText"
            val candidate = if (sb.isEmpty()) line else "$sb\n$line"
            if (ContextBuilder.estimateTokens(candidate) > MAX_TOKENS) break
            sb.setLength(0)
            sb.append(candidate)
        }
        return sb.toString()
    }

    /** Convenience: [read] then [toStructuredText] in one call. */
    fun compress(root: A11yNode): String = toStructuredText(read(root))

    private fun collectInteractive(
        node: A11yNode,
        out: MutableList<ElementRef>,
    ) {
        val isInteractive = node.clickable || node.focusable || node.checkable || node.scrollable
        if (isInteractive) {
            out += toElementRef(node)
        }
        node.children.forEach { collectInteractive(it, out) }
    }

    private fun toElementRef(node: A11yNode): ElementRef {
        // Compose (and some View-based UIs) often put the visible label on a non-interactive
        // child TextView, not on the clickable wrapper itself — fall back to the first
        // descendant with text so labels like "Show Kill-Switch" aren't lost.
        val text = (node.text ?: node.contentDesc ?: firstDescendantText(node) ?: "").trim()
        val bucket = "${node.bounds.left / BUCKET_PX},${node.bounds.top / BUCKET_PX}"
        val role = node.className.substringAfterLast('.')
        val states =
            buildList {
                if (node.clickable) add("clickable")
                if (node.checkable) add(if (node.checked) "checked" else "unchecked")
                if (node.scrollable) add("scrollable")
                if (!node.enabled) add("disabled")
            }
        return ElementRef(
            id = stableId(role, text, bucket),
            role = role,
            text = text,
            bounds = node.bounds,
            states = states,
        )
    }

    /** Pre-order search for the first descendant carrying a non-blank text or contentDesc. */
    private fun firstDescendantText(node: A11yNode): String? {
        for (child in node.children) {
            val direct = child.text?.trim()?.ifBlank { null } ?: child.contentDesc?.trim()?.ifBlank { null }
            if (direct != null) return direct
            firstDescendantText(child)?.let { return it }
        }
        return null
    }

    private fun stableId(
        role: String,
        text: String,
        bucket: String,
    ): String = Integer.toHexString("$role|$text|$bucket".hashCode())
}
