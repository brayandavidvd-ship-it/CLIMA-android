package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.GeminiModelOption
import com.example.model.SavedLocation
import com.example.model.WeatherProviderOption
import com.example.model.WeatherSnapshot
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.climaDataStore by preferencesDataStore(name = "clima_prefs")

data class ClimaSettingsData(
    val aiEnabled: Boolean = false,
    val aiConsentAccepted: Boolean = false,
    val apiKey: String = "",
    val selectedModel: GeminiModelOption = GeminiModelOption.FLASH_LITE,
    val selectedWeatherProvider: WeatherProviderOption = WeatherProviderOption.OPEN_METEO,
    val reduceAnimations: Boolean = false,
)

class ClimaPreferencesDataStore(
    private val context: Context,
    private val moshi: Moshi,
    private val cryptoManager: KeystoreCryptoManager,
) {
    private object Keys {
        val CACHED_WEATHER_JSON = stringPreferencesKey("cached_weather_json")
        val SAVED_LOCATION_JSON = stringPreferencesKey("saved_location_json")
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_CONSENT_ACCEPTED = booleanPreferencesKey("ai_consent_accepted")
        val ENCRYPTED_GEMINI_KEY = stringPreferencesKey("encrypted_gemini_key")
        val SELECTED_GEMINI_MODEL = stringPreferencesKey("selected_gemini_model")
        val SELECTED_WEATHER_PROVIDER = stringPreferencesKey("selected_weather_provider")
        val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        val AI_CACHE_MAP_JSON = stringPreferencesKey("ai_cache_map_json")
        val LAST_AUTO_SUMMARY_MILLIS = longPreferencesKey("last_auto_summary_millis")
        val LAST_AUTO_SUMMARY_TEXT = stringPreferencesKey("last_auto_summary_text")
        val LAST_AUTO_SUMMARY_TEMP = stringPreferencesKey("last_auto_summary_temp")
        val LAST_AUTO_SUMMARY_STATE = stringPreferencesKey("last_auto_summary_state")
    }

    private val weatherAdapter = moshi.adapter(WeatherSnapshot::class.java)
    private val locationAdapter = moshi.adapter(SavedLocation::class.java)
    private val stringMapAdapter = moshi.adapter<Map<String, String>>(
        Types.newParameterizedType(Map::class.java, String::class.java, String::class.java),
    )

    val settingsFlow: Flow<ClimaSettingsData> = context.climaDataStore.data.map { prefs ->
        val encryptedKey = prefs[Keys.ENCRYPTED_GEMINI_KEY].orEmpty()
        ClimaSettingsData(
            aiEnabled = prefs[Keys.AI_ENABLED] ?: false,
            aiConsentAccepted = prefs[Keys.AI_CONSENT_ACCEPTED] ?: false,
            apiKey = cryptoManager.decrypt(encryptedKey),
            selectedModel = GeminiModelOption.fromModelId(prefs[Keys.SELECTED_GEMINI_MODEL]),
            selectedWeatherProvider = WeatherProviderOption.fromId(prefs[Keys.SELECTED_WEATHER_PROVIDER]),
            reduceAnimations = prefs[Keys.REDUCE_ANIMATIONS] ?: false,
        )
    }

    suspend fun getCachedWeather(): WeatherSnapshot? {
        val json = context.climaDataStore.data.first()[Keys.CACHED_WEATHER_JSON] ?: return null
        return runCatching { weatherAdapter.fromJson(json) }.getOrNull()
    }

    suspend fun saveCachedWeather(snapshot: WeatherSnapshot) {
        val json = weatherAdapter.toJson(snapshot)
        context.climaDataStore.edit { prefs ->
            prefs[Keys.CACHED_WEATHER_JSON] = json
        }
    }

    suspend fun getSavedLocation(): SavedLocation? {
        val json = context.climaDataStore.data.first()[Keys.SAVED_LOCATION_JSON] ?: return null
        return runCatching { locationAdapter.fromJson(json) }.getOrNull()
    }

    suspend fun saveLocation(location: SavedLocation) {
        val json = locationAdapter.toJson(location)
        context.climaDataStore.edit { prefs ->
            prefs[Keys.SAVED_LOCATION_JSON] = json
        }
    }

    suspend fun setAiEnabled(enabled: Boolean, consentAccepted: Boolean) {
        context.climaDataStore.edit { prefs ->
            prefs[Keys.AI_ENABLED] = enabled
            if (consentAccepted) {
                prefs[Keys.AI_CONSENT_ACCEPTED] = true
            }
        }
    }

    suspend fun saveApiKey(rawApiKey: String) {
        val trimmed = rawApiKey.trim()
        val encrypted = if (trimmed.isEmpty()) "" else cryptoManager.encrypt(trimmed)
        context.climaDataStore.edit { prefs ->
            prefs[Keys.ENCRYPTED_GEMINI_KEY] = encrypted
        }
    }

    suspend fun setSelectedModel(model: GeminiModelOption) {
        context.climaDataStore.edit { prefs ->
            prefs[Keys.SELECTED_GEMINI_MODEL] = model.modelId
        }
    }

    suspend fun setSelectedWeatherProvider(provider: WeatherProviderOption) {
        context.climaDataStore.edit { prefs ->
            prefs[Keys.SELECTED_WEATHER_PROVIDER] = provider.id
        }
    }

    suspend fun setReduceAnimations(reduce: Boolean) {
        context.climaDataStore.edit { prefs ->
            prefs[Keys.REDUCE_ANIMATIONS] = reduce
        }
    }

    suspend fun getCachedAiResponse(cacheKey: String): String? {
        val json = context.climaDataStore.data.first()[Keys.AI_CACHE_MAP_JSON] ?: return null
        val map = runCatching { stringMapAdapter.fromJson(json) }.getOrNull() ?: return null
        return map[cacheKey]
    }

    suspend fun putCachedAiResponse(cacheKey: String, responseText: String) {
        context.climaDataStore.edit { prefs ->
            val currentJson = prefs[Keys.AI_CACHE_MAP_JSON]
            val currentMap = currentJson?.let {
                runCatching { stringMapAdapter.fromJson(it) }.getOrNull()
            }?.toMutableMap() ?: mutableMapOf()
            // Keep cache bounded to last 24 entries
            if (currentMap.size >= 24) {
                val firstKey = currentMap.keys.firstOrNull()
                if (firstKey != null) currentMap.remove(firstKey)
            }
            currentMap[cacheKey] = responseText
            prefs[Keys.AI_CACHE_MAP_JSON] = stringMapAdapter.toJson(currentMap)
        }
    }

    data class AutoSummaryMeta(
        val timestampMillis: Long,
        val text: String,
        val tempC: Double?,
        val visualStateName: String?,
    )

    suspend fun getAutoSummaryMeta(): AutoSummaryMeta {
        val prefs = context.climaDataStore.data.first()
        return AutoSummaryMeta(
            timestampMillis = prefs[Keys.LAST_AUTO_SUMMARY_MILLIS] ?: 0L,
            text = prefs[Keys.LAST_AUTO_SUMMARY_TEXT].orEmpty(),
            tempC = prefs[Keys.LAST_AUTO_SUMMARY_TEMP]?.toDoubleOrNull(),
            visualStateName = prefs[Keys.LAST_AUTO_SUMMARY_STATE],
        )
    }

    suspend fun saveAutoSummaryMeta(
        timestampMillis: Long,
        text: String,
        tempC: Double,
        visualStateName: String,
    ) {
        context.climaDataStore.edit { prefs ->
            prefs[Keys.LAST_AUTO_SUMMARY_MILLIS] = timestampMillis
            prefs[Keys.LAST_AUTO_SUMMARY_TEXT] = text
            prefs[Keys.LAST_AUTO_SUMMARY_TEMP] = tempC.toString()
            prefs[Keys.LAST_AUTO_SUMMARY_STATE] = visualStateName
        }
    }
}
