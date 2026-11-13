package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentRequest(
    @Json(name = "system_instruction") val systemInstruction: GeminiContentDto,
    @Json(name = "contents") val contents: List<GeminiContentDto>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfigDto,
)

@JsonClass(generateAdapter = true)
data class GeminiContentDto(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<GeminiPartDto>,
)

@JsonClass(generateAdapter = true)
data class GeminiPartDto(
    @Json(name = "text") val text: String,
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfigDto(
    @Json(name = "temperature") val temperature: Double = 0.6,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int = 120,
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidateDto>?,
    @Json(name = "error") val error: GeminiApiErrorDto?,
)

@JsonClass(generateAdapter = true)
data class GeminiCandidateDto(
    @Json(name = "content") val content: GeminiContentDto?,
)

@JsonClass(generateAdapter = true)
data class GeminiApiErrorDto(
    @Json(name = "code") val code: Int?,
    @Json(name = "message") val message: String?,
    @Json(name = "status") val status: String?,
)
