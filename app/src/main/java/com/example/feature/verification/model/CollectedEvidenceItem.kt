package com.example.feature.verification.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CollectedEvidenceItem(
    val title: String,
    val publisher: String,
    val publicationDate: String,
    val snippet: String,
    val url: String,
    val trustLevel: String = "Medium", // High, Authoritative, Medium, Low, Suspicious
    val category: String = "News",
    val relevanceScore: Int = 85,
    val evidenceRole: String = "PRIMARY_SOURCE", // PRIMARY_SOURCE, FACT_CHECK, CORROBORATION, CONTEXT
    val claimAlignment: String = "NEUTRAL", // SUPPORTS, REFUTES, CONTEXT, NEUTRAL
    val isDebunk: Boolean = false,
    val clusterId: String? = null,
    val retrievalQuery: String = ""
)

@JsonClass(generateAdapter = true)
data class ExtractedVisualAnalysis(
    val detectedText: String = "",
    val detectedUrls: List<String> = emptyList(),
    val detectedQrPayload: String? = null,
    val qualityStatus: String = "GOOD", // GOOD, TOO_BLURRY, TOO_DARK, TOO_BRIGHT, NO_READABLE_CONTENT
    val detectedAppOrPlatform: String? = null,
    val hasManipulatedVisuals: Boolean = false,
    val entitiesDetected: List<String> = emptyList()
)
