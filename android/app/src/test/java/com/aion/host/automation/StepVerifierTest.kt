package com.aion.host.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepVerifierTest {
    @Test
    fun `pass when expected text appears after the action`() {
        val result = StepVerifier.verify("Wi-Fi", before = "Settings home", after = "Wi-Fi settings screen")

        assertEquals(VerificationOutcome.PASS, result.outcome)
        assertFalse(StepVerifier.needsReflection(result))
    }

    @Test
    fun `pass is case-insensitive`() {
        val result = StepVerifier.verify("WI-FI", before = "x", after = "wi-fi settings")

        assertEquals(VerificationOutcome.PASS, result.outcome)
    }

    @Test
    fun `fail when the screen did not change at all`() {
        val result = StepVerifier.verify("Wi-Fi", before = "Settings home", after = "Settings home")

        assertEquals(VerificationOutcome.FAIL, result.outcome)
        assertFalse(StepVerifier.needsReflection(result))
    }

    @Test
    fun `ambiguous when the screen changed but expected text is missing`() {
        val result = StepVerifier.verify("Wi-Fi", before = "Settings home", after = "Battery settings")

        assertEquals(VerificationOutcome.AMBIGUOUS, result.outcome)
        assertTrue(StepVerifier.needsReflection(result))
    }

    @Test
    fun `blank expectation falls back to change detection`() {
        val unchanged = StepVerifier.verify("", before = "same", after = "same")
        val changed = StepVerifier.verify("", before = "a", after = "b")

        assertEquals(VerificationOutcome.FAIL, unchanged.outcome)
        assertEquals(VerificationOutcome.AMBIGUOUS, changed.outcome)
    }

    @Test
    fun `pass when expected text is fuzzy matched in post-action screen`() {
        // "Wi-Fi is ON" vs "Wi-Fi: ON" (will be stripped to "wifiison" vs "wifion" by TextSimilarity which scores > 0.7)
        val result = StepVerifier.verify("Wi-Fi is ON", before = "Settings home", after = "Wi-Fi: ON")

        assertEquals(VerificationOutcome.PASS, result.outcome)
        assertFalse(StepVerifier.needsReflection(result))
    }
}

