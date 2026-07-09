package com.aion.host.automation

import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Test-only: parses a real `uiautomator dump` XML file into our [A11yNode] model, so golden-file
 * tests can exercise [A11yTreeReader] against actually-recorded screens instead of hand-built trees.
 */
object UiAutomatorXmlParser {
    fun parse(xml: String): A11yNode {
        val factory = DocumentBuilderFactory.newInstance()
        val doc = factory.newDocumentBuilder().parse(xml.byteInputStream())
        val hierarchy = doc.documentElement
        val rootNodeElement = firstChildElement(hierarchy) ?: error("no root <node> in uiautomator dump")
        return toA11yNode(rootNodeElement)
    }

    private fun toA11yNode(el: Element): A11yNode =
        A11yNode(
            className = el.getAttribute("class"),
            text = el.getAttribute("text").ifBlank { null },
            contentDesc = el.getAttribute("content-desc").ifBlank { null },
            bounds = parseBounds(el.getAttribute("bounds")),
            clickable = el.getAttribute("clickable") == "true",
            focusable = el.getAttribute("focusable") == "true",
            checkable = el.getAttribute("checkable") == "true",
            checked = el.getAttribute("checked") == "true",
            scrollable = el.getAttribute("scrollable") == "true",
            enabled = el.getAttribute("enabled") != "false",
            children = childElements(el).map { toA11yNode(it) },
        )

    /** Parses uiautomator's "[left,top][right,bottom]" bounds format. */
    private fun parseBounds(raw: String): Bounds {
        val nums = Regex("""-?\d+""").findAll(raw).map { it.value.toInt() }.toList()
        if (nums.size < 4) return Bounds(0, 0, 0, 0)
        return Bounds(nums[0], nums[1], nums[2], nums[3])
    }

    private fun firstChildElement(el: Element): Element? = childElements(el).firstOrNull()

    private fun childElements(el: Element): List<Element> {
        val out = mutableListOf<Element>()
        val nodes = el.childNodes
        for (i in 0 until nodes.length) {
            val n = nodes.item(i)
            if (n is Element) out += n
        }
        return out
    }
}
