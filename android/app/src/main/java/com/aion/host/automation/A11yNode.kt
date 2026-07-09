package com.aion.host.automation

/** Screen-position rectangle, in pixels. */
data class Bounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

/**
 * DOC-009 §2 — an Android-independent mirror of the fields we need from
 * `android.view.accessibility.AccessibilityNodeInfo`. [AionAccessibilityService] maps the real
 * framework tree into this; [A11yTreeReader] (which does the actual compression) only ever sees
 * this model, so it's unit-testable on the plain JVM without an emulator.
 */
data class A11yNode(
    val className: String,
    val text: String? = null,
    val contentDesc: String? = null,
    val bounds: Bounds = Bounds(0, 0, 0, 0),
    val clickable: Boolean = false,
    val focusable: Boolean = false,
    val checkable: Boolean = false,
    val checked: Boolean = false,
    val scrollable: Boolean = false,
    val enabled: Boolean = true,
    val children: List<A11yNode> = emptyList(),
)

/** One compressed, addressable element surfaced to the Planner/ExecutorAgent (DOC-009 §2/§3). */
data class ElementRef(
    val id: String,
    val role: String,
    val text: String,
    val bounds: Bounds,
    val states: List<String>,
)
