package com.aion.host.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.aion.brain.providers.defaultProviderHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

data class WeatherReading(
    val temperatureC: Double,
    val description: String,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
)

@Serializable
private data class OpenMeteoCurrent(
    val temperature_2m: Double,
    val weather_code: Int,
    val relative_humidity_2m: Int,
    val wind_speed_10m: Double,
)

@Serializable
private data class OpenMeteoResponse(
    val current: OpenMeteoCurrent,
)

/**
 * T-160 (EPIC 17) — the owner's own free choice for weather: Open-Meteo (no API key, no signup)
 * over a paid provider, and the device's own last-known location over a hardcoded/user-typed city.
 * `ACCESS_COARSE_LOCATION` is enough — weather doesn't need GPS-grade precision.
 */
class WeatherReader(private val context: Context) {
    /** Real last-known location from whichever provider has one; null if none granted/available —
     * this is a bare read, not a fresh location request (no FusedLocationProviderClient dependency
     * for one weather lookup). */
    fun lastKnownLocation(): Location? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { provider ->
                try {
                    locationManager.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    null
                }
            }
            .maxByOrNull { it.time }
    }

    suspend fun currentWeather(latitude: Double, longitude: Double): WeatherReading {
        val client = defaultProviderHttpClient()
        val response =
            client
                .get("https://api.open-meteo.com/v1/forecast") {
                    parameter("latitude", latitude)
                    parameter("longitude", longitude)
                    parameter("current", "temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m")
                }
                .body<OpenMeteoResponse>()
        client.close()
        return WeatherReading(
            temperatureC = response.current.temperature_2m,
            description = describeWeatherCode(response.current.weather_code),
            humidityPercent = response.current.relative_humidity_2m,
            windSpeedKmh = response.current.wind_speed_10m,
        )
    }
}
