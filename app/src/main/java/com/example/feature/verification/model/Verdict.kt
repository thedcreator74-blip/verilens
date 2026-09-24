package com.example.feature.verification.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.VerdictContradicted
import com.example.ui.theme.VerdictInsufficient
import com.example.ui.theme.VerdictMisleading
import com.example.ui.theme.VerdictScam
import com.example.ui.theme.VerdictSupported

enum class Verdict(val displayName: String, val badgeColor: Color, val shortExplanation: String) {
    SUPPORTED("Supported / Factual", VerdictSupported, "Claims match official and authoritative reporting."),
    CONTRADICTED("Contradicted / False", VerdictContradicted, "Fact-checkers and primary sources refute this claim."),
    MISLEADING("Misleading / Out of Context", VerdictMisleading, "Partially true but omits key context or uses deceptive framing."),
    INSUFFICIENT_EVIDENCE("Insufficient Evidence", VerdictInsufficient, "Not enough authoritative evidence found to definitively confirm or deny."),
    SCAM_RISK("High Scam Risk", VerdictScam, "Phishing, financial scam, or impersonation patterns detected.");

    companion object {
        fun fromString(value: String?): Verdict {
            return when (value?.uppercase()) {
                "SUPPORTED", "TRUE", "VERIFIED" -> SUPPORTED
                "CONTRADICTED", "FALSE", "FAKE" -> CONTRADICTED
                "MISLEADING", "PARTLY_TRUE" -> MISLEADING
                "SCAM_RISK", "SCAM", "PHISHING" -> SCAM_RISK
                else -> INSUFFICIENT_EVIDENCE
            }
        }
    }
}
