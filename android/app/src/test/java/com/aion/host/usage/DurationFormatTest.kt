package com.aion.host.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {
    @Test
    fun `under an hour shows minutes only`() {
        assertEquals("42m", formatDuration(42 * 60_000L))
    }

    @Test
    fun `over an hour shows hours and minutes`() {
        assertEquals("5h 42m", formatDuration((5 * 60 + 42) * 60_000L))
    }

    @Test
    fun `zero formats as zero minutes, not a crash`() {
        assertEquals("0m", formatDuration(0L))
    }
}
