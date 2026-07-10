package com.aion.host.di

import com.aion.host.security.ProviderKey
import com.aion.host.security.SecretVault
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

/** T-102 — qualifiers for [GmailPlugin]/[TelegramPlugin]'s API tokens, so [BuiltInPluginRegistry] can take plain `String`s (matching [com.aion.brain.providers.OpenAiCompatProvider]/[com.aion.brain.providers.GeminiProvider]'s own `apiKey: String` shape) instead of depending on [SecretVault] directly — a plain-JVM test can then pass literal strings with no Android/Keystore dependency at all. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GmailToken

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TelegramToken

@Module
@InstallIn(SingletonComponent::class)
object PluginTokenModule {
    @Provides
    @GmailToken
    fun provideGmailToken(vault: SecretVault): String = vault.get(ProviderKey.GMAIL_ACCESS_TOKEN).orEmpty()

    @Provides
    @TelegramToken
    fun provideTelegramToken(vault: SecretVault): String = vault.get(ProviderKey.TELEGRAM_BOT_TOKEN).orEmpty()
}
