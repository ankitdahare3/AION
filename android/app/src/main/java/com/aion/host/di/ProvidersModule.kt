package com.aion.host.di

import com.aion.brain.BudgetGuard
import com.aion.brain.ExploringScoreStore
import com.aion.brain.Provider
import com.aion.brain.ProviderCaps
import com.aion.brain.ProviderRouter
import com.aion.brain.ScoreStore
import com.aion.brain.Tier
import com.aion.brain.providers.GeminiProvider
import com.aion.brain.providers.OpenAiCompatProvider
import com.aion.host.security.ProviderKey
import com.aion.host.security.SecretVault
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DOC-013 — first real wiring of `:brain`'s Provider/Router system into the live app. Everything
 * upstream of this (ProviderRouter, ScoreStore/RoomScoreStore, ExploringScoreStore, the individual
 * Provider adapters) already existed from T-030/T-112; nothing ever actually constructed a real
 * `Provider` from a real key and handed it to a router, a gap T-112/T-113 both found and documented.
 *
 * A provider only appears in the registry if its key is actually set in [SecretVault] — an unset
 * key just means fewer candidates, not a crash; [ProviderRouter.route] already handles an empty or
 * short candidate list gracefully (a caller with zero configured providers gets a clear
 * "No provider available" failure, not a null-pointer surprise).
 */
@Module
@InstallIn(SingletonComponent::class)
object ProvidersModule {
    @Provides
    @Singleton
    fun provideProviderRegistry(secretVault: SecretVault): List<Provider> =
        listOfNotNull(
            secretVault.get(ProviderKey.GROQ)?.takeIf { it.isNotBlank() }?.let {
                OpenAiCompatProvider(
                    id = "groq",
                    tier = Tier.FREE,
                    caps = ProviderCaps(),
                    endpoint = "https://api.groq.com/openai/v1",
                    model = "llama-3.3-70b-versatile",
                    apiKey = it,
                )
            },
            secretVault.get(ProviderKey.OPENROUTER)?.takeIf { it.isNotBlank() }?.let {
                OpenAiCompatProvider(
                    id = "openrouter",
                    tier = Tier.FREE,
                    caps = ProviderCaps(),
                    endpoint = "https://openrouter.ai/api/v1",
                    model = "tencent/hy3:free",
                    apiKey = it,
                )
            },
            secretVault.get(ProviderKey.NVIDIA)?.takeIf { it.isNotBlank() }?.let {
                OpenAiCompatProvider(
                    id = "nvidia",
                    tier = Tier.FREE,
                    caps = ProviderCaps(),
                    endpoint = "https://integrate.api.nvidia.com/v1",
                    // The 70B variant hung indefinitely (verified via direct curl, not assumed) —
                    // this 8B one responds in well under a second; swap once the larger one's
                    // availability for this key/account is confirmed.
                    model = "meta/llama-3.1-8b-instruct",
                    apiKey = it,
                )
            },
            secretVault.get(ProviderKey.GEMINI)?.takeIf { it.isNotBlank() }?.let {
                GeminiProvider(
                    id = "gemini",
                    tier = Tier.FREE,
                    caps = ProviderCaps(),
                    endpoint = "https://generativelanguage.googleapis.com/v1beta",
                    // gemini-2.5-flash 404s as "no longer available to new users" for this key
                    // (verified via direct curl); 2.0-flash is the broadest-available fallback.
                    model = "gemini-2.0-flash",
                    apiKey = it,
                )
            },
        )

    @Provides
    @Singleton
    fun provideProviderRouter(
        registry: List<Provider>,
        scoreStore: ScoreStore,
        budget: BudgetGuard,
    ): ProviderRouter = ProviderRouter(registry, ExploringScoreStore(scoreStore), budget)
}
