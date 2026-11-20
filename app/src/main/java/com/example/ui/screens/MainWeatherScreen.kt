package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.model.CitySearchResult
import com.example.model.DailyForecastItem
import com.example.model.HourlyForecastItem
import com.example.model.WeatherSnapshot
import com.example.model.WeatherVisualState
import com.example.ui.atmospheric.AtmosphericBackground
import com.example.ui.components.JumpingThreeDots
import com.example.ui.components.OpaqueCard
import com.example.ui.components.OpaqueCircleButton
import com.example.ui.components.OpaqueSkeletonCard
import com.example.ui.components.SparkleVectorIcon
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.TypewriterText
import com.example.ui.components.WeatherDetailsGrid
import com.example.ui.components.WeatherIcon
import com.example.ui.state.ClimaUiState
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.LightShapeViolet
import com.example.ui.theme.OpaqueCardColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWeatherScreen(
    uiState: ClimaUiState,
    canNavigateBack: Boolean,
    hasLocationPermission: () -> Boolean,
    onPermissionResult: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onUseAutoLocation: () -> Unit,
    onOpenCitySearch: () -> Unit,
    onCloseCitySearch: () -> Unit,
    onCityQueryChange: (String) -> Unit,
    onCitySelected: (CitySearchResult) -> Unit,
    onToggleReduceAnimations: () -> Unit,
    onRetryAiSummary: () -> Unit,
    onOpenChat: () -> Unit,
    onSelectDebugState: (WeatherVisualState?) -> Unit,
    onExitDebugSimulation: () -> Unit,
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionResult,
    )

    var requestedInitialPermission by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!requestedInitialPermission && !hasLocationPermission() && uiState.activeSnapshot == null) {
            requestedInitialPermission = true
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isDebugDialogOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = scrollState.value > 120) {
        coroutineScope.launch {
            scrollState.animateScrollTo(0)
        }
    }

    val snapshot = uiState.activeSnapshot
    val visualState = snapshot?.visualState ?: WeatherVisualState.STORM

    Box(modifier = Modifier.fillMaxSize()) {
        AtmosphericBackground(
            visualState = visualState,
            userReduceAnimations = uiState.userReduceAnimations,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopWeatherBar(
                cityName = snapshot?.cityName ?: stringResource(R.string.your_location_fallback),
                canNavigateBack = canNavigateBack,
                isMenuExpanded = isMenuExpanded,
                userReduceAnimations = uiState.userReduceAnimations,
                isDebugSimulationActive = uiState.isDebugSimulationActive,
                onBackClick = onNavigateBack,
                onTitleClick = onOpenCitySearch,
                onMenuOpen = { isMenuExpanded = true },
                onMenuDismiss = { isMenuExpanded = false },
                onRefreshClick = {
                    isMenuExpanded = false
                    onRefresh()
                },
                onSearchCityClick = {
                    isMenuExpanded = false
                    onOpenCitySearch()
                },
                onRequestLocationClick = {
                    isMenuExpanded = false
                    if (hasLocationPermission()) {
                        onUseAutoLocation()
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                },
                onToggleReduceAnimations = {
                    isMenuExpanded = false
                    onToggleReduceAnimations()
                },
                onOpenDebugDialog = {
                    isMenuExpanded = false
                    isDebugDialogOpen = true
                },
                onExitDebugClick = {
                    isMenuExpanded = false
                    onExitDebugSimulation()
                },
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(6.dp))

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (uiState.isDebugSimulationActive && uiState.debugSimulatedState != null) {
                        StatusOpaqueBanner(
                            text = stringResource(
                                R.string.debug_simulating_label,
                                uiState.debugSimulatedState.displayNameEs,
                            ),
                            actionLabel = stringResource(R.string.menu_exit_debug),
                            onAction = onExitDebugSimulation,
                            modifier = Modifier.testTag("debug_simulation_banner"),
                        )
                    }

                    if (uiState.isOfflineFallback && snapshot != null) {
                        StatusOpaqueBanner(
                            text = stringResource(
                                R.string.offline_updated_at,
                                formatTimestampClock(snapshot.updatedAtMillis),
                            ),
                            modifier = Modifier.testTag("offline_banner"),
                        )
                    }

                    when {
                        uiState.needsLocationPermission && snapshot == null -> {
                            LocationPermissionCard(
                                onGrantClick = {
                                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                },
                                onManualCityClick = onOpenCitySearch,
                            )
                        }

                        uiState.weatherError != null && snapshot == null -> {
                            WeatherErrorCard(
                                message = uiState.weatherError,
                                onRetryClick = onRefresh,
                                onManualCityClick = onOpenCitySearch,
                            )
                        }

                        uiState.isLoading && snapshot == null -> {
                            Spacer(modifier = Modifier.height(36.dp))
                            OpaqueSkeletonCard(height = 104.dp)
                            OpaqueSkeletonCard(height = 148.dp)
                            OpaqueSkeletonCard(height = 176.dp)
                        }

                        snapshot != null -> {
                            // Pantalla 1: Hero con temperatura flotante (±4dp, ciclo 4s)
                            WeatherHeroSection(
                                snapshot = snapshot,
                                reduceAnimations = uiState.userReduceAnimations,
                            )

                            // Tarjeta 1: IA del clima (entrada escalonada)
                            StaggeredEntrance(
                                index = 0,
                                delayStepMs = 120,
                                reduceAnimations = uiState.userReduceAnimations,
                            ) {
                                AiSummaryCard(
                                    summary = uiState.aiSummary,
                                    animationKey = uiState.aiSummaryKey,
                                    isLoading = uiState.isAiSummaryLoading,
                                    errorMessage = uiState.aiSummaryError,
                                    missingKeyMessage = uiState.aiMissingKeyMessage,
                                    reduceAnimations = uiState.userReduceAnimations,
                                    onRetry = onRetryAiSummary,
                                )
                            }

                            // Tarjeta 2: Por horas (6 columnas visibles)
                            StaggeredEntrance(
                                index = 1,
                                delayStepMs = 120,
                                reduceAnimations = uiState.userReduceAnimations,
                            ) {
                                HourlyForecastCard(hours = snapshot.hourlyForecast)
                            }

                            // Tarjeta 3: 10 días
                            StaggeredEntrance(
                                index = 2,
                                delayStepMs = 120,
                                reduceAnimations = uiState.userReduceAnimations,
                            ) {
                                DailyForecastCard(days = snapshot.dailyForecast)
                            }

                            // Pantalla 2: Detalles (7 widgets animados en rejilla 2 columnas)
                            WeatherDetailsGrid(
                                snapshot = snapshot,
                                reduceAnimations = uiState.userReduceAnimations,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Barra inferior fija "Pregúntale al clima" que abre el chat
            FixedAskWeatherBar(
                onClick = onOpenChat,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (uiState.isCitySearchOpen) {
        CitySearchDialog(
            query = uiState.citySearchQuery,
            results = uiState.citySearchResults,
            isSearching = uiState.isSearchingCities,
            onQueryChange = onCityQueryChange,
            onSelectCity = onCitySelected,
            onDismiss = onCloseCitySearch,
        )
    }

    if (BuildConfig.DEBUG && isDebugDialogOpen) {
        DebugWeatherStateDialog(
            currentSimulated = uiState.debugSimulatedState,
            onSelectState = { state ->
                isDebugDialogOpen = false
                onSelectDebugState(state)
            },
            onDismiss = { isDebugDialogOpen = false },
        )
    }
}

@Composable
private fun TopWeatherBar(
    cityName: String,
    canNavigateBack: Boolean,
    isMenuExpanded: Boolean,
    userReduceAnimations: Boolean,
    isDebugSimulationActive: Boolean,
    onBackClick: () -> Unit,
    onTitleClick: () -> Unit,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onRefreshClick: () -> Unit,
    onSearchCityClick: () -> Unit,
    onRequestLocationClick: () -> Unit,
    onToggleReduceAnimations: () -> Unit,
    onOpenDebugDialog: () -> Unit,
    onExitDebugClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (canNavigateBack) {
            OpaqueCircleButton(
                onClick = onBackClick,
                size = 44.dp,
                modifier = Modifier.testTag("top_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.back_content_description),
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            Spacer(
                modifier = Modifier
                    .size(44.dp)
                    .testTag("top_back_placeholder"),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onTitleClick)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = cityName,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box {
            OpaqueCircleButton(
                onClick = onMenuOpen,
                size = 44.dp,
                modifier = Modifier.testTag("top_menu_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.more_options_content_description),
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = onMenuDismiss,
                containerColor = OpaqueCardColor,
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.menu_refresh_weather),
                            color = TextPrimary,
                        )
                    },
                    onClick = onRefreshClick,
                    modifier = Modifier.testTag("menu_refresh_item"),
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.menu_search_city),
                            color = TextPrimary,
                        )
                    },
                    onClick = onSearchCityClick,
                    modifier = Modifier.testTag("menu_search_city_item"),
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.menu_request_location),
                            color = TextPrimary,
                        )
                    },
                    onClick = onRequestLocationClick,
                    modifier = Modifier.testTag("menu_location_item"),
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.menu_reduce_animations),
                            color = TextPrimary,
                        )
                    },
                    trailingIcon = if (userReduceAnimations) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else {
                        null
                    },
                    onClick = onToggleReduceAnimations,
                    modifier = Modifier.testTag("menu_reduce_animations_item"),
                )
                if (BuildConfig.DEBUG) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.menu_debug_mode),
                                color = AccentBlue,
                            )
                        },
                        onClick = onOpenDebugDialog,
                        modifier = Modifier.testTag("menu_debug_item"),
                    )
                    if (isDebugSimulationActive) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.menu_exit_debug),
                                    color = AccentBlue,
                                )
                            },
                            onClick = onExitDebugClick,
                            modifier = Modifier.testTag("menu_exit_debug_item"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherHeroSection(
    snapshot: WeatherSnapshot,
    reduceAnimations: Boolean,
) {
    val floatOffsetDp = if (!reduceAnimations) {
        rememberInfiniteTransition(label = "hero_temp_float").animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "float_dp",
        ).value
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = snapshot.conditionLabel,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "${snapshot.temperatureC.roundToInt()}°",
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary,
            modifier = Modifier.offset(y = floatOffsetDp.dp),
        )

        Text(
            text = stringResource(
                R.string.feels_like_format,
                snapshot.apparentTemperatureC.roundToInt(),
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = stringResource(
                R.string.max_min_format,
                snapshot.tempMaxC.roundToInt(),
                snapshot.tempMinC.roundToInt(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun AiSummaryCard(
    summary: String,
    animationKey: Int,
    isLoading: Boolean,
    errorMessage: String?,
    missingKeyMessage: String?,
    reduceAnimations: Boolean,
    onRetry: () -> Unit,
) {
    OpaqueCard(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_summary_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SparkleVectorIcon(size = 14.dp, tint = LightShapeViolet)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.weather_ai_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            when {
                isLoading && summary.isBlank() -> {
                    JumpingThreeDots(modifier = Modifier.padding(vertical = 4.dp))
                }

                missingKeyMessage != null -> {
                    Text(
                        text = missingKeyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }

                errorMessage != null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = onRetry,
                            modifier = Modifier.testTag("retry_ai_summary_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.retry_button),
                                color = AccentBlue,
                            )
                        }
                    }
                }

                else -> {
                    TypewriterText(
                        fullText = summary,
                        animationKey = animationKey,
                        reduceAnimations = reduceAnimations,
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlyForecastCard(hours: List<HourlyForecastItem>) {
    OpaqueCard(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hourly_forecast_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.hourly_forecast_title),
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columnWidth = (maxWidth / 6f).coerceAtLeast(48.dp)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    itemsIndexed(hours) { index, item ->
                        Column(
                            modifier = Modifier.width(columnWidth),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = if (index == 0) {
                                    stringResource(R.string.hourly_now_label)
                                } else {
                                    formatHourlyClockLabel(item.timeIso)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                            )
                            WeatherIcon(
                                weatherCode = item.weatherCode,
                                isDay = item.isDay,
                                size = 24.dp,
                            )
                            Text(
                                text = "${item.precipitationProbability}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentBlue,
                            )
                            Text(
                                text = "${item.temperatureC.roundToInt()}°",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = TextPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyForecastCard(days: List<DailyForecastItem>) {
    val shownDays = days.take(10)
    OpaqueCard(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_forecast_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.ten_days_forecast_title),
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columnWidth = (maxWidth / 6f).coerceAtLeast(48.dp)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    itemsIndexed(shownDays) { index, item ->
                        Column(
                            modifier = Modifier.width(columnWidth),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = if (index == 0) {
                                    stringResource(R.string.daily_today_label)
                                } else {
                                    formatSpanishShortDay(item.dateIso)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                            )
                            WeatherIcon(
                                weatherCode = item.weatherCode,
                                isDay = true,
                                size = 24.dp,
                            )
                            Text(
                                text = "${item.precipitationProbabilityMax}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentBlue,
                            )
                            Text(
                                text = "${item.tempMaxC.roundToInt()}°",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = TextPrimary,
                            )
                            Text(
                                text = "${item.tempMinC.roundToInt()}°",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FixedAskWeatherBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpaqueCard(
        shape = RoundedCornerShape(28.dp),
        onClick = onClick,
        modifier = modifier
            .height(54.dp)
            .testTag("ask_weather_bar"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SparkleVectorIcon(size = 16.dp, tint = LightShapeViolet)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.ask_weather_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(AccentBlue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = stringResource(R.string.send_question_content_description),
                    tint = OpaqueCardColor,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusOpaqueBanner(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    OpaqueCard(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentBlue,
                    modifier = Modifier
                        .clickable(onClick = onAction)
                        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun LocationPermissionCard(
    onGrantClick: () -> Unit,
    onManualCityClick: () -> Unit,
) {
    OpaqueCard(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .testTag("location_permission_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = stringResource(R.string.permission_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onGrantClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        contentColor = OpaqueCardColor,
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("grant_location_button"),
                ) {
                    Text(text = stringResource(R.string.permission_grant_button))
                }
                OutlinedButton(
                    onClick = onManualCityClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("manual_city_button"),
                ) {
                    Text(
                        text = stringResource(R.string.search_city_button),
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherErrorCard(
    message: String,
    onRetryClick: () -> Unit,
    onManualCityClick: () -> Unit,
) {
    OpaqueCard(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .testTag("weather_error_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message.ifBlank { stringResource(R.string.weather_error_generic) },
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRetryClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        contentColor = OpaqueCardColor,
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("retry_weather_button"),
                ) {
                    Text(text = stringResource(R.string.retry_button))
                }
                OutlinedButton(
                    onClick = onManualCityClick,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Text(
                        text = stringResource(R.string.search_city_button),
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CitySearchDialog(
    query: String,
    results: List<CitySearchResult>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSelectCity: (CitySearchResult) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OpaqueCardColor,
        title = {
            Text(
                text = stringResource(R.string.search_city_button),
                color = TextPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_city_hint),
                            color = TextSecondary,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("city_search_input"),
                )

                when {
                    isSearching -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = AccentBlue,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    query.trim().length >= 2 && results.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.search_city_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(results, key = { it.id }) { city ->
                                val subtitle = listOfNotNull(city.admin1, city.country)
                                    .filter { it.isNotBlank() }
                                    .joinToString(", ")
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectCity(city) }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = city.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                    )
                                    if (subtitle.isNotBlank()) {
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel_button),
                    color = AccentBlue,
                )
            }
        },
    )
}

@Composable
private fun DebugWeatherStateDialog(
    currentSimulated: WeatherVisualState?,
    onSelectState: (WeatherVisualState?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OpaqueCardColor,
        title = {
            Text(
                text = stringResource(R.string.debug_dialog_title),
                color = TextPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectState(null) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = currentSimulated == null,
                        onClick = { onSelectState(null) },
                        colors = RadioButtonDefaults.colors(selectedColor = AccentBlue),
                    )
                    Text(
                        text = stringResource(R.string.debug_real_weather_option),
                        color = TextPrimary,
                    )
                }

                WeatherVisualState.entries.forEach { state ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectState(state) }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentSimulated == state,
                            onClick = { onSelectState(state) },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentBlue),
                        )
                        Text(
                            text = state.displayNameEs,
                            color = TextPrimary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.back_content_description), color = TextPrimary)
            }
        },
    )
}

private fun formatHourlyClockLabel(timeIso: String): String {
    val hour24 = timeIso.substringAfter("T", "").take(2).toIntOrNull() ?: return "--"
    return when {
        hour24 == 0 -> "12 AM"
        hour24 < 12 -> "$hour24 AM"
        hour24 == 12 -> "12 PM"
        else -> "${hour24 - 12} PM"
    }
}

private fun formatSpanishShortDay(dateIso: String): String {
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = parser.parse(dateIso) ?: return@runCatching dateIso
        val cal = Calendar.getInstance().apply { time = parsed }
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Dom"
            Calendar.MONDAY -> "Lun"
            Calendar.TUESDAY -> "Mar"
            Calendar.WEDNESDAY -> "Mié"
            Calendar.THURSDAY -> "Jue"
            Calendar.FRIDAY -> "Vie"
            Calendar.SATURDAY -> "Sáb"
            else -> dateIso
        }
    }.getOrDefault(dateIso)
}

private fun formatTimestampClock(millis: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale("es", "MX"))
    return formatter.format(Date(millis))
}
