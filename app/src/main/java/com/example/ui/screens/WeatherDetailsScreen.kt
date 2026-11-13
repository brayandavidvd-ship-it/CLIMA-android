package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.WeatherProviderOption
import com.example.model.WeatherSnapshot
import com.example.model.WeatherVisualState
import com.example.ui.atmospheric.AtmosphericBackground
import com.example.ui.components.GlassSurface
import com.example.ui.state.ClimaUiState
import kotlin.math.roundToInt

@Composable
fun WeatherDetailsScreen(
    uiState: ClimaUiState,
    onNavigateBack: () -> Unit,
) {
    val snapshot = uiState.activeSnapshot
    val visualState = snapshot?.visualState ?: WeatherVisualState.CLOUDY
    val isDay = snapshot?.isDay ?: true

    Box(modifier = Modifier.fillMaxSize()) {
        AtmosphericBackground(
            visualState = visualState,
            isDay = isDay,
            reduceAnimations = uiState.settings.reduceAnimations,
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                ) {
                    // Header with back button and city title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("details_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back_content_description),
                                tint = Color.White,
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = snapshot?.cityName ?: stringResource(R.string.your_location_fallback),
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("details_city_title"),
                            )
                            if (snapshot != null) {
                                val providerLabel = WeatherProviderOption
                                    .fromId(snapshot.providerId)
                                    .shortLabel
                                Text(
                                    text = "${snapshot.temperatureC.roundToInt()}° · ${snapshot.conditionLabel} · $providerLabel",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.82f),
                                )
                            }
                        }
                    }

                    if (snapshot != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Row 1: Wind & Relative Humidity
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                DetailMetricCard(
                                    icon = Icons.Rounded.Air,
                                    title = stringResource(R.string.details_wind_title),
                                    value = "${snapshot.windSpeedKmh.roundToInt()} km/h",
                                    subtitle = windDescriptor(snapshot.windSpeedKmh),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_card_wind"),
                                )
                                DetailMetricCard(
                                    icon = Icons.Rounded.WaterDrop,
                                    title = stringResource(R.string.details_humidity_title),
                                    value = "${snapshot.humidityPercent}%",
                                    subtitle = humidityDescriptor(snapshot.humidityPercent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_card_humidity"),
                                )
                            }

                            // Row 2: Max UV Index & Atmospheric Pressure
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                DetailMetricCard(
                                    icon = Icons.Rounded.WbSunny,
                                    title = stringResource(R.string.details_uv_title),
                                    value = "${snapshot.uvIndexMax.roundToInt()}",
                                    subtitle = uvDescriptor(snapshot.uvIndexMax),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_card_uv"),
                                )
                                DetailMetricCard(
                                    icon = Icons.Rounded.Compress,
                                    title = stringResource(R.string.details_pressure_title),
                                    value = "${snapshot.pressureHpa.roundToInt()} hPa",
                                    subtitle = pressureDescriptor(snapshot.pressureHpa),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_card_pressure"),
                                )
                            }

                            // Row 3: Rain Probability
                            DetailMetricCard(
                                icon = Icons.Rounded.Umbrella,
                                title = stringResource(R.string.details_rain_prob_title),
                                value = "${snapshot.precipitationProbabilityMax}%",
                                subtitle = "Sensación térmica ${snapshot.apparentTemperatureC.roundToInt()}° · Nubosidad ${snapshot.cloudCoverPercent}%",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_card_rain_probability"),
                            )

                            // Row 4: Solar Cycle (Sunrise / Sunset)
                            SolarCycleCard(
                                snapshot = snapshot,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_card_solar_cycle"),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier,
        cornerRadius = 15.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
private fun SolarCycleCard(
    snapshot: WeatherSnapshot,
    modifier: Modifier = Modifier,
) {
    val sunriseTime = formatIsoClock(snapshot.sunriseIso)
    val sunsetTime = formatIsoClock(snapshot.sunsetIso)

    GlassSurface(
        modifier = modifier,
        cornerRadius = 15.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WbTwilight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.details_solar_cycle_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.82f),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.details_sunrise_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.76f),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sunriseTime,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                        color = Color.White,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.details_sunset_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.76f),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sunsetTime,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

private fun formatIsoClock(isoString: String): String {
    val clock = isoString.substringAfter("T", "")
    return if (clock.length >= 5) clock.take(5) else "--:--"
}

private fun windDescriptor(kmh: Double): String = when {
    kmh < 10 -> "Brisa ligera"
    kmh < 25 -> "Viento moderado"
    else -> "Rachas fuertes"
}

private fun humidityDescriptor(percent: Int): String = when {
    percent < 35 -> "Ambiente seco"
    percent <= 65 -> "Confortable"
    else -> "Ambiente húmedo"
}

private fun uvDescriptor(uv: Double): String = when {
    uv < 3 -> "Nivel bajo"
    uv < 6 -> "Nivel moderado"
    uv < 8 -> "Nivel alto"
    else -> "Nivel muy alto"
}

private fun pressureDescriptor(hpa: Double): String = when {
    hpa < 1009 -> "Presión baja"
    hpa <= 1020 -> "Presión normal"
    else -> "Presión alta"
}
