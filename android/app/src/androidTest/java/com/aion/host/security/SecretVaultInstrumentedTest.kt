package com.aion.host.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * T-024 AC — real on-device test: a real Android Keystore is required (Robolectric can't fake
 * key generation/wrapping reliably), so this must be instrumented, not a JVM unit test.
 */
@RunWith(AndroidJUnit4::class)
class SecretVaultInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun clearVault() {
        val vault = SecretVault(context)
        ProviderKey.entries.forEach { vault.remove(it) }
    }

    @Test
    fun keySurvivesAcrossFreshInstances() {
        // Two separate SecretVault instances (independent EncryptedSharedPreferences wrappers,
        // same backing file) simulate the app restarting.
        val beforeRestart = SecretVault(context)
        beforeRestart.put(ProviderKey.GROQ, "test-secret-value-12345")

        val afterRestart = SecretVault(context)
        assertEquals("test-secret-value-12345", afterRestart.get(ProviderKey.GROQ))
    }

    @Test
    fun rawPreferencesFileNeverContainsPlaintextKeyOrValue() {
        val vault = SecretVault(context)
        vault.put(ProviderKey.GEMINI, "super-secret-plaintext-marker-9f2a")

        val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/aion_secrets.xml")
        val raw = prefsFile.readText()
        assertFalse("plaintext value leaked into the prefs file", raw.contains("super-secret-plaintext-marker-9f2a"))
        assertFalse("plaintext pref key name leaked into the prefs file", raw.contains(ProviderKey.GEMINI.prefKey))
    }

    @Test
    fun removedKeyIsGone() {
        val vault = SecretVault(context)
        vault.put(ProviderKey.OPENROUTER, "temp-value")
        vault.remove(ProviderKey.OPENROUTER)
        assertNull(vault.get(ProviderKey.OPENROUTER))
        assertFalse(vault.has(ProviderKey.OPENROUTER))
    }
}
