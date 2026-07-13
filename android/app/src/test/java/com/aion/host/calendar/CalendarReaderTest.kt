package com.aion.host.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CalendarReaderTest {
    @Test
    fun `day range spans exactly midnight to midnight in the system time zone`() {
        val zone = ZoneId.systemDefault()
        val noon = ZonedDateTime.of(2026, 7, 13, 12, 30, 0, 0, zone).toInstant().toEpochMilli()

        val (start, end) = dayRangeMs(noon)

        val expectedStart = ZonedDateTime.of(2026, 7, 13, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        val expectedEnd = ZonedDateTime.of(2026, 7, 14, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expectedStart, start)
        assertEquals(expectedEnd, end)
    }

    @Test
    fun `a moment just before midnight still belongs to that day, not the next`() {
        val zone = ZoneId.systemDefault()
        val almostMidnight = ZonedDateTime.of(2026, 7, 13, 23, 59, 59, 0, zone).toInstant().toEpochMilli()

        val (start, end) = dayRangeMs(almostMidnight)

        assertEquals(ZonedDateTime.of(2026, 7, 13, 0, 0, 0, 0, zone).toInstant().toEpochMilli(), start)
        assertEquals(ZonedDateTime.of(2026, 7, 14, 0, 0, 0, 0, zone).toInstant().toEpochMilli(), end)
    }
}
