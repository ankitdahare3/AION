package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginManifestLoaderTest {
    // DOC-005 §2's own example manifest, verbatim.
    private val gmailManifestJson =
        """
        {
          "id": "com.aion.plugin.gmail",
          "name": "Gmail",
          "version": "1.2.0",
          "apiLevel": 1,
          "permissions": ["INTERNET", "READ_MAIL_SCOPE"],
          "tools": [
            {
              "name": "send_email",
              "sideEffect": true,
              "inputSchema": "{}",
              "description": "Sends an email via Gmail"
            }
          ],
          "dna": {"learn": true, "reflect": true, "benchmark": "bench/gmail.yaml", "update": true}
        }
        """.trimIndent()

    @Test
    fun `parses the real DOC-005 example manifest`() {
        val result = PluginManifestLoader.load(gmailManifestJson)

        require(result is ManifestLoadResult.Loaded)
        assertEquals("com.aion.plugin.gmail", result.manifest.id)
        assertEquals(1, result.manifest.apiLevel)
        assertEquals(listOf("INTERNET", "READ_MAIL_SCOPE"), result.manifest.permissions)
        assertEquals(1, result.manifest.tools.size)
        assertEquals(
            "send_email",
            result.manifest.tools
                .first()
                .name,
        )
        assertTrue(
            result.manifest.tools
                .first()
                .sideEffect,
        )
        assertEquals("bench/gmail.yaml", result.manifest.dna.benchmark)
    }

    @Test
    fun `malformed json is a parse error, not a crash`() {
        val result = PluginManifestLoader.load("{ not valid json")

        assertTrue(result is ManifestLoadResult.ParseError)
    }

    @Test
    fun `missing a required field is a parse error`() {
        val result = PluginManifestLoader.load("""{"name": "Gmail"}""")

        assertTrue(result is ManifestLoadResult.ParseError)
    }
}
