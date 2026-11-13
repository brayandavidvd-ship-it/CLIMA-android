package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.ClimaPreferencesDataStore
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.LocationHelper
import com.example.data.remote.NetworkModule
import com.example.data.repository.AiResponseResult
import com.example.data.repository.ConnectionTestResult
import com.example.data.repository.GeminiWeatherRepository
import com.example.data.repository.OpenMeteoWeatherDataSource
import com.example.data.repository.OpenWeatherDemoDataSource
import com.example.data.repository.WeatherFetchResult
import com.example.data.repository.WeatherRepository
import com.example.domain.LocalRecommendationsEngine
import com.example.model.CitySearchResult
import com.example.model.GeminiModelOption
import com.example.model.MockWeatherFactory
import com.example.model.WeatherChipType
import com.example.model.WeatherProviderOption
import com.example.model.WeatherSnapshot
import com.example.model.WeatherVisualState
import com.example.ui.state.ClimaUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClimaViewModel(application: Application) : AndroidViewModel(application) {

    private val cryptoManager = KeystoreCryptoManager()
    private val prefs = ClimaPreferencesDataStore(
        context = application,
        moshi = NetworkModule.moshi,
        cryptoManager = cryptoManager,
    )
    private val locationHelper = LocationHelper(application)
    private val openMeteoDataSource = OpenMeteoWeatherDataSource(
        forecastApi = NetworkModule.forecastApi,
        geocodingApi = NetworkModule.geocodingApi,
    )
    private val openWeatherDemoDataSource = OpenWeatherDemoDataSource()
    private val weatherRepository = WeatherRepository(
        dataSources = mapOf(
            WeatherProviderOption.OPEN_METEO to openMeteoDataSource,
            WeatherProviderOption.OPEN_WEATHER_DEMO to openWeatherDemoDataSource,
        ),
        prefs = prefs,
        locationHelper = locationHelper,
    )
    private val geminiRepository = GeminiWeatherRepository(
        geminiApi = NetworkModule.geminiApi,
        prefs = prefs,
    )

    private val _uiState = MutableStateFlow(ClimaUiState())
    val uiState: StateFlow<ClimaUiState> = _uiState.asStateFlow()

    private var realSnapshot: WeatherSnapshot? = null
    private var lastRealVisualState: WeatherVisualState? = null
    private var citySearchJob: Job? = null
    private var aiJob: Job? = null

    init {
        viewModelScope.launch {
            prefs.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            val cached = weatherRepository.getCachedWeather()
            if (cached != null) {
                realSnapshot = cached
                lastRealVisualState = cached.visualState
                applyRealSnapshot(cached, isOfflineFallback = false)
            }
            refreshWeather(forceRefresh = false)
        }
    }

    fun hasLocationPermission(): Boolean = locationHelper.hasCoarseLocationPermission()

    fun onLocationPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            if (granted) {
                val res = weatherRepository.switchToAutoLocation()
                handleWeatherFetchResult(res)
            } else {
                val saved = weatherRepository.getSavedLocation()
                if (saved == null && realSnapshot == null && !_uiState.value.isDebugSimulationActive) {
                    _uiState.update {
                        it.copy(
                            isLoadingWeather = false,
                            needsLocationSelection = true,
                            isCitySearchOpen = true,
                        )
                    }
                }
            }
        }
    }

    fun refreshWeather(forceRefresh: Boolean) {
        if (_uiState.value.isDebugSimulationActive) {
            _uiState.update { it.copy(isRefreshing = false, isLoadingWeather = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = forceRefresh,
                    isLoadingWeather = realSnapshot == null,
                    weatherErrorMessage = null,
                )
            }
            val result = weatherRepository.loadWeather(forceRefresh = forceRefresh)
            handleWeatherFetchResult(result)
        }
    }

    private suspend fun handleWeatherFetchResult(result: WeatherFetchResult) {
        when (result) {
            is WeatherFetchResult.Success -> {
                realSnapshot = result.snapshot
                val stateChanged = lastRealVisualState != null &&
                    lastRealVisualState != result.snapshot.visualState
                lastRealVisualState = result.snapshot.visualState

                if (!_uiState.value.isDebugSimulationActive) {
                    applyRealSnapshot(
                        snapshot = result.snapshot,
                        isOfflineFallback = result.isOfflineFallback,
                        clearChip = stateChanged,
                    )
                    loadAutoSummaryFor(result.snapshot)
                }
            }

            is WeatherFetchResult.NeedsLocation -> {
                if (!_uiState.value.isDebugSimulationActive) {
                    _uiState.update {
                        it.copy(
                            isLoadingWeather = false,
                            isRefreshing = false,
                            needsLocationSelection = true,
                        )
                    }
                }
            }

            is WeatherFetchResult.Error -> {
                if (!_uiState.value.isDebugSimulationActive) {
                    _uiState.update {
                        it.copy(
                            isLoadingWeather = false,
                            isRefreshing = false,
                            weatherErrorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    private fun applyRealSnapshot(
        snapshot: WeatherSnapshot,
        isOfflineFallback: Boolean,
        clearChip: Boolean = false,
    ) {
        val recs = LocalRecommendationsEngine.evaluate(snapshot)
        _uiState.update { current ->
            current.copy(
                activeSnapshot = snapshot,
                recommendations = recs,
                selectedChip = if (clearChip) null else current.selectedChip,
                isLoadingWeather = false,
                isRefreshing = false,
                isOfflineFallback = isOfflineFallback,
                needsLocationSelection = false,
                weatherErrorMessage = null,
                aiCardText = current.aiCardText.ifBlank {
                    LocalRecommendationsEngine.buildFallbackSummary(snapshot)
                },
            )
        }
    }

    private fun loadAutoSummaryFor(snapshot: WeatherSnapshot) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            val settings = _uiState.value.settings
            if (settings.aiEnabled && settings.apiKey.isNotBlank()) {
                _uiState.update { it.copy(isAiThinking = true, bottomBarNotice = null) }
            }
            when (
                val res = geminiRepository.getOrGenerateAutoSummary(
                    snapshot = snapshot,
                    aiEnabled = settings.aiEnabled,
                    apiKey = settings.apiKey,
                    model = settings.selectedModel,
                )
            ) {
                is AiResponseResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAiThinking = false,
                            aiCardText = res.text,
                            aiAnimationKey = it.aiAnimationKey + 1,
                            bottomBarNotice = null,
                        )
                    }
                }
                is AiResponseResult.Fallback -> {
                    _uiState.update {
                        it.copy(
                            isAiThinking = false,
                            aiCardText = res.text,
                            aiAnimationKey = it.aiAnimationKey + 1,
                            bottomBarNotice = res.noticeMessage,
                        )
                    }
                }
            }
        }
    }

    fun onChipSelected(chip: WeatherChipType) {
        val snapshot = _uiState.value.activeSnapshot ?: return
        if (_uiState.value.selectedChip == chip) {
            _uiState.update { it.copy(selectedChip = null) }
            if (_uiState.value.isDebugSimulationActive) {
                _uiState.update {
                    it.copy(
                        aiCardText = MockWeatherFactory.simulatedAiSummary(snapshot),
                        aiAnimationKey = it.aiAnimationKey + 1,
                    )
                }
            } else {
                loadAutoSummaryFor(snapshot)
            }
            return
        }

        _uiState.update { it.copy(selectedChip = chip, bottomBarNotice = null) }

        if (_uiState.value.isDebugSimulationActive) {
            val simulatedAnswer = LocalRecommendationsEngine.buildFallbackChipAnswer(chip, snapshot)
            _uiState.update {
                it.copy(
                    isAiThinking = false,
                    aiCardText = simulatedAnswer,
                    aiAnimationKey = it.aiAnimationKey + 1,
                )
            }
            return
        }

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            val settings = _uiState.value.settings
            if (settings.aiEnabled && settings.apiKey.isNotBlank()) {
                _uiState.update { it.copy(isAiThinking = true) }
            }
            when (
                val res = geminiRepository.askChipQuestion(
                    chip = chip,
                    snapshot = snapshot,
                    aiEnabled = settings.aiEnabled,
                    apiKey = settings.apiKey,
                    model = settings.selectedModel,
                )
            ) {
                is AiResponseResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAiThinking = false,
                            aiCardText = res.text,
                            aiAnimationKey = it.aiAnimationKey + 1,
                            bottomBarNotice = null,
                        )
                    }
                }
                is AiResponseResult.Fallback -> {
                    _uiState.update {
                        it.copy(
                            isAiThinking = false,
                            aiCardText = res.text,
                            aiAnimationKey = it.aiAnimationKey + 1,
                            bottomBarNotice = res.noticeMessage,
                        )
                    }
                }
            }
        }
    }

    fun onAskCustomQuestion(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        val snapshot = _uiState.value.activeSnapshot ?: return

        if (_uiState.value.isDebugSimulationActive) {
            val simulatedAnswer =
                "En modo de prueba (${snapshot.conditionLabel}, ${snapshot.temperatureC.toInt()}°): " +
                    LocalRecommendationsEngine.buildFallbackSummary(snapshot)
            _uiState.update {
                it.copy(
                    selectedChip = null,
                    isAiThinking = false,
                    aiCardText = simulatedAnswer,
                    aiAnimationKey = it.aiAnimationKey + 1,
                )
            }
            return
        }

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            val settings = _uiState.value.settings
            if (!settings.aiEnabled) {
                _uiState.update { it.copy(bottomBarNotice = "Activa la IA en Ajustes") }
                return@launch
            }
            if (settings.apiKey.isBlank()) {
                _uiState.update { it.copy(bottomBarNotice = "Agrega tu clave de Gemini en Ajustes") }
                return@launch
            }

            _uiState.update { it.copy(selectedChip = null, isAiThinking = true, bottomBarNotice = null) }
            when (
                val res = geminiRepository.askCustomQuestion(
                    question = trimmed,
                    snapshot = snapshot,
                    aiEnabled = settings.aiEnabled,
                    apiKey = settings.apiKey,
                    model = settings.selectedModel,
                )
            ) {
                is AiResponseResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAiThinking = false,
                            aiCardText = res.text,
                            aiAnimationKey = it.aiAnimationKey + 1,
                            bottomBarNotice = null,
                        )
                    }
                }
                is AiResponseResult.Fallback -> {
                    _uiState.update {
                        it.copy(
                            isAiThinking = false,
                            aiCardText = res.text,
                            aiAnimationKey = it.aiAnimationKey + 1,
                            bottomBarNotice = res.noticeMessage,
                        )
                    }
                }
            }
        }
    }

    fun clearBottomBarNotice() {
        _uiState.update { it.copy(bottomBarNotice = null) }
    }

    // --- City Search ---

    fun openCitySearch() {
        _uiState.update { it.copy(isCitySearchOpen = true) }
    }

    fun closeCitySearch() {
        _uiState.update {
            it.copy(
                isCitySearchOpen = false,
                citySearchQuery = "",
                citySearchResults = emptyList(),
                isSearchingCities = false,
            )
        }
    }

    fun onCitySearchQueryChanged(query: String) {
        _uiState.update { it.copy(citySearchQuery = query) }
        citySearchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(citySearchResults = emptyList(), isSearchingCities = false) }
            return
        }
        citySearchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchingCities = true) }
            delay(320)
            val result = weatherRepository.searchCities(query)
            _uiState.update {
                it.copy(
                    citySearchResults = result.getOrDefault(emptyList()),
                    isSearchingCities = false,
                )
            }
        }
    }

    fun onCitySelected(city: CitySearchResult) {
        closeCitySearch()
        if (_uiState.value.isDebugSimulationActive) {
            exitDebugSimulation()
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWeather = true, weatherErrorMessage = null) }
            val result = weatherRepository.selectManualCity(city)
            handleWeatherFetchResult(result)
        }
    }

    fun onUseAutoLocationClicked() {
        closeCitySearch()
        if (_uiState.value.isDebugSimulationActive) {
            exitDebugSimulation()
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWeather = true, weatherErrorMessage = null) }
            val result = weatherRepository.switchToAutoLocation()
            handleWeatherFetchResult(result)
        }
    }

    // --- Settings ---

    fun setAiEnabled(enabled: Boolean, consentAccepted: Boolean) {
        viewModelScope.launch {
            prefs.setAiEnabled(enabled, consentAccepted)
        }
    }

    fun saveApiKey(rawKey: String) {
        viewModelScope.launch {
            prefs.saveApiKey(rawKey)
            _uiState.update { it.copy(connectionTestResult = ConnectionTestResult.Idle) }
        }
    }

    fun selectGeminiModel(model: GeminiModelOption) {
        viewModelScope.launch {
            prefs.setSelectedModel(model)
            _uiState.update { it.copy(connectionTestResult = ConnectionTestResult.Idle) }
        }
    }

    fun selectWeatherProvider(provider: WeatherProviderOption) {
        viewModelScope.launch {
            val changed = _uiState.value.settings.selectedWeatherProvider != provider
            prefs.setSelectedWeatherProvider(provider)
            if (changed) {
                if (_uiState.value.isDebugSimulationActive) {
                    _uiState.update { it.copy(debugSimulatedState = null) }
                }
                _uiState.update {
                    it.copy(
                        isRefreshing = true,
                        weatherErrorMessage = null,
                    )
                }
                val result = weatherRepository.loadWeather(
                    forceRefresh = true,
                    providerOverride = provider,
                )
                handleWeatherFetchResult(result)
            }
        }
    }

    fun testGeminiConnection(currentKeyInput: String) {
        viewModelScope.launch {
            val trimmed = currentKeyInput.trim()
            if (trimmed.isNotEmpty()) {
                prefs.saveApiKey(trimmed)
            }
            _uiState.update { it.copy(connectionTestResult = ConnectionTestResult.Testing) }
            val res = geminiRepository.testConnection(
                apiKey = trimmed.ifEmpty { _uiState.value.settings.apiKey },
                model = _uiState.value.settings.selectedModel,
            )
            _uiState.update { it.copy(connectionTestResult = res) }
        }
    }

    fun resetConnectionTestState() {
        _uiState.update { it.copy(connectionTestResult = ConnectionTestResult.Idle) }
    }

    fun setReduceAnimations(reduce: Boolean) {
        viewModelScope.launch {
            prefs.setReduceAnimations(reduce)
        }
    }

    // --- Debug Test Mode (Isolated, strictly guarded by BuildConfig.DEBUG) ---

    fun selectDebugWeatherState(state: WeatherVisualState?) {
        if (!BuildConfig.DEBUG) return
        if (state == null) {
            exitDebugSimulation()
            return
        }
        aiJob?.cancel()
        val cityName = realSnapshot?.cityName ?: "Ciudad de México"
        val mockSnapshot = MockWeatherFactory.createForState(
            state = state,
            cityName = cityName,
        )
        val mockRecommendations = LocalRecommendationsEngine.evaluate(mockSnapshot)
        val mockSummary = MockWeatherFactory.simulatedAiSummary(mockSnapshot)

        _uiState.update { current ->
            current.copy(
                debugSimulatedState = state,
                activeSnapshot = mockSnapshot,
                recommendations = mockRecommendations,
                selectedChip = null,
                isAiThinking = false,
                aiCardText = mockSummary,
                aiAnimationKey = current.aiAnimationKey + 1,
                bottomBarNotice = null,
                isLoadingWeather = false,
                isRefreshing = false,
                isOfflineFallback = false,
                needsLocationSelection = false,
                weatherErrorMessage = null,
            )
        }
    }

    fun exitDebugSimulation() {
        if (!BuildConfig.DEBUG) return
        val restored = realSnapshot
        if (restored != null) {
            _uiState.update { current ->
                current.copy(
                    debugSimulatedState = null,
                    activeSnapshot = restored,
                    recommendations = LocalRecommendationsEngine.evaluate(restored),
                    selectedChip = null,
                    isAiThinking = false,
                    aiCardText = LocalRecommendationsEngine.buildFallbackSummary(restored),
                    aiAnimationKey = current.aiAnimationKey + 1,
                    bottomBarNotice = null,
                )
            }
            loadAutoSummaryFor(restored)
        } else {
            _uiState.update {
                it.copy(
                    debugSimulatedState = null,
                    activeSnapshot = null,
                    selectedChip = null,
                    bottomBarNotice = null,
                )
            }
            refreshWeather(forceRefresh = false)
        }
    }
}
