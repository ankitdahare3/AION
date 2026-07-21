package com.aion.host.di

import com.aion.brain.ProviderCapsConfig
import com.aion.brain.ProviderConfig
import com.aion.brain.ProviderCostConfig
import com.aion.brain.Tier
import com.aion.brain.providers.GeminiProvider
import com.aion.brain.providers.OpenAiCompatProvider
import com.aion.host.security.ProviderKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** T-167 — providers.yaml is now the real source of truth for the provider registry; these cover the mapping logic that used to be a hardcoded list. */
class ProvidersModuleTest {
    @Test
    fun `known provider ids map to their real SecretVault key`() {
        assertEquals(ProviderKey.GROQ, ProvidersModule.keyFor("groq"))
        assertEquals(ProviderKey.OPENROUTER, ProvidersModule.keyFor("openrouter"))
        assertEquals(ProviderKey.NVIDIA, ProvidersModule.keyFor("nvidia"))
        assertEquals(ProviderKey.GEMINI, ProvidersModule.keyFor("gemini"))
        assertEquals(ProviderKey.OPENAI, ProvidersModule.keyFor("openai"))
        assertEquals(ProviderKey.DEEPSEEK, ProvidersModule.keyFor("deepseek"))
    }

    @Test
    fun `an unrecognized provider id has no key`() {
        assertNull(ProvidersModule.keyFor("some-future-provider"))
    }

    @Test
    fun `tier strings map to the real Tier enum, unknown tiers are rejected`() {
        assertEquals(Tier.LOCAL, ProvidersModule.tierFor("local"))
        assertEquals(Tier.FREE, ProvidersModule.tierFor("free"))
        assertEquals(Tier.PAID, ProvidersModule.tierFor("paid"))
        assertNull(ProvidersModule.tierFor("bogus"))
    }

    @Test
    fun `an openai_compat config builds a real OpenAiCompatProvider`() {
        val config =
            ProviderConfig(
                id = "groq",
                kind = "openai_compat",
                endpoint = "https://api.groq.com/openai/v1",
                models = listOf("llama-3.3-70b-versatile"),
                tier = "free",
                privacy = "cloud",
            )

        val provider = ProvidersModule.buildProvider(config, apiKey = "key123")

        assertEquals(OpenAiCompatProvider::class, provider!!::class)
        assertEquals("groq", provider.id)
        assertEquals(Tier.FREE, provider.tier)
    }

    @Test
    fun `a google config builds a real GeminiProvider`() {
        val config =
            ProviderConfig(
                id = "gemini",
                kind = "google",
                endpoint = "https://generativelanguage.googleapis.com/v1beta",
                models = listOf("gemini-2.0-flash"),
                tier = "paid",
                privacy = "cloud",
            )

        val provider = ProvidersModule.buildProvider(config, apiKey = "key123")

        assertEquals(GeminiProvider::class, provider!!::class)
        assertEquals("gemini", provider.id)
        assertEquals(Tier.PAID, provider.tier)
    }

    @Test
    fun `an unsupported kind is skipped instead of guessed at`() {
        val config = ProviderConfig(id = "local-llamacpp", kind = "local", tier = "local", privacy = "on_device")

        assertNull(ProvidersModule.buildProvider(config, apiKey = "irrelevant"))
    }

    @Test
    fun `a config with no models produces no provider`() {
        val config =
            ProviderConfig(
                id = "groq",
                kind = "openai_compat",
                endpoint = "https://api.groq.com/openai/v1",
                models = emptyList(),
                tier = "free",
                privacy = "cloud",
            )

        assertNull(ProvidersModule.buildProvider(config, apiKey = "key123"))
    }

    @Test
    fun `a config with no endpoint produces no provider`() {
        val config =
            ProviderConfig(
                id = "groq",
                kind = "openai_compat",
                endpoint = null,
                models = listOf("llama-3.3-70b-versatile"),
                tier = "free",
                privacy = "cloud",
            )

        assertNull(ProvidersModule.buildProvider(config, apiKey = "key123"))
    }

    @Test
    fun `caps and cost from the config carry through to the built provider`() {
        val config =
            ProviderConfig(
                id = "gemini",
                kind = "google",
                endpoint = "https://generativelanguage.googleapis.com/v1beta",
                models = listOf("gemini-2.0-flash"),
                caps = ProviderCapsConfig(vision = true, tools = true, context = 1048576, stream = true),
                cost = ProviderCostConfig(costInPerMTok = 0.075, costOutPerMTok = 0.30),
                tier = "paid",
                privacy = "cloud",
            )

        val provider = ProvidersModule.buildProvider(config, apiKey = "key123")!!

        assertEquals(true, provider.caps.vision)
        assertEquals(1048576, provider.caps.contextTokens)
    }
}
