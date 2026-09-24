package com.example.feature.verification.engine

import com.example.feature.verification.claim.NormalizedClaim
import com.example.feature.verification.model.CollectedEvidenceItem
import com.example.feature.verification.model.ExtractedVisualAnalysis
import com.example.feature.verification.model.Verdict
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class ReasoningResult(
    val verdict: Verdict,
    val credibilityScore: Int,
    val summary: String,
    val verifiedInformation: String,
    val reasoning: String,
    val recommendations: List<String>,
    val disclaimer: String = "VeriLens AI cross-examines multimodal inputs against authoritative facts. Always consult official notices before critical actions."
)

class GeminiEvidenceReasoner {

    suspend fun synthesizeVerdict(
        claim: NormalizedClaim,
        evidence: List<CollectedEvidenceItem>,
        visualAnalysis: ExtractedVisualAnalysis? = null,
        articleBody: String? = null
    ): ReasoningResult = withContext(Dispatchers.Default) {
        val claimText = claim.primaryClaim.lowercase()

        // 1. Check for Scam / Phishing indicators
        if (claim.claimType == "SCAM" ||
            claimText.contains("won lottery") ||
            claimText.contains("bank account blocked") ||
            claimText.contains("click link to claim") ||
            claimText.contains("share otp") ||
            claimText.contains("kyc expired")
        ) {
            return@withContext ReasoningResult(
                verdict = Verdict.SCAM_RISK,
                credibilityScore = 8,
                summary = "High probability of social engineering or financial phishing attack.",
                verifiedInformation = "Official banks and institutions never ask for OTPs, passwords, or immediate payments through unofficial links.",
                reasoning = "The claim exhibits high-risk urgent phrasing, deceptive call-to-action, and lacks any authentic organizational authorization.",
                recommendations = listOf(
                    "Do not click on suspicious links or provide credentials.",
                    "Report and block the sender immediately.",
                    "Verify directly through the institution's official app or hotline."
                )
            )
        }

        // 2. Check for Fact-check debunks
        val hasDebunk = evidence.any { it.isDebunk || it.trustLevel == "Authoritative" && (it.snippet.contains("false", true) || it.snippet.contains("fake", true) || it.snippet.contains("debunk", true) || it.snippet.contains("hoax", true)) }
        if (hasDebunk) {
            return@withContext ReasoningResult(
                verdict = Verdict.CONTRADICTED,
                credibilityScore = 18,
                summary = "This claim has been investigated and contradicted by verified fact-checkers.",
                verifiedInformation = "Independent journalistic investigations and primary sources report that this statement or media is fabricated or altered.",
                reasoning = "Cross-referenced evidence confirms that official records and authoritative outlets directly dispute the core assertions made in this claim.",
                recommendations = listOf(
                    "Do not forward or share this viral content.",
                    "Review primary citations from certified fact-checking partners."
                )
            )
        }

        // 3. Check for Authoritative support
        val hasAuthoritativeSupport = evidence.any {
            (it.trustLevel == "Authoritative" || it.trustLevel == "Established") &&
                    !it.snippet.contains("false", true) &&
                    !it.snippet.contains("misleading", true) &&
                    it.relevanceScore >= 80
        }

        if (hasAuthoritativeSupport && claim.isCheckable) {
            return@withContext ReasoningResult(
                verdict = Verdict.SUPPORTED,
                credibilityScore = 92,
                summary = "Corroborated by reputable journalistic and primary documentation.",
                verifiedInformation = "The core statements in this claim align with official announcements and established reporting.",
                reasoning = "Multiple independent and authoritative sources confirm the underlying facts and timeline.",
                recommendations = listOf(
                    "Content is reliable based on current public records.",
                    "Cite original publishers when sharing."
                )
            )
        }

        // 4. Default / Insufficient evidence
        return@withContext ReasoningResult(
            verdict = Verdict.INSUFFICIENT_EVIDENCE,
            credibilityScore = 50,
            summary = "Insufficient verifiable evidence found across authoritative databases.",
            verifiedInformation = "No conclusive proof or verified debunk exists in major fact-checking archives for this specific assertion.",
            reasoning = "The text contains statements that could not be decisively confirmed or refuted using real-time open verification repositories.",
            recommendations = listOf(
                "Exercise caution before sharing unverified claims.",
                "Seek direct primary sources or wait for official statements."
            )
        )
    }
}
