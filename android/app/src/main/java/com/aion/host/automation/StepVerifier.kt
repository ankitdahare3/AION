package com.aion.host.automation

import com.aion.brain.TextSimilarity

enum class VerificationOutcome { PASS, FAIL, AMBIGUOUS }

data class VerificationResult(
    val outcome: VerificationOutcome,
    val confidence: Double,
    val reason: String,
)

/**
 * DOC-009 §4 — expected_observation vs post-action a11y diff. Pure text comparison: capturing the
 * before/after structured-text snapshots (and the 500ms debounce between action and "after") is the
 * caller's job (ActionDispatcher/ExecutorAgent), not this. confidence < 0.7 is the "hand off to
 * ReflectorAgent" signal per DOC-009.
 */
object StepVerifier {
    private const val CONFIDENCE_THRESHOLD = 0.7

    fun verify(
        expected: String,
        before: String,
        after: String,
    ): VerificationResult {
        val expectedNorm = expected.trim().lowercase()
        val changed = after.lowercase() != before.lowercase()

        val bestMatch = if (expectedNorm.isNotEmpty()) {
            after.lines().maxOfOrNull { line ->
                TextSimilarity.similarity(expectedNorm, line)
            } ?: 0.0
        } else {
            0.0
        }

        return when {
            expectedNorm.isNotEmpty() && (after.lowercase().contains(expectedNorm) || bestMatch >= CONFIDENCE_THRESHOLD) ->
                VerificationResult(VerificationOutcome.PASS, maxOf(0.95, bestMatch), "expected text found in post-action screen")
            !changed ->
                VerificationResult(VerificationOutcome.FAIL, 0.9, "screen did not change after the action")
            else ->
                VerificationResult(VerificationOutcome.AMBIGUOUS, 0.4, "screen changed but expected text not found")
        }
    }

    fun needsReflection(result: VerificationResult): Boolean = result.confidence < CONFIDENCE_THRESHOLD
}
