package com.aion.host.automation

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * DOC-009 §2 — reads the active window's accessibility tree, maps it to the Android-independent
 * [A11yNode] model, and exposes compressed structured text via [A11yTreeReader]. Read-only for
 * now; dispatching actions (tap/swipe/type) is T-041's ActionDispatcher, not this service.
 */
class AionAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op — callers (ExecutorAgent, T-051) pull a snapshot on demand via currentScreenText()
        // rather than this service reacting to every window-state event.
    }

    override fun onInterrupt() {}

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
