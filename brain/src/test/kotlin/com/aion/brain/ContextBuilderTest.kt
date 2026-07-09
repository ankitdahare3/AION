package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private const val PERSONA = "You are AION, a personal on-device assistant."
private const val SAFETY = "Never execute a side-effect action without explicit approval."

private fun msg(content: String) = Msg(role = "user", content = content)

class ContextBuilderTest {
    @Test
    fun `short history fits entirely, nothing dropped`() {
        val history = (1..3).map { msg("turn $it") }
        val result = ContextBuilder.build(PERSONA, SAFETY, history)

        assertEquals(3, result.messages.size)
        assertEquals(0, result.turnsDropped)
        assertTrue(result.estimatedTokens <= ContextBuilder.DEFAULT_MAX_TOKENS)
    }

    @Test
    fun `history longer than N=6 turns is capped to the last 6`() {
        val history = (1..20).map { msg("turn $it") }
        val result = ContextBuilder.build(PERSONA, SAFETY, history)

        assertTrue(result.messages.size <= ContextBuilder.HISTORY_TURNS)
        // the most recent turns must be the ones kept
        assertEquals("turn 20", result.messages.last().content)
    }

    @Test
    fun `oversized turns get dropped oldest-first until the budget fits`() {
        val bigTurn = "x".repeat(10_000) // ~2500 tokens each at 4 chars/token
        val history = (1..6).map { msg(bigTurn) }
        val result = ContextBuilder.build(PERSONA, SAFETY, history, maxTokens = 4000)

        assertTrue(result.estimatedTokens <= 4000)
        assertTrue(result.turnsDropped > 0)
        assertTrue(result.messages.size < 6)
    }

    @Test
    fun `empty history still respects the budget, just the prefix`() {
        val result = ContextBuilder.build(PERSONA, SAFETY, emptyList())
        assertEquals(0, result.messages.size)
        assertEquals(ContextBuilder.estimateTokens("$PERSONA\n\n$SAFETY"), result.estimatedTokens)
    }

    @Test
    fun `persona plus safety prefix exceeding the budget alone throws rather than silently truncating safety text`() {
        val hugePersona = "p".repeat(100_000)
        assertThrows(IllegalArgumentException::class.java) {
            ContextBuilder.build(hugePersona, SAFETY, emptyList(), maxTokens = 100)
        }
    }

    @Test
    fun `token budget is never exceeded across a wide range of history sizes and turn lengths`() {
        val turnLengths = listOf(1, 10, 100, 500, 1_000, 5_000, 20_000)
        val historyCounts = listOf(0, 1, 3, 6, 10, 50)

        for (len in turnLengths) {
            for (count in historyCounts) {
                val history = (1..count).map { msg("t".repeat(len)) }
                val result = ContextBuilder.build(PERSONA, SAFETY, history)
                assertTrue(
                    "budget exceeded for len=$len count=$count: ${result.estimatedTokens} > ${ContextBuilder.DEFAULT_MAX_TOKENS}",
                    result.estimatedTokens <= ContextBuilder.DEFAULT_MAX_TOKENS,
                )
            }
        }
    }

    @Test
    fun `safety text always survives even when history is trimmed to zero`() {
        val bigTurn = "x".repeat(50_000)
        val history = (1..6).map { msg(bigTurn) }
        val result = ContextBuilder.build(PERSONA, SAFETY, history, maxTokens = 200)

        assertTrue(result.messages.isEmpty())
        assertTrue(result.system.contains(SAFETY))
        assertTrue(result.estimatedTokens <= 200)
    }
}
