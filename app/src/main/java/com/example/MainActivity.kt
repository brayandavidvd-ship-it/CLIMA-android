package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.MainWeatherScreen
import com.example.ui.screens.WeatherChatScreen
import com.example.ui.theme.ClimaTheme
import com.example.ui.viewmodel.ClimaViewModel

object ClimaRoutes {
    const val MAIN = "main_weather"
    const val CHAT = "weather_chat"
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
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val canNavigateBack = remember(currentBackStackEntry) {
                    navController.previousBackStackEntry != null
                }

                NavHost(
                    navController = navController,
                    startDestination = ClimaRoutes.MAIN,
                ) {
                    composable(ClimaRoutes.MAIN) {
                        MainWeatherScreen(
                            uiState = uiState,
                            canNavigateBack = canNavigateBack,
                            hasLocationPermission = viewModel::hasLocationPermission,
                            onPermissionResult = viewModel::onPermissionResult,
                            onNavigateBack = { navController.popBackStack() },
                            onRefresh = { viewModel.refreshWeather(forceRefresh = true) },
                            onUseAutoLocation = viewModel::useAutoLocation,
                            onOpenCitySearch = viewModel::openCitySearch,
                            onCloseCitySearch = viewModel::closeCitySearch,
                            onCityQueryChange = viewModel::onCitySearchQueryChanged,
                            onCitySelected = viewModel::onCitySelected,
                            onToggleReduceAnimations = viewModel::toggleReduceAnimations,
                            onRetryAiSummary = viewModel::retryAiSummary,
                            onOpenChat = {
                                navController.navigate(ClimaRoutes.CHAT)
                            },
                            onSelectDebugState = viewModel::selectDebugWeatherState,
                            onExitDebugSimulation = viewModel::exitDebugSimulation,
                        )
                    }

                    composable(ClimaRoutes.CHAT) {
                        WeatherChatScreen(
                            uiState = uiState,
                            canNavigateBack = canNavigateBack,
                            onNavigateBack = { navController.popBackStack() },
                            onSendMessage = viewModel::sendChatMessage,
                            onRetryLastQuestion = viewModel::retryLastChatQuestion,
                            onClearChat = viewModel::clearChatHistory,
                            onRefreshWeather = { viewModel.refreshWeather(forceRefresh = true) },
                        )
                    }
                }
            }
        }
    }
}
