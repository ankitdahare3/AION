package com.aion.host.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T-040 AC — golden-file tests on 5 recorded trees (real `uiautomator dump` output from the
 * aion_test AVD: launcher home, Settings, Wi-Fi settings, our own AION app, Contacts).
 */
class A11yTreeReaderGoldenTest {
    private val fixtures =
        listOf(
            "tree1_home",
            "tree2_settings",
            "tree3_wifi",
            "tree4_aion",
            "tree5_contacts",
        )

    @Test
    fun `each recorded tree compresses to exactly its golden structured text`() {
        for (name in fixtures) {
            val xml = readResource("/a11y-trees/$name.xml")
            val golden = readResource("/a11y-golden/$name.txt")

            val tree = UiAutomatorXmlParser.parse(xml)
            val actual = A11yTreeReader.compress(tree)

            assertEquals("mismatch for fixture $name", golden.trimEnd(), actual.trimEnd())
        }
    }

    @Test
    fun `compression is deterministic across repeated runs on the same recorded tree`() {
        for (name in fixtures) {
            val xml = readResource("/a11y-trees/$name.xml")
            val tree = UiAutomatorXmlParser.parse(xml)
            assertEquals(A11yTreeReader.compress(tree), A11yTreeReader.compress(tree))
        }
    }

    @Test
    fun `every recorded tree compresses within the 2000-token budget`() {
        for (name in fixtures) {
            val xml = readResource("/a11y-trees/$name.xml")
            val tree = UiAutomatorXmlParser.parse(xml)
            val elements = A11yTreeReader.read(tree)
            val text = A11yTreeReader.toStructuredText(elements)
            assertTrue(
                "fixture $name exceeded the token budget",
                com.aion.brain.ContextBuilder
                    .estimateTokens(text) <= 2000,
            )
        }
    }

    private fun readResource(path: String): String =
        checkNotNull(javaClass.getResourceAsStream(path)) { "missing test resource: $path" }
            .bufferedReader()
            .readText()
}
