package com.example

import com.example.data.local.ClimaPreferencesDataStore
import com.example.data.local.KeystoreCryptoManager
import com.example.data.remote.NetworkModule
import com.example.data.repository.GeminiWeatherRepository
import com.example.data.repository.OpenWeatherDemoDataSource
import com.example.domain.LocalRecommendationsEngine
import com.example.model.MockWeatherFactory
import com.example.model.WeatherChipType
import com.example.model.WeatherProviderOption
import com.example.model.WeatherVisualState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import android.content.Context

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ClimaDomainTest {

    @Test
    fun wmoCodeMapping_coversAllNineStatesAndUnknownFallback() {
        assertEquals(WeatherVisualState.SUNNY, WeatherVisualState.fromWmoCode(0, isDay = true))
        assertEquals(WeatherVisualState.CLEAR_NIGHT, WeatherVisualState.fromWmoCode(0, isDay = false))
        assertEquals(WeatherVisualState.PARTLY_CLOUDY, WeatherVisualState.fromWmoCode(1, isDay = true))
        assertEquals(WeatherVisualState.PARTLY_CLOUDY, WeatherVisualState.fromWmoCode(2, isDay = false))
        assertEquals(WeatherVisualState.CLOUDY, WeatherVisualState.fromWmoCode(3, isDay = true))
        assertEquals(WeatherVisualState.FOG, WeatherVisualState.fromWmoCode(45, isDay = true))
        assertEquals(WeatherVisualState.FOG, WeatherVisualState.fromWmoCode(48, isDay = false))
        assertEquals(WeatherVisualState.DRIZZLE, WeatherVisualState.fromWmoCode(53, isDay = true))
        assertEquals(WeatherVisualState.RAIN, WeatherVisualState.fromWmoCode(63, isDay = true))
        assertEquals(WeatherVisualState.RAIN, WeatherVisualState.fromWmoCode(81, isDay = true))
        assertEquals(WeatherVisualState.SNOW, WeatherVisualState.fromWmoCode(73, isDay = true))
        assertEquals(WeatherVisualState.STORM, WeatherVisualState.fromWmoCode(95, isDay = true))
        assertEquals(WeatherVisualState.CLOUDY, WeatherVisualState.fromWmoCode(999, isDay = true))
    }

    @Test
    fun recommendationsEngine_respectsPriorityMaxTwoAndSubtitleLimit() {
        WeatherVisualState.entries.forEach { state ->
            val snapshot = MockWeatherFactory.createForState(state)
            val recs = LocalRecommendationsEngine.evaluate(snapshot)
            assertTrue("Expected <= 2 recommendations for $state", recs.size <= 2)
            recs.forEach { rec ->
                assertTrue(
                    "Subtitle '${rec.subtitle}' exceeds 35 chars (${rec.subtitle.length})",
                    rec.subtitle.length <= 35,
                )
            }
        }

        val sunnyHot = MockWeatherFactory.createForState(WeatherVisualState.SUNNY)
        val sunnyRecs = LocalRecommendationsEngine.evaluate(sunnyHot)
        assertEquals(listOf("Protector solar", "Hidrátate"), sunnyRecs.map { it.title })

        val storm = MockWeatherFactory.createForState(WeatherVisualState.STORM)
        val stormRecs = LocalRecommendationsEngine.evaluate(storm)
        assertEquals(listOf("Lleva paraguas", "Mejor quédate bajo techo"), stormRecs.map { it.title })

        val clearNight = MockWeatherFactory.createForState(WeatherVisualState.CLEAR_NIGHT)
        val nightRecs = LocalRecommendationsEngine.evaluate(clearNight)
        assertEquals(listOf("Se ven las estrellas"), nightRecs.map { it.title })
    }

    @Test
    fun mockWeatherFactory_generatesAllNineStatesAndChipFallbacks() {
        assertEquals(9, WeatherVisualState.entries.size)
        WeatherVisualState.entries.forEach { state ->
            val snapshot = MockWeatherFactory.createForState(state)
            assertEquals(state, snapshot.visualState)
            assertTrue(snapshot.pressureHpa > 900.0)
            assertTrue(snapshot.hourlyForecast.isNotEmpty())
            assertTrue(snapshot.dailyForecast.isNotEmpty())
            val summary = MockWeatherFactory.simulatedAiSummary(snapshot)
            assertTrue(summary.isNotBlank())
            WeatherChipType.entries.forEach { chip ->
                val chipAnswer = LocalRecommendationsEngine.buildFallbackChipAnswer(chip, snapshot)
                assertTrue(chipAnswer.isNotBlank())
            }
        }
    }

    @Test
    fun weatherProviders_andDemoDataSource_provideExtendedForecasts() = runTest {
        assertEquals(2, WeatherProviderOption.entries.size)
        val demoSource = OpenWeatherDemoDataSource()
        val snapshot = demoSource.fetchCurrentWeather(19.43, -99.13, "Ciudad de México")
        assertEquals(WeatherProviderOption.OPEN_WEATHER_DEMO.id, snapshot.providerId)
        assertTrue(snapshot.hourlyForecast.size >= 12)
        assertTrue(snapshot.dailyForecast.size >= 3)
        val cities = demoSource.searchCities("Mont")
        assertTrue(cities.any { it.name.contains("Monterrey") })
    }

    @Test
    fun geminiPrompt_usesSymbolsAndSynchronizedDeviceTime() {
        val snapshot = MockWeatherFactory.createForState(WeatherVisualState.RAIN).let {
            it.copy(hourlyForecast = it.hourlyForecast.reversed(), dailyForecast = it.dailyForecast.reversed())
        }
        val repo = GeminiWeatherRepository(
            geminiApi = NetworkModule.geminiApi,
            prefs = ClimaPreferencesDataStore(
                context = ApplicationProvider.getApplicationContext<Context>(),
                moshi = NetworkModule.moshi,
                cryptoManager = KeystoreCryptoManager(),
            ),
        )
        val prompt = repo.buildWeatherContextPrompt(snapshot)
        assertTrue(prompt.contains("Hora local exacta del dispositivo:"))
        assertTrue(prompt.contains("Pronóstico por horas (ordenado cronológicamente):"))
        assertTrue(prompt.contains("Pronóstico diario (ordenado por fecha):"))
        assertTrue(prompt.contains("%"))
        assertTrue(prompt.contains("°"))
        assertFalse(prompt.lowercase().contains("por ciento"))

        val idx15 = prompt.indexOf("15:00")
        val idx17 = prompt.indexOf("17:00")
        val idx20 = prompt.indexOf("20:00")
        assertTrue(idx15 in 0 until idx17 && idx17 < idx20)

        val cleaned = repo.sanitizePlainText("**Sí**, a las 5 habrá 85 por ciento de lluvia y 16 grados.")
        assertEquals("Sí, a las 5 habrá 85% de lluvia y 16°.", cleaned)
    }
}
