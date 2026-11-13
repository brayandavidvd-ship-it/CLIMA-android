package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.model.LocalRecommendation
import com.example.model.WeatherChipType
import com.example.model.WeatherProviderOption
import com.example.model.WeatherSnapshot
import com.example.model.WeatherVisualState
import com.example.ui.atmospheric.AtmosphericBackground
import com.example.ui.components.CitySearchBottomSheet
import com.example.ui.components.GlassSurface
import com.example.ui.components.PulsingSparkleIcon
import com.example.ui.components.RecommendationCircularIcon
import com.example.ui.components.SimulationStatusBanner
import com.example.ui.components.ThreeDotsThinkingIndicator
import com.example.ui.components.TypewriterText
import com.example.ui.state.ClimaUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainWeatherScreen(
    uiState: ClimaUiState,
    hasLocationPermission: () -> Boolean,
    onLocationPermissionResult: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onOpenCitySearch: () -> Unit,
    onCloseCitySearch: () -> Unit,
    onCityQueryChange: (String) -> Unit,
    onCitySelected: (com.example.model.CitySearchResult) -> Unit,
    onUseAutoLocation: () -> Unit,
    onChipClick: (WeatherChipType) -> Unit,
    onAskQuestion: (String) -> Unit,
    onOpenDetails: () -> Unit,
    onOpenSettings: () -> Unit,
    onExitDebugSimulation: () -> Unit,
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onLocationPermissionResult,
    )

    var requestedInitialPermission by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!requestedInitialPermission && !hasLocationPermission() && uiState.activeSnapshot == null) {
            requestedInitialPermission = true
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    val snapshot = uiState.activeSnapshot
    val visualState = snapshot?.visualState ?: WeatherVisualState.CLOUDY
    val isDay = snapshot?.isDay ?: true

    var questionInput by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

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
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        val minContentHeight = maxHeight
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                                .heightIn(min = minContentHeight)
                                .align(Alignment.TopCenter)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp),
                        ) {
                            if (BuildConfig.DEBUG && uiState.isDebugSimulationActive) {
                                Spacer(modifier = Modifier.height(8.dp))
                                SimulationStatusBanner(
                                    simulatedState = uiState.debugSimulatedState,
                                    onExitSimulation = onExitDebugSimulation,
                                )
                            }

                            // 1. Top Header: City selector left, Settings gear right
                            TopHeaderRow(
                                cityName = snapshot?.cityName ?: stringResource(R.string.select_city_button),
                                onCityClick = onOpenCitySearch,
                                onSettingsClick = onOpenSettings,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (snapshot != null) {
                                // 2. Central Weather Block + Quick Summary Capsule
                                CentralWeatherBlock(
                                    snapshot = snapshot,
                                    isOfflineFallback = uiState.isOfflineFallback,
                                    onOpenDetails = onOpenDetails,
                                )

                                // 3. Flexible atmospheric space
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 28.dp),
                                )

                                // 4. "IA del clima" Card
                                WeatherAiCard(
                                    text = uiState.aiCardText,
                                    animationKey = uiState.aiAnimationKey,
                                    isThinking = uiState.isAiThinking,
                                    reduceAnimations = uiState.settings.reduceAnimations,
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // 5. Row of 3 Interactive Chips
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    WeatherChipType.entries.forEach { chip ->
                                        val isSelected = uiState.selectedChip == chip
                                        GlassSurface(
                                            cornerRadius = 22.dp,
                                            highlighted = isSelected,
                                            onClick = { onChipClick(chip) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("weather_chip_${chip.name.lowercase()}"),
                                        ) {
                                            Text(
                                                text = chip.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color.White,
                                                modifier = Modifier.padding(
                                                    horizontal = 14.dp,
                                                    vertical = 10.dp,
                                                ),
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // 6. Exactly 2 Local Recommendation Cards
                                uiState.recommendations.take(2).forEach { rec ->
                                    RecommendationCard(
                                        recommendation = rec,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                            } else {
                                EmptyOrLoadingState(
                                    isLoading = uiState.isLoadingWeather,
                                    needsLocationSelection = uiState.needsLocationSelection,
                                    errorMessage = uiState.weatherErrorMessage,
                                    onSearchCity = onOpenCitySearch,
                                    onRetry = onRefresh,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                )
                            }
                        }
                    }
                }

                // 7. Floating Bottom Capsule
                if (snapshot != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .padding(horizontal = 20.dp)
                            .padding(top = 4.dp, bottom = 14.dp),
                    ) {
                        uiState.bottomBarNotice?.let { notice ->
                            Text(
                                text = notice,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f),
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag("bottom_bar_notice"),
                            )
                        }

                        BottomAskCapsule(
                            value = questionInput,
                            onValueChange = { questionInput = it },
                            onSend = {
                                val textToSend = questionInput.trim()
                                if (textToSend.isNotEmpty()) {
                                    onAskQuestion(textToSend)
                                    questionInput = ""
                                    keyboardController?.hide()
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (uiState.isCitySearchOpen) {
        CitySearchBottomSheet(
            query = uiState.citySearchQuery,
            results = uiState.citySearchResults,
            isSearching = uiState.isSearchingCities,
            onQueryChange = onCityQueryChange,
            onCitySelected = onCitySelected,
            onUseCurrentLocation = {
                if (hasLocationPermission()) {
                    onUseAutoLocation()
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            onDismiss = onCloseCitySearch,
        )
    }
}

@Composable
private fun TopHeaderRow(
    cityName: String,
    onCityClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onCityClick)
                .minimumInteractiveComponentSize()
                .padding(horizontal = 6.dp, vertical = 8.dp)
                .testTag("city_selector_button"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = stringResource(R.string.location_pin_content_description),
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = cityName,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("settings_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.settings_content_description),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun CentralWeatherBlock(
    snapshot: WeatherSnapshot,
    isOfflineFallback: Boolean,
    onOpenDetails: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
    ) {
        Text(
            text = "${snapshot.temperatureC.roundToInt()}°",
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
            modifier = Modifier.testTag("temperature_text"),
        )

        Text(
            text = snapshot.conditionLabel,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
            ),
            color = Color.White.copy(alpha = 0.94f),
            modifier = Modifier.testTag("condition_label_text"),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(
                R.string.max_min_format,
                snapshot.tempMaxC.roundToInt(),
                snapshot.tempMinC.roundToInt(),
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.testTag("max_min_text"),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Summary Capsule (Humidity, Wind, UV -> navigates to WeatherDetailsScreen)
        GlassSurface(
            cornerRadius = 24.dp,
            onClick = onOpenDetails,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("quick_summary_capsule"),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.quick_summary_format,
                        snapshot.humidityPercent,
                        snapshot.windSpeedKmh.roundToInt(),
                        snapshot.uvIndexMax.roundToInt(),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.94f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.quick_summary_cd),
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        val activeProvider = WeatherProviderOption.fromId(snapshot.providerId)
        if (activeProvider != WeatherProviderOption.OPEN_METEO) {
            Spacer(modifier = Modifier.height(8.dp))
            GlassSurface(
                cornerRadius = 12.dp,
                modifier = Modifier.testTag("provider_source_badge"),
            ) {
                Text(
                    text = stringResource(R.string.provider_source_badge, activeProvider.shortLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        if (isOfflineFallback) {
            Spacer(modifier = Modifier.height(8.dp))
            val minutesAgo = ((System.currentTimeMillis() - snapshot.updatedAtMillis) / 60_000L)
                .coerceAtLeast(0L)
                .toInt()
            val label = if (minutesAgo == 0) {
                stringResource(R.string.updated_just_now)
            } else {
                stringResource(R.string.updated_minutes_ago, minutesAgo)
            }
            GlassSurface(cornerRadius = 12.dp) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun WeatherAiCard(
    text: String,
    animationKey: Int,
    isThinking: Boolean,
    reduceAnimations: Boolean,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weather_ai_card"),
        cornerRadius = 15.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingSparkleIcon(
                    reduceAnimations = reduceAnimations,
                    contentDescription = stringResource(R.string.ai_sparkle_content_description),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.weather_ai_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isThinking) {
                ThreeDotsThinkingIndicator(reduceAnimations = reduceAnimations)
            } else {
                TypewriterText(
                    fullText = text,
                    animationKey = animationKey,
                    reduceAnimations = reduceAnimations,
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: LocalRecommendation,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier.testTag("recommendation_card_${recommendation.id}"),
        cornerRadius = 15.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecommendationCircularIcon(iconType = recommendation.iconType)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                )
                Text(
                    text = recommendation.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BottomAskCapsule(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    GlassSurface(
        cornerRadius = 28.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bottom_ask_capsule"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.ask_weather_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ask_weather_input"),
                )
            }

            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("send_question_button"),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.24f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.send_question_content_description),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyOrLoadingState(
    isLoading: Boolean,
    needsLocationSelection: Boolean,
    errorMessage: String?,
    onSearchCity: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.loading_weather),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
        } else if (needsLocationSelection) {
            Text(
                text = stringResource(R.string.select_city_prompt),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onSearchCity,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1B2432),
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("empty_select_city_button"),
            ) {
                Text(stringResource(R.string.select_city_button))
            }
        } else if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1B2432),
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("retry_weather_button"),
            ) {
                Text(stringResource(R.string.retry_button))
            }
        }
    }
}
