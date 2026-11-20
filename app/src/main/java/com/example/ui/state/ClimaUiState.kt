package com.example.ui.state

import com.example.model.ChatMessage
import com.example.model.CitySearchResult
import com.example.model.WeatherSnapshot
import com.example.model.WeatherVisualState

data class ClimaUiState(
    val activeSnapshot: WeatherSnapshot? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val needsLocationPermission: Boolean = false,
    val isOfflineFallback: Boolean = false,
    val weatherError: String? = null,
    val aiSummary: String = "",
    val aiSummaryKey: Int = 0,
    val isAiSummaryLoading: Boolean = false,
    val aiSummaryError: String? = null,
    val aiMissingKeyMessage: String? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val isChatSending: Boolean = false,
    val userReduceAnimations: Boolean = false,
    val isCitySearchOpen: Boolean = false,
    val citySearchQuery: String = "",
    val citySearchResults: List<CitySearchResult> = emptyList(),
    val isSearchingCities: Boolean = false,
    val debugSimulatedState: WeatherVisualState? = null,
) {
    val isDebugSimulationActive: Boolean
        get() = debugSimulatedState != null
}
