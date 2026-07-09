package com.aion.host.automation

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * DOC-009 §2 — reads the active window's accessibility tree, maps it to the Android-independent
 * [A11yNode] model, and exposes compressed structured text via [A11yTreeReader]. Also the live
 * handle [ActionDispatcher] (T-041) dispatches gestures/actions through, via [instance] — only one
 * instance ever runs (the OS enforces a single connection per accessibility service).
 */
class AionAccessibilityService : AccessibilityService() {
    companion object {
        var instance: AionAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op — callers (ExecutorAgent, T-051) pull a snapshot on demand via currentScreenText()
        // rather than this service reacting to every window-state event.
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    /** Compressed snapshot of the active window (DOC-009 §2), or null if no window is available. */
    fun currentScreenText(): String? {
        val root = rootInActiveWindow ?: return null
        return try {
            A11yTreeReader.compress(toA11yNode(root))
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    /** All interactive elements on the active window right now, or empty if no window is available. */
    fun currentElements(): List<ElementRef> {
        val root = rootInActiveWindow ?: return emptyList()
        return try {
            A11yTreeReader.read(toA11yNode(root))
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    /**
     * Re-walks the live tree looking for the node whose stable id (DOC-009 §2 rule, via
     * [A11yTreeReader.idOf]) matches [id]. Caller owns the returned node and must recycle it.
     * Needed for actions like [ActionDispatcher.type] that require a live node, not just cached
     * bounds — unlike tap/swipe, which only need the coordinates already captured in [ElementRef].
     */
    fun findNodeById(id: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val match = findMatch(root, id)
        if (match !== root) {
            @Suppress("DEPRECATION")
            root.recycle()
        }
        return match
    }

    private fun findMatch(
        node: AccessibilityNodeInfo,
        targetId: String,
    ): AccessibilityNodeInfo? {
        if (A11yTreeReader.idOf(toA11yNode(node)) == targetId) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findMatch(child, targetId)
            if (found != null) {
                if (found !== child) {
                    @Suppress("DEPRECATION")
                    child.recycle()
                }
                return found
            }
            @Suppress("DEPRECATION")
            child.recycle()
        }
        return null
    }

    private fun toA11yNode(node: AccessibilityNodeInfo): A11yNode {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val children = mutableListOf<A11yNode>()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            children += toA11yNode(child)
            @Suppress("DEPRECATION")
            child.recycle()
        }

        return A11yNode(
            className = node.className?.toString() ?: "",
            text = node.text?.toString(),
            contentDesc = node.contentDescription?.toString(),
            bounds = Bounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
            clickable = node.isClickable,
            focusable = node.isFocusable,
            checkable = node.isCheckable,
            checked = node.isChecked,
            scrollable = node.isScrollable,
            enabled = node.isEnabled,
            children = children,
        )
    }
}
