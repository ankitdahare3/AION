package com.aion.host.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeDescriptionTest {
    @Test
    fun `known codes map to their real WMO description`() {
        assertEquals("Clear sky", describeWeatherCode(0))
        assertEquals("Partly cloudy", describeWeatherCode(2))
        assertEquals("Fog", describeWeatherCode(45))
        assertEquals("Rain", describeWeatherCode(63))
        assertEquals("Snow", describeWeatherCode(73))
        assertEquals("Thunderstorm", describeWeatherCode(95))
    }

    @Test
    fun `unrecognized code falls back to Unknown rather than crashing`() {
        assertEquals("Unknown", describeWeatherCode(-1))
        assertEquals("Unknown", describeWeatherCode(999))
    }
}
