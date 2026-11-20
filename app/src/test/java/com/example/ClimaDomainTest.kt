package com.example

import com.example.data.remote.GeminiApi
import com.example.data.remote.GeminiCandidate
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerateContentRequest
import com.example.data.remote.GeminiGenerateContentResponse
import com.example.data.remote.GeminiPart
import com.example.data.remote.NetworkModule
import com.example.data.repository.AiOutcome
import com.example.data.repository.GeminiAiRepository
import com.example.model.MockWeatherFactory
import com.example.model.WeatherVisualState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClimaDomainTest {

    private class FakeGeminiApi(
        private val jsonPayload: String,
    ) : GeminiApi {
        var lastRequest: GeminiGenerateContentRequest? = null
        override suspend fun generateContent(
            apiKey: String,
            request: GeminiGenerateContentRequest,
        ): GeminiGenerateContentResponse {
            lastRequest = request
            return GeminiGenerateContentResponse(
                candidates = listOf(
                    GeminiCandidate(
                        content = GeminiContent(
                            role = "model",
                            parts = listOf(GeminiPart(text = jsonPayload)),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun wmoCodeMapping_coversAllSevenVisualStates() {
        assertEquals(WeatherVisualState.SUNNY, WeatherVisualState.fromWmoCode(0, isDay = true))
        assertEquals(WeatherVisualState.NIGHT, WeatherVisualState.fromWmoCode(0, isDay = false))
        assertEquals(WeatherVisualState.CLOUDY, WeatherVisualState.fromWmoCode(3, isDay = true))
        assertEquals(WeatherVisualState.FOG, WeatherVisualState.fromWmoCode(45, isDay = true))
        assertEquals(WeatherVisualState.RAIN, WeatherVisualState.fromWmoCode(63, isDay = true))
        assertEquals(WeatherVisualState.SNOW, WeatherVisualState.fromWmoCode(73, isDay = true))
        assertEquals(WeatherVisualState.STORM, WeatherVisualState.fromWmoCode(95, isDay = true))
        assertEquals(WeatherVisualState.CLOUDY, WeatherVisualState.fromWmoCode(999, isDay = true))
    }

    @Test
    fun weatherSnapshot_isLocalNight_usesSunriseAndSunsetClock() {
        val daySnapshot = MockWeatherFactory.createForState(WeatherVisualState.SUNNY)
        assertFalse(daySnapshot.isLocalNight)

        val nightSnapshot = MockWeatherFactory.createForState(WeatherVisualState.NIGHT)
        assertTrue(nightSnapshot.isLocalNight)
        assertEquals(WeatherVisualState.NIGHT, nightSnapshot.visualState)

        val afterSunset = daySnapshot.copy(localTimeIso = "2026-11-19T18:30")
        assertTrue(afterSunset.isLocalNight)
        assertEquals(WeatherVisualState.NIGHT, afterSunset.visualState)
    }

    @Test
    fun mockWeatherFactory_generatesSevenStatesWith12HoursAnd10Days() {
        assertEquals(7, WeatherVisualState.entries.size)
        WeatherVisualState.entries.forEach { state ->
            val snapshot = MockWeatherFactory.createForState(state)
            assertEquals(state, snapshot.visualState)
            assertEquals(12, snapshot.hourlyForecast.size)
            assertEquals(10, snapshot.dailyForecast.size)
            assertTrue(snapshot.pressureHpa > 900.0)
        }
    }

    @Test
    fun geminiAiRepository_returnsMissingApiKey_whenPlaceholderOrBlank() = runTest {
        val storm = MockWeatherFactory.createForState(WeatherVisualState.STORM)
        val fakeApi = FakeGeminiApi("""{"answer":"Llueve fuerte.","includeRainBars":true}""")

        val placeholderRepo = GeminiAiRepository(
            geminiApi = fakeApi,
            moshi = NetworkModule.moshi,
            apiKeyProvider = { GeminiAiRepository.PLACEHOLDER_KEY },
        )
        assertTrue(placeholderRepo.generateSummary(storm) is AiOutcome.MissingApiKey)

        val blankRepo = GeminiAiRepository(
            geminiApi = fakeApi,
            moshi = NetworkModule.moshi,
            apiKeyProvider = { "   " },
        )
        assertTrue(blankRepo.askQuestion("¿Llevo paraguas?", storm) is AiOutcome.MissingApiKey)
    }

    @Test
    fun geminiAiRepository_parsesStructuredJsonAndRespectsRainBarsFlag() = runTest {
        val storm = MockWeatherFactory.createForState(WeatherVisualState.STORM)
        val rainApi = FakeGeminiApi("""{"answer":"**Sí**, llévalo por tormentas.","includeRainBars":true}""")
        val repoWithRain = GeminiAiRepository(
            geminiApi = rainApi,
            moshi = NetworkModule.moshi,
            apiKeyProvider = { "test-valid-key" },
        )

        val umbrellaOutcome = repoWithRain.askQuestion("¿Llevo paraguas?", storm) as AiOutcome.Success
        assertEquals("Sí, llévalo por tormentas.", umbrellaOutcome.reply.text)
        assertEquals(4, umbrellaOutcome.reply.rainBars.size)
        assertTrue(
            rainApi.lastRequest?.contents?.firstOrNull()?.parts?.firstOrNull()?.text
                ?.contains(storm.cityName) == true,
        )

        val summaryOutcome = repoWithRain.generateSummary(storm) as AiOutcome.Success
        assertTrue(summaryOutcome.reply.rainBars.isEmpty())
    }

    @Test
    fun geminiAiRepository_buildWeatherContextAndSanitizePlainText() {
        val storm = MockWeatherFactory.createForState(WeatherVisualState.STORM)
        val repo = GeminiAiRepository(
            geminiApi = FakeGeminiApi("{}"),
            moshi = NetworkModule.moshi,
            apiKeyProvider = { "test-key" },
        )

        val context = repo.buildWeatherContext(storm)
        assertTrue(context.contains("Ciudad: ${storm.cityName}"))
        assertTrue(context.contains("Próximas horas:"))
        assertTrue(context.contains("Próximos días:"))
        assertEquals("Clima despejado hoy", repo.sanitizePlainText("  **Clima** __despejado__ `hoy`  "))
    }
}
