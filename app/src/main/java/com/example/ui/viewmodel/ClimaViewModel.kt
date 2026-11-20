package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.ClimaPreferencesDataStore
import com.example.data.local.LocationHelper
import com.example.data.remote.NetworkModule
import com.example.data.repository.AiOutcome
import com.example.data.repository.AiRepository
import com.example.data.repository.GeminiAiRepository
import com.example.data.repository.OpenMeteoWeatherDataSource
import com.example.data.repository.WeatherFetchResult
import com.example.data.repository.WeatherRepository
import com.example.model.ChatMessage
import com.example.model.ChatSender
import com.example.model.CitySearchResult
import com.example.model.MockWeatherFactory
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
import java.util.UUID

class ClimaViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = ClimaPreferencesDataStore(
        context = application,
        moshi = NetworkModule.moshi,
    )
    private val locationHelper = LocationHelper(application)
    private val weatherDataSource = OpenMeteoWeatherDataSource(
        forecastApi = NetworkModule.forecastApi,
        geocodingApi = NetworkModule.geocodingApi,
    )
    private val weatherRepository = WeatherRepository(
        weatherDataSource = weatherDataSource,
        prefs = prefs,
        locationHelper = locationHelper,
    )
    private val aiRepository: AiRepository = GeminiAiRepository(
        geminiApi = NetworkModule.geminiApi,
        moshi = NetworkModule.moshi,
    )

    private val _uiState = MutableStateFlow(ClimaUiState())
    val uiState: StateFlow<ClimaUiState> = _uiState.asStateFlow()

    private var realSnapshot: WeatherSnapshot? = null
    private var lastUserQuestion: String? = null
    private var summaryJob: Job? = null
    private var chatJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            prefs.reduceAnimationsFlow.collect { reduce ->
                _uiState.update { it.copy(userReduceAnimations = reduce) }
            }
        }
        viewModelScope.launch {
            val cached = weatherRepository.getCachedWeather()
            if (cached != null) {
                realSnapshot = cached
                _uiState.update {
                    it.copy(
                        activeSnapshot = cached,
                        isLoading = false,
                        needsLocationPermission = false,
                    )
                }
                loadAiSummary(cached)
            }
            refreshWeather(forceRefresh = false)
        }
    }

    fun hasLocationPermission(): Boolean = locationHelper.hasCoarseLocationPermission()

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = realSnapshot == null, needsLocationPermission = false) }
                handleWeatherResult(weatherRepository.switchToAutoLocation())
            }
        } else if (realSnapshot == null && !_uiState.value.isDebugSimulationActive) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    needsLocationPermission = true,
                )
            }
        }
    }

    fun refreshWeather(forceRefresh: Boolean = true) {
        if (_uiState.value.isDebugSimulationActive) {
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = forceRefresh && realSnapshot != null,
                    isLoading = realSnapshot == null,
                    weatherError = null,
                )
            }
            handleWeatherResult(weatherRepository.loadWeather())
        }
    }

    fun useAutoLocation() {
        closeCitySearch()
        if (_uiState.value.isDebugSimulationActive) {
            exitDebugSimulation()
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = realSnapshot == null, isRefreshing = realSnapshot != null, weatherError = null) }
            handleWeatherResult(weatherRepository.switchToAutoLocation())
        }
    }

    private fun handleWeatherResult(result: WeatherFetchResult) {
        when (result) {
            is WeatherFetchResult.Success -> {
                realSnapshot = result.snapshot
                if (!_uiState.value.isDebugSimulationActive) {
                    _uiState.update {
                        it.copy(
                            activeSnapshot = result.snapshot,
                            isLoading = false,
                            isRefreshing = false,
                            needsLocationPermission = false,
                            isOfflineFallback = result.isOfflineFallback,
                            weatherError = null,
                        )
                    }
                    loadAiSummary(result.snapshot)
                }
            }

            is WeatherFetchResult.NeedsLocationPermission -> {
                if (!_uiState.value.isDebugSimulationActive) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            needsLocationPermission = realSnapshot == null,
                        )
                    }
                }
            }

            is WeatherFetchResult.Error -> {
                if (!_uiState.value.isDebugSimulationActive) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            weatherError = result.message,
                        )
                    }
                }
            }
        }
    }

    // --- Manual City Search (fallback when permission denied or chosen from menu) ---

    fun openCitySearch() {
        _uiState.update { it.copy(isCitySearchOpen = true) }
    }

    fun closeCitySearch() {
        searchJob?.cancel()
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
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(citySearchResults = emptyList(), isSearchingCities = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchingCities = true) }
            delay(300L)
            val results = weatherRepository.searchCities(query).getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    citySearchResults = results,
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
            _uiState.update {
                it.copy(
                    isLoading = true,
                    needsLocationPermission = false,
                    weatherError = null,
                )
            }
            handleWeatherResult(weatherRepository.selectManualCity(city))
        }
    }

    // --- Reduce Animations ---

    fun toggleReduceAnimations() {
        viewModelScope.launch {
            prefs.setReduceAnimations(!_uiState.value.userReduceAnimations)
        }
    }

    // --- Gemini AI Summary & Chat ---

    private fun loadAiSummary(snapshot: WeatherSnapshot) {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAiSummaryLoading = true,
                    aiSummaryError = null,
                    aiMissingKeyMessage = null,
                )
            }
            when (val outcome = aiRepository.generateSummary(snapshot)) {
                is AiOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            isAiSummaryLoading = false,
                            aiSummary = outcome.reply.text,
                            aiSummaryKey = it.aiSummaryKey + 1,
                            aiSummaryError = null,
                            aiMissingKeyMessage = null,
                        )
                    }
                }

                is AiOutcome.MissingApiKey -> {
                    _uiState.update {
                        it.copy(
                            isAiSummaryLoading = false,
                            aiSummaryError = null,
                            aiMissingKeyMessage = outcome.message,
                        )
                    }
                }

                is AiOutcome.Error -> {
                    _uiState.update {
                        it.copy(
                            isAiSummaryLoading = false,
                            aiSummaryError = outcome.message,
                            aiMissingKeyMessage = null,
                        )
                    }
                }
            }
        }
    }

    fun retryAiSummary() {
        val snapshot = _uiState.value.activeSnapshot ?: return
        loadAiSummary(snapshot)
    }

    fun sendChatMessage(rawQuestion: String) {
        val question = rawQuestion.trim()
        if (question.isEmpty()) return
        val snapshot = _uiState.value.activeSnapshot ?: return

        lastUserQuestion = question
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.User,
            text = question,
        )
        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMessage,
                isChatSending = true,
            )
        }
        executeChatReply(question = question, snapshot = snapshot)
    }

    fun retryLastChatQuestion() {
        val question = lastUserQuestion ?: return
        val snapshot = _uiState.value.activeSnapshot ?: return
        _uiState.update { current ->
            val trimmed = if (current.chatMessages.lastOrNull()?.isError == true) {
                current.chatMessages.dropLast(1)
            } else {
                current.chatMessages
            }
            current.copy(
                chatMessages = trimmed,
                isChatSending = true,
            )
        }
        executeChatReply(question = question, snapshot = snapshot)
    }

    private fun executeChatReply(
        question: String,
        snapshot: WeatherSnapshot,
    ) {
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            when (val outcome = aiRepository.askQuestion(question, snapshot)) {
                is AiOutcome.Success -> {
                    val aiMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = ChatSender.Ai,
                        text = outcome.reply.text,
                        rainBars = outcome.reply.rainBars,
                    )
                    _uiState.update {
                        it.copy(
                            chatMessages = it.chatMessages + aiMsg,
                            isChatSending = false,
                        )
                    }
                }

                is AiOutcome.MissingApiKey -> {
                    val errorMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = ChatSender.Ai,
                        text = outcome.message,
                        isError = true,
                    )
                    _uiState.update {
                        it.copy(
                            chatMessages = it.chatMessages + errorMsg,
                            isChatSending = false,
                        )
                    }
                }

                is AiOutcome.Error -> {
                    val errorMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = ChatSender.Ai,
                        text = outcome.message,
                        isError = true,
                    )
                    _uiState.update {
                        it.copy(
                            chatMessages = it.chatMessages + errorMsg,
                            isChatSending = false,
                        )
                    }
                }
            }
        }
    }

    fun clearChatHistory() {
        chatJob?.cancel()
        lastUserQuestion = null
        _uiState.update {
            it.copy(
                chatMessages = emptyList(),
                isChatSending = false,
            )
        }
    }

    // --- Hidden Debug Test Mode (BuildConfig.DEBUG only) ---

    fun selectDebugWeatherState(state: WeatherVisualState?) {
        if (!BuildConfig.DEBUG) return
        if (state == null) {
            exitDebugSimulation()
            return
        }
        val cityName = realSnapshot?.cityName ?: "Tu ubicación"
        val simulated = MockWeatherFactory.createForState(
            state = state,
            cityName = cityName,
        )
        _uiState.update {
            it.copy(
                debugSimulatedState = state,
                activeSnapshot = simulated,
                isLoading = false,
                isRefreshing = false,
                needsLocationPermission = false,
                isOfflineFallback = false,
                weatherError = null,
            )
        }
        loadAiSummary(simulated)
    }

    fun exitDebugSimulation() {
        if (!BuildConfig.DEBUG) return
        val restored = realSnapshot
        if (restored != null) {
            _uiState.update {
                it.copy(
                    debugSimulatedState = null,
                    activeSnapshot = restored,
                    isLoading = false,
                    isRefreshing = false,
                    needsLocationPermission = false,
                    weatherError = null,
                )
            }
            loadAiSummary(restored)
        } else {
            _uiState.update {
                it.copy(
                    debugSimulatedState = null,
                    activeSnapshot = null,
                )
            }
            refreshWeather(forceRefresh = false)
        }
    }
}
