package com.example.feature.verification.model

import com.squareup.moshi.JsonClass

enum class VerificationStep(
    val stepIndex: Int,
    val label: String,
    val description: String
) {
    UPLOADING_PREPROCESSING(1, "Processing Input", "Preparing and standardizing image resolution..."),
    EXTRACTING_TEXT(2, "Extracting Text", "Running on-device OCR across visual surface..."),
    ANALYZING_SCREENSHOT(3, "Analyzing Screenshot", "Auditing layout, branding & visual manipulation signs..."),
    IDENTIFYING_CLAIM(4, "Identifying Primary Claim", "Filtering UI noise, ads & isolating primary proposition..."),
    DETECTING_CATEGORY(5, "Detecting Category", "Routing to specialized verification strategy..."),
    SEARCHING_TRUSTED_SOURCES(6, "Searching Trusted Sources", "Querying official registries and authoritative archives..."),
    COLLECTING_EVIDENCE(7, "Collecting Evidence", "Extracting relevant snippets, publications & timestamps..."),
    COMPARING_EVIDENCE(8, "Comparing Evidence", "Corroborating statements and ranking source credibility..."),
    GENERATING_REPORT(9, "Generating Report", "Synthesizing explainable evidence report..."),
    COMPLETED(10, "Verification Completed", "Report generated successfully."),
    FAILED(-1, "Verification Failed", "An error occurred during verification.")
}

data class VerificationProgress(
    val step: VerificationStep = VerificationStep.UPLOADING_PREPROCESSING,
    val detailMessage: String = "",
    val progressPercent: Float = 0.1f
)

@JsonClass(generateAdapter = true)
data class ExtractedVisualAnalysis(
    val headline: String = "",
    val logos: List<String> = emptyList(),
    val layout: String = "",
    val manipulationIndicators: List<String> = emptyList(),
    val primaryClaim: String = "",
    val contextNotes: String = ""
)

enum class ClaimCategory(val displayName: String) {
    GOVERNMENT("Government & Public Policy"),
    HEALTH("Health & Medical Science"),
    TECHNOLOGY("Technology & Software"),
    FINANCE("Finance, Economy & Banking"),
    EDUCATION("Education & Academics"),
    WEATHER("Meteorology & Climate"),
    SPORTS("Sports & Athletics"),
    BUSINESS("Business & Corporate Affairs"),
    GENERAL_NEWS("News & Current Affairs")
}

@JsonClass(generateAdapter = true)
data class CollectedEvidenceItem(
    val title: String,
    val publisher: String,
    val publicationDate: String = "Recent",
    val snippet: String,
    val url: String,
    val trustLevel: String, // "High", "Medium", "Questionable"
    val category: String,
    val relevanceScore: Int = 85
)

@JsonClass(generateAdapter = true)
data class GeminiEvidenceReportPayload(
    val verifiedInformation: String,
    val assessment: String,
    val confidence: Int,
    val verdict: String, // "VERIFIED", "MISLEADING", "QUESTIONABLE", "INCONCLUSIVE"
    val evidenceSummary: List<String>,
    val reasoning: String,
    val recommendations: List<String>,
    val disclaimer: String = "VeriLens AI evaluates claims against collected evidence. Exercise personal critical judgment before sharing."
)
