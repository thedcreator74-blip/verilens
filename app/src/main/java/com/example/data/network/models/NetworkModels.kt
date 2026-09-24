package com.example.data.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VerificationRequest(
    @Json(name = "type") val type: String, // "SCREENSHOT", "TEXT", "LINK"
    @Json(name = "content") val content: String,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class VerificationResponse(
    @Json(name = "id") val id: String,
    @Json(name = "credibilityScore") val credibilityScore: Int,
    @Json(name = "verdict") val verdict: String,
    @Json(name = "summary") val summary: String,
    @Json(name = "evidencePoints") val evidencePoints: List<String>,
    @Json(name = "sources") val sources: List<SourceItem>,
    @Json(name = "recommendations") val recommendations: List<String>
)

@JsonClass(generateAdapter = true)
data class SourceItem(
    @Json(name = "name") val name: String,
    @Json(name = "url") val url: String,
    @Json(name = "trustScore") val trustScore: Int
)
