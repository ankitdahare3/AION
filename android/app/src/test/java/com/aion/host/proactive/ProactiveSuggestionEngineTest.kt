package com.aion.host.proactive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProactiveSuggestionEngineTest {
    private val now = 1_000_000_000L

    @Test
    fun `no signals crossed produces no suggestions`() {
        val result =
            ProactiveSuggestionEngine.evaluate(
                batteryPercent = 80,
                charging = false,
                nowMs = now,
                nextEventStartMs = null,
                nextEventTitle = null,
                screenTimeTodayMs = 30 * 60_000L,
            )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `low battery while not charging suggests power saver`() {
        val result =
            ProactiveSuggestionEngine.evaluate(
                batteryPercent = 15,
                charging = false,
                nowMs = now,
                nextEventStartMs = null,
                nextEventTitle = null,
                screenTimeTodayMs = 0,
            )
        assertEquals(1, result.size)
        assertEquals(SuggestionKind.BATTERY_LOW, result[0].kind)
        assertTrue(result[0].message.contains("15%"))
    }

    @Test
    fun `low battery while charging does not suggest power saver`() {
        val result =
            ProactiveSuggestionEngine.evaluate(
                batteryPercent = 15,
                charging = true,
                nowMs = now,
                nextEventStartMs = null,
                nextEventTitle = null,
                screenTimeTodayMs = 0,
            )
        assertTrue(result.none { it.kind == SuggestionKind.BATTERY_LOW })
    }

    @Test
    fun `an event 10 minutes away triggers the upcoming-event suggestion`() {
        val result =
            ProactiveSuggestionEngine.evaluate(
                batteryPercent = 80,
                charging = false,
                nowMs = now,
                nextEventStartMs = now + 10 * 60_000L,
                nextEventTitle = "Team Standup",
                screenTimeTodayMs = 0,
            )
        assertEquals(1, result.size)
        assertEquals(SuggestionKind.UPCOMING_EVENT, result[0].kind)
        assertTrue(result[0].message.contains("Team Standup"))
    }

    @Test
    fun `an event far in the future does not trigger the suggestion`() {
        val result =
            ProactiveSuggestionEngine.evaluate(
                batteryPercent = 80,
                charging = false,
                nowMs = now,
                nextEventStartMs = now + 60 * 60_000L,
                nextEventTitle = "Team Standup",
                screenTimeTodayMs = 0,
            )
        assertTrue(result.none { it.kind == SuggestionKind.UPCOMING_EVENT })
    }

    @Test
    fun `an event that already started (negative minutes away) does not trigger the suggestion`() {
        val result =
            ProactiveSuggestionEngine.evaluate(
                batteryPercent = 80,
                charging = false,
                nowMs = now,
                nextEventStartMs = now - 5 * 60_000L,
                nextEventTitle = "Team Standup",
                screenTimeTodayMs = 0,
            )
        assertTrue(result.none { it.kind == SuggestionKind.UPCOMING_EVENT })
    }

    @Test
    fun `2 or more hours of screen time suggests a break`() {
        val result =
            ProactiveSuggestionEngine.evaluate(
                batteryPercent = 80,
                charging = false,
                nowMs = now,
                nextEventStartMs = null,
                nextEventTitle = null,
                screenTimeTodayMs = 2 * 60 * 60_000L,
            )
        assertEquals(1, result.size)
        assertEquals(SuggestionKind.TAKE_A_BREAK, result[0].kind)
    }

    @Test
    fun `all three signals crossed at once produce all three suggestions`() {
        val result =
            ProactiveSuggestionEngine.evaluate(
                batteryPercent = 10,
                charging = false,
                nowMs = now,
                nextEventStartMs = now + 5 * 60_000L,
                nextEventTitle = "Client Call",
                screenTimeTodayMs = 3 * 60 * 60_000L,
            )
        assertEquals(3, result.size)
        assertEquals(
            setOf(SuggestionKind.BATTERY_LOW, SuggestionKind.UPCOMING_EVENT, SuggestionKind.TAKE_A_BREAK),
            result.map { it.kind }.toSet(),
        )
    }
}
