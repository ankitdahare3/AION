package com.aion.host.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** DOC-013 §5 / DOC-005 §5 — secrets this alpha needs (LLM providers + plugin API tokens); each gets one key slot in [SecretVault]. */
enum class ProviderKey(
    val label: String,
    val prefKey: String,
) {
    GROQ("Groq API Key", "groq_api_key"),
    OPENROUTER("OpenRouter API Key", "openrouter_api_key"),
    GEMINI("Gemini API Key", "gemini_api_key"),
    NVIDIA("NVIDIA API Key", "nvidia_api_key"),

    // T-102 — GmailPlugin expects a bare OAuth2 access token (no client id/secret flow built here,
    // BACKLOG.md); TelegramPlugin expects a bot token from @BotFather, a single static secret.
    GMAIL_ACCESS_TOKEN("Gmail Access Token", "gmail_access_token"),
    TELEGRAM_BOT_TOKEN("Telegram Bot Token", "telegram_bot_token"),
}

/**
 * SR-08 (DOC-017 §2 T4) — API keys live only in Android Keystore-backed encrypted storage, never
 * logged. `android:allowBackup="false"` (AndroidManifest, T-004) already excludes this file from
 * auto-backup/device-transfer, so no separate backup-exclusion rules are needed here.
 */
@Singleton
class SecretVault
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs: SharedPreferences by lazy {
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                buildMasterKey(context),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        // T-120 (DOC-017 T4) — StrongBox where available: a real, physically separate secure
        // element, a step up from the TEE-backed Keystore alone. Requesting it throws on hardware
        // that doesn't have it (most budget/older devices), so this falls back to the plain
        // TEE-backed key rather than crashing SecretVault construction on those devices.
        private fun buildMasterKey(context: Context): MasterKey =
            try {
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .setRequestStrongBoxBacked(true)
                    .build()
            } catch (e: Exception) {
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            }

        fun put(
            key: ProviderKey,
            value: String,
        ) {
            prefs.edit().putString(key.prefKey, value).apply()
        }

        fun get(key: ProviderKey): String? = prefs.getString(key.prefKey, null)

        fun remove(key: ProviderKey) {
            prefs.edit().remove(key.prefKey).apply()
        }

        fun has(key: ProviderKey): Boolean = prefs.contains(key.prefKey)

        companion object {
            private const val FILE_NAME = "aion_secrets"
        }
    }
