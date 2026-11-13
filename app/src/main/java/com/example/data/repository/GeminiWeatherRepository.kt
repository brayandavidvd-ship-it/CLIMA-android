package com.example.data.repository

import com.example.data.local.ClimaPreferencesDataStore
import com.example.data.remote.GeminiApi
import com.example.data.remote.GeminiContentDto
import com.example.data.remote.GeminiGenerateContentRequest
import com.example.data.remote.GeminiGenerationConfigDto
import com.example.data.remote.GeminiPartDto
import com.example.domain.LocalRecommendationsEngine
import com.example.model.GeminiModelOption
import com.example.model.WeatherChipType
import com.example.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

sealed interface AiResponseResult {
    data class Success(val text: String, val fromCache: Boolean) : AiResponseResult
    data class Fallback(val text: String, val noticeMessage: String?) : AiResponseResult
}

sealed interface ConnectionTestResult {
    data object Idle : ConnectionTestResult
    data object Testing : ConnectionTestResult
    data class Success(val message: String) : ConnectionTestResult
    data class Failure(val message: String) : ConnectionTestResult
}

class GeminiWeatherRepository(
    private val geminiApi: GeminiApi,
    private val prefs: ClimaPreferencesDataStore,
) {
    companion object {
        private const val AUTO_SUMMARY_INTERVAL_MS = 60L * 60L * 1000L
        private const val TEMP_DELTA_THRESHOLD_C = 3.0

        internal const val SYSTEM_PROMPT =
            "Eres un asistente meteorológico conversacional en español de México: cercano, humano y sumamente directo. " +
                "Recibirás la hora local exacta del dispositivo, el clima actual, el pronóstico ordenado por horas y el pronóstico diario. " +
                "Responde siempre en 1 o 2 frases breves (máximo ~200 caracteres), en texto plano sin Markdown, viñetas ni asteriscos. " +
                "Usa estrictamente los símbolos % y ° para porcentajes y temperaturas (por ejemplo: 24°, 70%); queda terminantemente prohibido escribir palabras como 'por ciento' o 'grados'. " +
                "Prohibido dar respuestas evasivas o repetir estadísticas frías: si preguntan por una hora específica (ej. '¿Va a llover a las 5?') o por mañana o próximos días, analiza el pronóstico por horas o diario y responde de forma lógica y directa (empezando con un sí o no claro respaldado por los datos). " +
                "Traduce los datos técnicos en utilidad diaria para vestir, salir o planear según temperatura, viento y probabilidad de lluvia. " +
                "Usa solo los datos provistos; si falta algún dato secundario ignóralo y nunca inventes cifras ni consejos médicos. " +
                "Si preguntan algo ajeno al clima, redirígelo cordialmente hacia el estado del tiempo."
    }

    suspend fun getOrGenerateAutoSummary(
        snapshot: WeatherSnapshot,
        aiEnabled: Boolean,
        apiKey: String,
        model: GeminiModelOption,
    ): AiResponseResult = withContext(Dispatchers.IO) {
        if (!aiEnabled || apiKey.isBlank()) {
            return@withContext AiResponseResult.Fallback(
                text = LocalRecommendationsEngine.buildFallbackSummary(snapshot),
                noticeMessage = null,
            )
        }

        val lastMeta = prefs.getAutoSummaryMeta()
        val now = System.currentTimeMillis()
        val stateChanged = lastMeta.visualStateName != snapshot.visualState.name
        val tempChangedEnough = lastMeta.tempC == null ||
            abs(lastMeta.tempC - snapshot.temperatureC) >= TEMP_DELTA_THRESHOLD_C
        val intervalElapsed = (now - lastMeta.timestampMillis) > AUTO_SUMMARY_INTERVAL_MS

        if (lastMeta.text.isNotBlank() && !stateChanged && !tempChangedEnough && !intervalElapsed) {
            return@withContext AiResponseResult.Success(text = lastMeta.text, fromCache = true)
        }

        val cacheKey = "${snapshot.cacheSignature()}|AUTO_SUMMARY|${model.modelId}"
        prefs.getCachedAiResponse(cacheKey)?.let { cached ->
            prefs.saveAutoSummaryMeta(now, cached, snapshot.temperatureC, snapshot.visualState.name)
            return@withContext AiResponseResult.Success(text = cached, fromCache = true)
        }

        val userPrompt = buildWeatherContextPrompt(snapshot) +
            "\nGenera un resumen breve y útil del clima actual."

        return@withContext when (val callResult = executeGenerateContent(apiKey, model.modelId, userPrompt)) {
            is ApiCallResult.Ok -> {
                val cleaned = sanitizePlainText(callResult.text)
                prefs.putCachedAiResponse(cacheKey, cleaned)
                prefs.saveAutoSummaryMeta(now, cleaned, snapshot.temperatureC, snapshot.visualState.name)
                AiResponseResult.Success(text = cleaned, fromCache = false)
            }
            is ApiCallResult.Err -> {
                AiResponseResult.Fallback(
                    text = LocalRecommendationsEngine.buildFallbackSummary(snapshot),
                    noticeMessage = callResult.userMessage,
                )
            }
        }
    }

    suspend fun askChipQuestion(
        chip: WeatherChipType,
        snapshot: WeatherSnapshot,
        aiEnabled: Boolean,
        apiKey: String,
        model: GeminiModelOption,
    ): AiResponseResult = withContext(Dispatchers.IO) {
        if (!aiEnabled || apiKey.isBlank()) {
            return@withContext AiResponseResult.Fallback(
                text = LocalRecommendationsEngine.buildFallbackChipAnswer(chip, snapshot),
                noticeMessage = if (!aiEnabled) "Activa la IA en Ajustes" else "Agrega tu clave de Gemini en Ajustes",
            )
        }

        val cacheKey = "${snapshot.cacheSignature()}|CHIP_${chip.name}|${model.modelId}"
        prefs.getCachedAiResponse(cacheKey)?.let { cached ->
            return@withContext AiResponseResult.Success(text = cached, fromCache = true)
        }

        val userPrompt = buildWeatherContextPrompt(snapshot) +
            "\nPregunta del usuario: ${chip.label}"

        return@withContext when (val callResult = executeGenerateContent(apiKey, model.modelId, userPrompt)) {
            is ApiCallResult.Ok -> {
                val cleaned = sanitizePlainText(callResult.text)
                prefs.putCachedAiResponse(cacheKey, cleaned)
                AiResponseResult.Success(text = cleaned, fromCache = false)
            }
            is ApiCallResult.Err -> {
                AiResponseResult.Fallback(
                    text = LocalRecommendationsEngine.buildFallbackChipAnswer(chip, snapshot),
                    noticeMessage = callResult.userMessage,
                )
            }
        }
    }

    suspend fun askCustomQuestion(
        question: String,
        snapshot: WeatherSnapshot,
        aiEnabled: Boolean,
        apiKey: String,
        model: GeminiModelOption,
    ): AiResponseResult = withContext(Dispatchers.IO) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) {
            return@withContext AiResponseResult.Fallback(
                text = LocalRecommendationsEngine.buildFallbackSummary(snapshot),
                noticeMessage = null,
            )
        }
        if (!aiEnabled) {
            return@withContext AiResponseResult.Fallback(
                text = LocalRecommendationsEngine.buildFallbackSummary(snapshot),
                noticeMessage = "Activa la IA en Ajustes",
            )
        }
        if (apiKey.isBlank()) {
            return@withContext AiResponseResult.Fallback(
                text = LocalRecommendationsEngine.buildFallbackSummary(snapshot),
                noticeMessage = "Agrega tu clave de Gemini en Ajustes",
            )
        }

        val cacheKey = "${snapshot.cacheSignature()}|Q_${trimmed.lowercase().hashCode()}|${model.modelId}"
        prefs.getCachedAiResponse(cacheKey)?.let { cached ->
            return@withContext AiResponseResult.Success(text = cached, fromCache = true)
        }

        val userPrompt = buildWeatherContextPrompt(snapshot) +
            "\nPregunta del usuario: $trimmed"

        return@withContext when (val callResult = executeGenerateContent(apiKey, model.modelId, userPrompt)) {
            is ApiCallResult.Ok -> {
                val cleaned = sanitizePlainText(callResult.text)
                prefs.putCachedAiResponse(cacheKey, cleaned)
                AiResponseResult.Success(text = cleaned, fromCache = false)
            }
            is ApiCallResult.Err -> {
                AiResponseResult.Fallback(
                    text = LocalRecommendationsEngine.buildFallbackSummary(snapshot),
                    noticeMessage = callResult.userMessage,
                )
            }
        }
    }

    suspend fun testConnection(
        apiKey: String,
        model: GeminiModelOption,
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            return@withContext ConnectionTestResult.Failure("Ingresa tu clave de API de Gemini primero.")
        }
        when (
            val res = executeGenerateContent(
                apiKey = trimmed,
                modelId = model.modelId,
                userPrompt = "Responde únicamente con: Conexión lista.",
            )
        ) {
            is ApiCallResult.Ok -> ConnectionTestResult.Success("Conexión exitosa con ${model.modelId}.")
            is ApiCallResult.Err -> ConnectionTestResult.Failure(res.userMessage)
        }
    }

    /**
     * Shares synchronized device local time, city name, current telemetry,
     * and chronologically ordered hourly & daily forecasts; exact GPS coordinates are never included.
     */
    internal fun buildWeatherContextPrompt(
        snapshot: WeatherSnapshot,
        deviceNowMillis: Long = System.currentTimeMillis(),
    ): String {
        val deviceClockFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val deviceTimeFormatted = deviceClockFormat.format(Date(deviceNowMillis))
        val deviceDatePrefix = deviceTimeFormatted.substringBefore(" ")
        val snapshotClock = snapshot.localTimeIso.substringAfter("T", snapshot.localTimeIso).ifBlank { "actual" }

        val orderedHourly = snapshot.hourlyForecast
            .sortedBy { it.timeIso }
            .take(18)
        val orderedDaily = snapshot.dailyForecast
            .sortedBy { it.dateIso }

        return buildString {
            appendLine("Contexto temporal sincronizado:")
            appendLine("- Hora local exacta del dispositivo: $deviceTimeFormatted")
            appendLine("- Ciudad: ${snapshot.cityName} (hora de lectura: $snapshotClock)")
            appendLine()
            appendLine("Clima actual:")
            appendLine("- Estado: ${snapshot.conditionLabel}")
            appendLine("- Temperatura: ${snapshot.temperatureC.roundToInt()}° (sensación ${snapshot.apparentTemperatureC.roundToInt()}°)")
            appendLine("- Máx/Mín hoy: ${snapshot.tempMaxC.roundToInt()}° / ${snapshot.tempMinC.roundToInt()}°")
            appendLine("- Humedad: ${snapshot.humidityPercent}%")
            appendLine("- Viento: ${snapshot.windSpeedKmh.roundToInt()} km/h")
            appendLine("- Probabilidad de lluvia hoy: ${snapshot.precipitationProbabilityMax}%")
            appendLine("- Índice UV máximo: ${snapshot.uvIndexMax.roundToInt()}")

            if (orderedHourly.isNotEmpty()) {
                appendLine()
                appendLine("Pronóstico por horas (ordenado cronológicamente):")
                orderedHourly.forEach { hourItem ->
                    val datePart = hourItem.timeIso.substringBefore("T", "")
                    val hourLabel = hourItem.timeIso.substringAfter("T", hourItem.timeIso).take(5)
                    val dayPrefix = if (datePart == deviceDatePrefix || datePart.isBlank()) "Hoy" else datePart
                    val hourState = com.example.model.WeatherVisualState
                        .fromWmoCode(hourItem.weatherCode, isDay = true)
                        .conditionLabelEs
                    appendLine("- $dayPrefix $hourLabel: ${hourItem.temperatureC.roundToInt()}°, lluvia ${hourItem.precipitationProbability}%, $hourState")
                }
            }

            if (orderedDaily.isNotEmpty()) {
                appendLine()
                appendLine("Pronóstico diario (ordenado por fecha):")
                orderedDaily.forEachIndexed { index, dayItem ->
                    val dayTag = when (index) {
                        0 -> "Hoy (${dayItem.dateIso})"
                        1 -> "Mañana (${dayItem.dateIso})"
                        else -> dayItem.dateIso
                    }
                    appendLine("- $dayTag: Máx ${dayItem.tempMaxC.roundToInt()}° / Mín ${dayItem.tempMinC.roundToInt()}°, lluvia ${dayItem.precipitationProbabilityMax}%")
                }
            }
        }.trim()
    }

    private sealed interface ApiCallResult {
        data class Ok(val text: String) : ApiCallResult
        data class Err(val userMessage: String) : ApiCallResult
    }

    private suspend fun executeGenerateContent(
        apiKey: String,
        modelId: String,
        userPrompt: String,
    ): ApiCallResult {
        return try {
            val request = GeminiGenerateContentRequest(
                systemInstruction = GeminiContentDto(
                    parts = listOf(GeminiPartDto(text = SYSTEM_PROMPT)),
                ),
                contents = listOf(
                    GeminiContentDto(
                        role = "user",
                        parts = listOf(GeminiPartDto(text = userPrompt)),
                    ),
                ),
                generationConfig = GeminiGenerationConfigDto(
                    temperature = 0.6,
                    maxOutputTokens = 120,
                ),
            )
            val response = geminiApi.generateContent(
                model = modelId,
                apiKey = apiKey,
                body = request,
            )
            if (response.isSuccessful) {
                val text = response.body()
                    ?.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.trim()
                if (!text.isNullOrBlank()) {
                    ApiCallResult.Ok(text)
                } else {
                    ApiCallResult.Err("Gemini devolvió una respuesta vacía.")
                }
            } else {
                val message = when (response.code()) {
                    400 -> "Solicitud inválida para el modelo $modelId."
                    401, 403 -> "Clave de API inválida o sin permisos."
                    404 -> "El modelo $modelId no está disponible para tu clave."
                    429 -> "Se agotó temporalmente la cuota de Gemini."
                    else -> "Error del servicio Gemini (${response.code()})."
                }
                ApiCallResult.Err(message)
            }
        } catch (_: IOException) {
            ApiCallResult.Err("Sin conexión a internet para consultar la IA.")
        } catch (e: Exception) {
            ApiCallResult.Err(e.localizedMessage ?: "Error al conectar con Gemini.")
        }
    }

    internal fun sanitizePlainText(raw: String): String {
        return raw
            .replace("**", "")
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .replace(Regex("(?i)\\s*por ciento\\b"), "%")
            .replace(Regex("(?i)(\\d+)\\s*grados?\\b"), "$1°")
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
