package com.example.ui.state

import com.example.data.local.ClimaSettingsData
import com.example.data.repository.ConnectionTestResult
import com.example.model.CitySearchResult
import com.example.model.LocalRecommendation
import com.example.model.WeatherChipType
import com.example.model.WeatherSnapshot
import com.example.model.WeatherVisualState

data class ClimaUiState(
    val activeSnapshot: WeatherSnapshot? = null,
    val recommendations: List<LocalRecommendation> = emptyList(),
    val aiCardText: String = "",
    val aiAnimationKey: Int = 0,
    val isAiThinking: Boolean = false,
    val bottomBarNotice: String? = null,
    val selectedChip: WeatherChipType? = null,
    val isLoadingWeather: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOfflineFallback: Boolean = false,
    val needsLocationSelection: Boolean = false,
    val weatherErrorMessage: String? = null,
    val isCitySearchOpen: Boolean = false,
    val citySearchQuery: String = "",
    val citySearchResults: List<CitySearchResult> = emptyList(),
    val isSearchingCities: Boolean = false,
    val settings: ClimaSettingsData = ClimaSettingsData(),
    val connectionTestResult: ConnectionTestResult = ConnectionTestResult.Idle,
    val debugSimulatedState: WeatherVisualState? = null,
) {
    val isDebugSimulationActive: Boolean
        get() = debugSimulatedState != null
}
