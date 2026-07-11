package com.aion.host.brain

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aion.brain.BrainRequest
import com.aion.brain.Msg
import com.aion.brain.Provider
import com.aion.brain.ProviderCaps
import com.aion.brain.TaskType
import com.aion.brain.Tier
import com.aion.brain.providers.GeminiProvider
import com.aion.brain.providers.OpenAiCompatProvider
import com.aion.host.security.ProviderKey
import com.aion.host.security.SecretVault
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * T-121-adjacent — real, live proof that a real API key set in [SecretVault] actually reaches its
 * real provider endpoint (this project's own choice of base URL + model, not just "some request
 * succeeded") and gets a real response back. Each provider is called directly, not through
 * [ProviderRouter] — routing/scoring is already covered elsewhere (T-112); this isolates exactly
 * one thing per case: does THIS key + endpoint + model combination actually work.
 *
 * Not part of any CI-run suite's normal expectations: CI has no real keys installed, so every case
 * skips cleanly rather than failing when its key is absent.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LiveProviderSmokeTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var secretVault: SecretVault

    @Before
    fun setup() {
        hiltRule.inject()
    }

    // Some free-tier models (e.g. OpenRouter's tencent/hy3:free) spend a real chunk of the budget
    // on hidden reasoning tokens before emitting final content — verified via direct curl that
    // 20 tokens wasn't enough for that model to ever finish, 600 reliably is.
    private val req =
        BrainRequest(
            taskType = TaskType.CHAT,
            system = "Reply with exactly one word.",
            messages = listOf(Msg("user", "Say hello.")),
            maxTokens = 600,
        )

    @Test
    fun groqRespondsToARealPrompt() =
        liveCheck(ProviderKey.GROQ) {
            OpenAiCompatProvider(
                id = "groq",
                tier = Tier.FREE,
                caps = ProviderCaps(),
                endpoint = "https://api.groq.com/openai/v1",
                model = "llama-3.3-70b-versatile",
                apiKey = it,
            )
        }

    @Test
    fun openRouterRespondsToARealPrompt() =
        liveCheck(ProviderKey.OPENROUTER) {
            OpenAiCompatProvider(
                id = "openrouter",
                tier = Tier.FREE,
                caps = ProviderCaps(),
                endpoint = "https://openrouter.ai/api/v1",
                model = "tencent/hy3:free",
                apiKey = it,
            )
        }

    @Test
    fun nvidiaRespondsToARealPrompt() =
        liveCheck(ProviderKey.NVIDIA) {
            OpenAiCompatProvider(
                id = "nvidia",
                tier = Tier.FREE,
                caps = ProviderCaps(),
                endpoint = "https://integrate.api.nvidia.com/v1",
                model = "meta/llama-3.1-8b-instruct",
                apiKey = it,
            )
        }

    @Test
    fun geminiRespondsToARealPrompt() =
        liveCheck(ProviderKey.GEMINI) {
            GeminiProvider(
                id = "gemini",
                tier = Tier.FREE,
                caps = ProviderCaps(),
                endpoint = "https://generativelanguage.googleapis.com/v1beta",
                model = "gemini-2.0-flash",
                apiKey = it,
            )
        }

    private fun liveCheck(
        key: ProviderKey,
        build: (String) -> Provider,
    ) = runTest {
        val apiKey = secretVault.get(key)
        if (apiKey.isNullOrBlank()) return@runTest // no real key set on this device — nothing to prove here

        val provider = build(apiKey)
        val result = provider.complete(req)

        assertTrue("expected a non-blank real response from ${provider.id}", result.text.isNotBlank())
        android.util.Log.i("LiveProviderSmokeTest", "${provider.id} -> \"${result.text}\" (${result.latencyMs}ms)")
    }
}
