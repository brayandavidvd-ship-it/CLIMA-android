package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.MainWeatherScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WeatherDetailsScreen
import com.example.ui.theme.ClimaTheme
import com.example.ui.viewmodel.ClimaViewModel

object ClimaRoutes {
    const val MAIN = "main_weather"
    const val DETAILS = "weather_details"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            ClimaTheme {
                val viewModel: ClimaViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = ClimaRoutes.MAIN,
                ) {
                    composable(ClimaRoutes.MAIN) {
                        MainWeatherScreen(
                            uiState = uiState,
                            hasLocationPermission = viewModel::hasLocationPermission,
                            onLocationPermissionResult = viewModel::onLocationPermissionResult,
                            onRefresh = { viewModel.refreshWeather(forceRefresh = true) },
                            onOpenCitySearch = viewModel::openCitySearch,
                            onCloseCitySearch = viewModel::closeCitySearch,
                            onCityQueryChange = viewModel::onCitySearchQueryChanged,
                            onCitySelected = viewModel::onCitySelected,
                            onUseAutoLocation = viewModel::onUseAutoLocationClicked,
                            onChipClick = viewModel::onChipSelected,
                            onAskQuestion = viewModel::onAskCustomQuestion,
                            onOpenDetails = {
                                navController.navigate(ClimaRoutes.DETAILS)
                            },
                            onOpenSettings = {
                                viewModel.resetConnectionTestState()
                                navController.navigate(ClimaRoutes.SETTINGS)
                            },
                            onExitDebugSimulation = viewModel::exitDebugSimulation,
                        )
                    }

                    composable(ClimaRoutes.DETAILS) {
                        WeatherDetailsScreen(
                            uiState = uiState,
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(ClimaRoutes.SETTINGS) {
                        SettingsScreen(
                            uiState = uiState,
                            hasLocationPermission = viewModel::hasLocationPermission,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenCitySearch = viewModel::openCitySearch,
                            onCloseCitySearch = viewModel::closeCitySearch,
                            onCityQueryChange = viewModel::onCitySearchQueryChanged,
                            onCitySelected = viewModel::onCitySelected,
                            onUseAutoLocation = viewModel::onUseAutoLocationClicked,
                            onSetAiEnabled = viewModel::setAiEnabled,
                            onSaveApiKey = viewModel::saveApiKey,
                            onSelectModel = viewModel::selectGeminiModel,
                            onTestConnection = viewModel::testGeminiConnection,
                            onSelectWeatherProvider = viewModel::selectWeatherProvider,
                            onSetReduceAnimations = viewModel::setReduceAnimations,
                            onDebugSelectState = viewModel::selectDebugWeatherState,
                        )
                    }
                }
            }
        }
    }
}
