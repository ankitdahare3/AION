package com.aion.host.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T-042 AC — 95% resolution on golden trees. Reuses T-040's 5 real `uiautomator dump` fixtures:
 * for each, a set of realistic natural-language queries a Planner might issue, asserting
 * [ElementResolver] finds the intended element with confidence >= 0.5.
 */
class ElementResolverGoldenTest {
    private data class Case(
        val fixture: String,
        val query: String,
        val expectedText: String,
    )

    private val cases =
        listOf(
            // tree1_home — launcher
            Case("tree1_home", "chrome", "Chrome"),
            Case("tree1_home", "chrome browser", "Chrome"),
            Case("tree1_home", "messages", "Messages"),
            Case("tree1_home", "voice search", "Voice search"),
            Case("tree1_home", "search", "Search"),
            // tree2_settings
            Case("tree2_settings", "network and internet", "Network & internet"),
            Case("tree2_settings", "search settings", "Search settings"),
            Case("tree2_settings", "connected devices", "Connected devices"),
            Case("tree2_settings", "apps", "Apps"),
            Case("tree2_settings", "notifications", "Notifications"),
            Case("tree2_settings", "battery", "Battery"),
            Case("tree2_settings", "storage", "Storage"),
            Case("tree2_settings", "sound and vibration", "Sound & vibration"),
            // tree3_wifi
            Case("tree3_wifi", "wifi", "Wi-Fi"),
            Case("tree3_wifi", "wi-fi", "Wi-Fi"),
            Case("tree3_wifi", "add network", "Add network"),
            Case("tree3_wifi", "saved networks", "Saved networks"),
            Case("tree3_wifi", "network preferences", "Network preferences"),
            Case("tree3_wifi", "scan qr code", "Scan QR code"),
            Case("tree3_wifi", "fix connectivity", "Fix connectivity"),
            Case("tree3_wifi", "navigate up", "Navigate up"),
            // tree4_aion — our own app
            Case("tree4_aion", "kill switch", "Show Kill-Switch"),
            Case("tree4_aion", "show kill switch", "Show Kill-Switch"),
            Case("tree4_aion", "api keys", "API Keys"),
            Case("tree4_aion", "back to setup", "Back to Setup"),
            Case("tree4_aion", "verify chain", "Verify chain"),
            // tree5_contacts
            Case("tree5_contacts", "add account", "ADD ACCOUNT"),
            Case("tree5_contacts", "import", "IMPORT"),
            Case("tree5_contacts", "create new contact", "Create new contact"),
            Case("tree5_contacts", "open navigation drawer", "Open navigation drawer"),
        )

    @Test
    fun `fuzzy-text resolution hits at least 95 percent across the golden-tree query set`() {
        val elementsByFixture = cases.map { it.fixture }.distinct().associateWith { elementsFor(it) }
        val failures = mutableListOf<String>()

        for (case in cases) {
            val resolved =
                ElementResolver.resolve(
                    elementsByFixture.getValue(case.fixture),
                    ResolveQuery.ByText(case.query),
                )
            if (resolved == null || resolved.element.text != case.expectedText) {
                failures += "[${case.fixture}] \"${case.query}\" -> expected \"${case.expectedText}\", got $resolved"
            }
        }

        val passRate = (cases.size - failures.size).toDouble() / cases.size
        assertTrue(
            "resolution rate $passRate below 95%% (${cases.size - failures.size}/${cases.size}); failures:\n" +
                failures.joinToString("\n"),
            passRate >= 0.95,
        )
    }

    @Test
    fun `exact id resolution always returns full confidence`() {
        val elements = elementsFor("tree4_aion")
        val target = elements.first { it.text == "API Keys" }

        val resolved = ElementResolver.resolve(elements, ResolveQuery.ById(target.id))

        assertEquals(target, resolved?.element)
        assertEquals(1.0, resolved?.confidence)
        assertEquals(ResolveMethod.EXACT_ID, resolved?.method)
    }

    @Test
    fun `a query with no plausible match resolves to null rather than a weak guess`() {
        val elements = elementsFor("tree1_home")

        val resolved = ElementResolver.resolve(elements, ResolveQuery.ByText("bluetooth settings"))

        assertNull(resolved)
    }

    private fun elementsFor(fixture: String): List<ElementRef> {
        val xml = readResource("/a11y-trees/$fixture.xml")
        return A11yTreeReader.read(UiAutomatorXmlParser.parse(xml))
    }

    private fun readResource(path: String): String =
        checkNotNull(javaClass.getResourceAsStream(path)) { "missing test resource: $path" }
            .bufferedReader()
            .readText()
}
