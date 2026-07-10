package com.aion.host.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ADR-019 (DOC-019) — SQLCipher at-rest DB encryption needs a passphrase; generated once on first
 * run and Keystore-backed the same way [SecretVault] protects API keys (separate file: this isn't
 * a provider key, doesn't belong in that enum-keyed store).
 */
@Singleton
class DbPassphraseStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs by lazy {
            val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        fun getOrCreatePassphrase(): CharArray {
            prefs.getString(KEY, null)?.let { return it.toCharArray() }
            val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val hex = bytes.joinToString("") { "%02x".format(it) }
            prefs.edit().putString(KEY, hex).apply()
            return hex.toCharArray()
        }

        private companion object {
            const val FILE_NAME = "aion_db_key"
            const val KEY = "passphrase"
        }
    }
