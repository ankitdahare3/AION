package com.aion.host.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * T-138 (DOC-017 §5 T6) — gates the whole app behind a real biometric/device-credential check on
 * open. [canAuthenticate] on a device with neither enrolled biometrics nor a screen lock returns
 * false; the caller degrades gracefully (no gate) in that case rather than permanently locking the
 * owner out of their own device — same "build the mechanism, degrade gracefully when the platform
 * capability genuinely isn't there" pattern [ShizukuBridge] already uses. `DEVICE_CREDENTIAL` is
 * included alongside `BIOMETRIC_WEAK` so a PIN/pattern still works on a device with a screen lock
 * but no enrolled fingerprint; [androidx.biometric.BiometricPrompt.PromptInfo] forbids combining
 * `setNegativeButtonText` with a `DEVICE_CREDENTIAL` authenticator, so this never sets one — the
 * system's own PIN/pattern screen provides its own way out.
 */
object AppLockGate {
    private const val ALLOWED_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun canAuthenticate(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /** [onFailure] fires for both an explicit cancel and any real auth failure — both mean "stay locked". */
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ) {
        val promptInfo =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle("Unlock AION")
                .setSubtitle("Verify it's you before AION opens")
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build()
        val prompt =
            BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) = onFailure()

                    override fun onAuthenticationFailed() = onFailure()
                },
            )
        prompt.authenticate(promptInfo)
    }
}
