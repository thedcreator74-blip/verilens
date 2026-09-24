package com.example.feature.verification.model

enum class VerificationStep(val stepIndex: Int, val title: String) {
    UPLOADING_PREPROCESSING(1, "Pre-processing input"),
    ANALYZING_IMAGE_QUALITY(2, "Checking image quality"),
    OCR_TEXT_EXTRACTION(3, "Extracting text & visual cues"),
    CLAIM_NORMALIZATION(4, "Formulating checkable claim"),
    DOMAIN_TRUST_CHECK(5, "Verifying domain & entity reputation"),
    SEARCHING_EVIDENCE(6, "Retrieving cross-source evidence"),
    TEMPORAL_ANALYSIS(7, "Analyzing timeline & origin"),
    EVIDENCE_CLUSTERING(8, "Clustering corroborating sources"),
    GEMINI_REASONING(9, "Synthesizing evidence verdict"),
    FINALIZING_REPORT(10, "Generating comprehensive report")
}

data class VerificationProgress(
    val step: VerificationStep = VerificationStep.UPLOADING_PREPROCESSING,
    val detailMessage: String = "Initializing...",
    val progressPercent: Float = 0.1f
)
