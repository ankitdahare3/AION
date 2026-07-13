package com.aion.host.usage

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class UsageStatsReaderTest {
    @Test
    fun `start of day is midnight in the system time zone`() {
        val zone = ZoneId.systemDefault()
        val noon = ZonedDateTime.of(2026, 7, 13, 12, 30, 0, 0, zone).toInstant().toEpochMilli()

        val start = startOfDayMs(noon)

        assertEquals(ZonedDateTime.of(2026, 7, 13, 0, 0, 0, 0, zone).toInstant().toEpochMilli(), start)
    }
}
