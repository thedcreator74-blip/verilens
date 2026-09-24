package com.example.ui.screens.report

import com.example.feature.verification.model.CollectedEvidenceItem
import com.example.feature.verification.model.Verdict

data class ReportDetail(
    val id: Long,
    val title: String,
    val originalClaim: String,
    val verifiedInformation: String,
    val inputType: String,
    val timestamp: Long,
    val credibilityScore: Int,
    val verdict: Verdict,
    val summary: String,
    val reasoning: String,
    val category: String,
    val sources: List<CollectedEvidenceItem>,
    val recommendations: List<String>,
    val disclaimer: String
)
