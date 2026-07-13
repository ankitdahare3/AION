package com.aion.host.weather

/**
 * T-160 (EPIC 17) — maps Open-Meteo's WMO weather codes to a short human-readable description.
 * Pure function (no Android import), unit-tested without a device. Table per Open-Meteo's own
 * documented WMO 4677 code list — not exhaustive of every code, but covers every code Open-Meteo's
 * `current` endpoint actually returns.
 */
internal fun describeWeatherCode(code: Int): String =
    when (code) {
        0 -> "Clear sky"
        1, 2, 3 -> "Partly cloudy"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75 -> "Snow"
        77 -> "Snow grains"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }
