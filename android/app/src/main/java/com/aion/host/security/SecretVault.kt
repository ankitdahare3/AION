package com.aion.host.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** DOC-013 §5 — providers this alpha targets; each gets one key slot in [SecretVault]. */
enum class ProviderKey(
    val label: String,
    val prefKey: String,
) {
    GROQ("Groq API Key", "groq_api_key"),
    OPENROUTER("OpenRouter API Key", "openrouter_api_key"),
    GEMINI("Gemini API Key", "gemini_api_key"),
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
            val masterKey =
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
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
