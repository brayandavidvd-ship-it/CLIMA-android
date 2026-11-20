package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentRequest(
    @Json(name = "system_instruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<GeminiPart>,
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String,
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Double = 0.4,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int = 220,
    @Json(name = "responseMimeType") val responseMimeType: String = "application/json",
    @Json(name = "responseSchema") val responseSchema: GeminiSchema? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiSchema(
    @Json(name = "type") val type: String,
    @Json(name = "properties") val properties: Map<String, GeminiSchemaProperty>? = null,
    @Json(name = "required") val required: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiSchemaProperty(
    @Json(name = "type") val type: String,
    @Json(name = "description") val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?,
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?,
)

@JsonClass(generateAdapter = true)
data class GeminiStructuredWeatherReply(
    @Json(name = "answer") val answer: String,
    @Json(name = "includeRainBars") val includeRainBars: Boolean = false,
)
