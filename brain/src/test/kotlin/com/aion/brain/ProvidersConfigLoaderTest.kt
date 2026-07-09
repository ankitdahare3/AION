package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProvidersConfigLoaderTest {
    private val sampleYaml =
        """
        providers:
          - id: local-llamacpp
            kind: local
            models: [qwen3-4b-int4]
            caps:
              vision: false
              tools: true
              context: 8192
              stream: true
            tier: local
            privacy: on_device
          - id: groq
            kind: openai_compat
            endpoint: https://api.groq.com/openai/v1
            models: [llama-3.3-70b-versatile]
            caps:
              tools: true
              context: 32768
            cost:
              in: 0.05
              out: 0.08
            tier: free
            privacy: cloud
        """.trimIndent()

    @Test
    fun `parses the full launch set fields`() {
        val registry = ProvidersConfigLoader.load(sampleYaml)
        assertEquals(2, registry.providers.size)

        val local = registry.providers[0]
        assertEquals("local-llamacpp", local.id)
        assertEquals("local", local.kind)
        assertEquals("local", local.tier)
        assertEquals("on_device", local.privacy)
        assertEquals(listOf("qwen3-4b-int4"), local.models)
        assertTrue(local.caps.tools)

        val groq = registry.providers[1]
        assertEquals("groq", groq.id)
        assertEquals("https://api.groq.com/openai/v1", groq.endpoint)
        assertEquals(32768, groq.caps.context)
        assertEquals(0.05, groq.cost.costInPerMTok, 1e-9)
        assertEquals(0.08, groq.cost.costOutPerMTok, 1e-9)
        assertEquals("cloud", groq.privacy)
    }

    @Test
    fun `caps and cost fall back to defaults when omitted`() {
        val minimal =
            """
            providers:
              - id: bare-minimum
                kind: local
                tier: local
                privacy: on_device
            """.trimIndent()
        val registry = ProvidersConfigLoader.load(minimal)
        val provider = registry.providers.single()
        assertEquals(8192, provider.caps.context)
        assertTrue(provider.caps.tools)
        assertEquals(0.0, provider.cost.costInPerMTok, 1e-9)
    }
}
