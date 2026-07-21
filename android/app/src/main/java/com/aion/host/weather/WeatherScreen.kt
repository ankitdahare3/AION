package com.aion.host.weather

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors

private sealed interface WeatherUiState {
    data object Loading : WeatherUiState

    data object NoLocation : WeatherUiState

    data class Loaded(
        val reading: WeatherReading,
    ) : WeatherUiState

    data class Failed(
        val message: String,
    ) : WeatherUiState
}

/**
 * T-160 (EPIC 17) — real current weather (Open-Meteo, free, no API key) at the device's own real
 * last-known location. [resumeSignal] (same pattern as every other real-data screen) re-reads on
 * resume so a just-granted Location permission shows real data without navigating away and back.
 */
@Composable
fun WeatherScreen(
    resumeSignal: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<WeatherUiState>(WeatherUiState.Loading) }

    LaunchedEffect(resumeSignal) {
        state = WeatherUiState.Loading
        val location = WeatherReader(context).lastKnownLocation()
        state =
            if (location == null) {
                WeatherUiState.NoLocation
            } else {
                try {
                    WeatherUiState.Loaded(WeatherReader(context).currentWeather(location.latitude, location.longitude))
                } catch (e: Exception) {
                    WeatherUiState.Failed(e.message ?: e.javaClass.simpleName)
                }
            }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Weather", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)
        when (val s = state) {
            WeatherUiState.Loading ->
                Text(
                    "Loading…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AionColors.OnSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            WeatherUiState.NoLocation ->
                Text(
                    "No location available, or Location access hasn't been granted yet (Setup screen).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AionColors.OnSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            is WeatherUiState.Failed ->
                Text(
                    "Couldn't fetch weather right now: ${s.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AionColors.Error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            is WeatherUiState.Loaded -> {
                Text(
                    "${s.reading.temperatureC.toInt()}°C",
                    style = MaterialTheme.typography.displaySmall,
                    color = AionColors.OnBackground,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    s.reading.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AionColors.OnSurfaceVariant,
                )
                Text(
                    "Humidity ${s.reading.humidityPercent}% · Wind ${s.reading.windSpeedKmh.toInt()} km/h",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AionColors.OnSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
