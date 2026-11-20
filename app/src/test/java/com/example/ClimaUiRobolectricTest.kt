package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.example.model.ChatMessage
import com.example.model.ChatSender
import com.example.model.HourlyRainBar
import com.example.model.MockWeatherFactory
import com.example.model.WeatherVisualState
import com.example.ui.components.WeatherIcon
import com.example.ui.screens.MainWeatherScreen
import com.example.ui.screens.WeatherChatScreen
import com.example.ui.state.ClimaUiState
import com.example.ui.theme.ClimaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ClimaUiRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mainScreenRoot_hidesBackButton_whenCannotNavigateBack() {
        val snapshot = MockWeatherFactory.createForState(WeatherVisualState.STORM)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ClimaTheme {
                MainWeatherScreen(
                    uiState = ClimaUiState(activeSnapshot = snapshot, isLoading = false),
                    canNavigateBack = false,
                    hasLocationPermission = { true },
                    onPermissionResult = {},
                    onNavigateBack = {},
                    onRefresh = {},
                    onUseAutoLocation = {},
                    onOpenCitySearch = {},
                    onCloseCitySearch = {},
                    onCityQueryChange = {},
                    onCitySelected = {},
                    onToggleReduceAnimations = {},
                    onRetryAiSummary = {},
                    onOpenChat = {},
                    onSelectDebugState = {},
                    onExitDebugSimulation = {},
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(160)

        composeRule.onNodeWithTag("top_back_button").assertDoesNotExist()
        composeRule.onNodeWithTag("top_back_placeholder").assertExists()
        composeRule.onNodeWithTag("top_menu_button").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("ask_weather_bar").assertIsDisplayed()
    }

    @Test
    fun mainScreen_rendersBackButton_whenCanNavigateBack() {
        val snapshot = MockWeatherFactory.createForState(WeatherVisualState.SUNNY)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ClimaTheme {
                MainWeatherScreen(
                    uiState = ClimaUiState(activeSnapshot = snapshot, isLoading = false),
                    canNavigateBack = true,
                    hasLocationPermission = { true },
                    onPermissionResult = {},
                    onNavigateBack = {},
                    onRefresh = {},
                    onUseAutoLocation = {},
                    onOpenCitySearch = {},
                    onCloseCitySearch = {},
                    onCityQueryChange = {},
                    onCitySelected = {},
                    onToggleReduceAnimations = {},
                    onRetryAiSummary = {},
                    onOpenChat = {},
                    onSelectDebugState = {},
                    onExitDebugSimulation = {},
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(160)

        composeRule.onNodeWithTag("top_back_button").assertIsDisplayed()
    }

    @Test
    fun chatScreen_rendersContextMessagesChipsAndInput() {
        val snapshot = MockWeatherFactory.createForState(WeatherVisualState.STORM)
        val messages = listOf(
            ChatMessage(id = "1", sender = ChatSender.User, text = "¿Llevo paraguas hoy?"),
            ChatMessage(
                id = "2",
                sender = ChatSender.Ai,
                text = "Sí, llévalo. Hay 70% de lluvia a las 3 PM.",
                rainBars = listOf(
                    HourlyRainBar("3 PM", 70),
                    HourlyRainBar("4 PM", 40),
                ),
            ),
        )
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ClimaTheme {
                WeatherChatScreen(
                    uiState = ClimaUiState(
                        activeSnapshot = snapshot,
                        isLoading = false,
                        chatMessages = messages,
                        userReduceAnimations = true,
                    ),
                    canNavigateBack = true,
                    onNavigateBack = {},
                    onSendMessage = {},
                    onRetryLastQuestion = {},
                    onClearChat = {},
                    onRefreshWeather = {},
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(160)

        composeRule.onNodeWithTag("chat_back_button").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_context_card").assertIsDisplayed()
        composeRule.onNodeWithText("¿Llevo paraguas hoy?").assertIsDisplayed()
        composeRule.onNodeWithText("Sí, llévalo. Hay 70% de lluvia a las 3 PM.").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_chip_0").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_send_button").assertIsDisplayed()
    }

    @Test
    fun weatherIcon_rendersFlatVectorsForAllWeatherStates() {
        composeRule.setContent {
            ClimaTheme {
                WeatherVisualState.entries.forEach { state ->
                    val snap = MockWeatherFactory.createForState(state)
                    WeatherIcon(
                        weatherCode = snap.weatherCode,
                        isDay = !snap.isLocalNight,
                        size = 48.dp,
                    )
                }
            }
        }
    }
}
