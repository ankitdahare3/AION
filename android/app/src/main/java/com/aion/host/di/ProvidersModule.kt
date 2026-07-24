package com.aion.host.di

import android.content.Context
import com.aion.brain.BudgetGuard
import com.aion.brain.ExploringScoreStore
import com.aion.brain.Provider
import com.aion.brain.ProviderCaps
import com.aion.brain.ProviderConfig
import com.aion.brain.ProviderRouter
import com.aion.brain.ProvidersConfigLoader
import com.aion.brain.ScoreStore
import com.aion.brain.Tier
import com.aion.brain.providers.AnthropicProvider
import com.aion.brain.providers.GeminiProvider
import com.aion.brain.providers.OpenAiCompatProvider
import com.aion.host.security.ProviderKey
import com.aion.host.security.SecretVault
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DOC-013 — first real wiring of `:brain`'s Provider/Router system into the live app, and (T-167)
 * genuinely config-driven now: `assets/providers.yaml` is the single source of truth for which
 * providers exist and their endpoint/model/tier/cost. Before T-167 this was a hardcoded list that
 * had silently drifted out of sync with providers.yaml's own claims (BACKLOG.md) — ProvidersConfig
 * existed and was unit-tested but had zero production callers.
 *
 * A provider only appears in the registry if BOTH its `kind` maps to a real adapter today
 * (`openai_compat` -> [OpenAiCompatProvider], `google` -> [GeminiProvider], `anthropic` ->
 * [AnthropicProvider]; anything else is skipped, not guessed at) AND its key is actually set in
 * [SecretVault] — either just means fewer candidates, not a crash; [ProviderRouter.route] already
 * handles an empty or short candidate list gracefully (a caller with zero configured providers
 * gets a clear "No provider available" failure).
 */
@Module
@InstallIn(SingletonComponent::class)
object ProvidersModule {
    // providers.yaml's own `id` is the stable key here — it's also what ScoreStore/BudgetGuard
    // persist provider stats under, so this mapping is the only place that needs to change if a
    // provider's id ever moves; an id with no entry just means no SecretVault key exists for it yet.
    internal fun keyFor(id: String): ProviderKey? =
        when (id) {
            "groq" -> ProviderKey.GROQ
            "openrouter" -> ProviderKey.OPENROUTER
            "nvidia" -> ProviderKey.NVIDIA
            "gemini" -> ProviderKey.GEMINI
            "openai" -> ProviderKey.OPENAI
            "deepseek" -> ProviderKey.DEEPSEEK
            "anthropic" -> ProviderKey.ANTHROPIC
            else -> null
        }

    internal fun tierFor(tier: String): Tier? =
        when (tier) {
            "local" -> Tier.LOCAL
            "free" -> Tier.FREE
            "paid" -> Tier.PAID
            else -> null
        }

    internal fun buildProvider(
        config: ProviderConfig,
        apiKey: String,
    ): Provider? {
        val tier = tierFor(config.tier) ?: return null
        val model = config.models.firstOrNull() ?: return null
        val endpoint = config.endpoint ?: return null
        val caps =
            ProviderCaps(
                vision = config.caps.vision,
                tools = config.caps.tools,
                contextTokens = config.caps.context,
                stream = config.caps.stream,
            )
        return when (config.kind) {
            "openai_compat" ->
                OpenAiCompatProvider(
                    id = config.id,
                    tier = tier,
                    caps = caps,
                    endpoint = endpoint,
                    model = model,
                    apiKey = apiKey,
                    costInPerMTok = config.cost.costInPerMTok,
                    costOutPerMTok = config.cost.costOutPerMTok,
                )
            "google" ->
                GeminiProvider(
                    id = config.id,
                    tier = tier,
                    caps = caps,
                    endpoint = endpoint,
                    model = model,
                    apiKey = apiKey,
                    costInPerMTok = config.cost.costInPerMTok,
                    costOutPerMTok = config.cost.costOutPerMTok,
                )
            "anthropic" ->
                AnthropicProvider(
                    id = config.id,
                    tier = tier,
                    caps = caps,
                    endpoint = endpoint,
                    model = model,
                    apiKey = apiKey,
                    costInPerMTok = config.cost.costInPerMTok,
                    costOutPerMTok = config.cost.costOutPerMTok,
                )
            else -> null
        }
    }

    // @JvmSuppressWildcards — without it, Dagger's Hilt *test* components (only surfaced once a
    // second @HiltAndroidTest class also injected ProviderRouter) see List<Provider> and
    // List<? extends Provider> as mismatched binding types; this forces the plain Java signature.
    @Provides
    @Singleton
    fun provideProviderRegistry(
        @ApplicationContext context: Context,
        secretVault: SecretVault,
    ): @JvmSuppressWildcards List<Provider> {
        val yaml =
            context.assets
                .open("providers.yaml")
                .bufferedReader()
                .use { it.readText() }
        return ProvidersConfigLoader
            .load(yaml)
            .providers
            .mapNotNull { config ->
                val key = keyFor(config.id) ?: return@mapNotNull null
                val apiKey = secretVault.get(key)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                buildProvider(config, apiKey)
            }
    }

    @Provides
    @Singleton
    fun provideProviderRouter(
        registry: @JvmSuppressWildcards List<Provider>,
        scoreStore: ScoreStore,
        budget: BudgetGuard,
    ): ProviderRouter = ProviderRouter(registry, ExploringScoreStore(scoreStore), budget)
}
