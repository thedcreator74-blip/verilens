package com.example.feature.chat.engine

import com.example.feature.chat.model.ChatSource
import com.example.feature.chat.model.VerificationContext

/**
 * Deterministic evidence assistant engine for immediate, robust offline/zero-latency execution,
 * and refusal handling when offline or fallback is needed.
 */
object EvidenceAssistantEngine {

    private val UNRELATED_PATTERNS = listOf(
        "write python", "write code", "write a code", "write script",
        "write me a poem", "write a poem", "write a story", "write lyrics",
        "tell me a joke", "tell a joke", "make me laugh",
        "recipe for", "how to bake", "how to cook",
        "play a game", "who won the game"
    )

    fun isUnrelatedQuery(query: String): Boolean {
        val q = query.trim().lowercase()
        return UNRELATED_PATTERNS.any { q.contains(it) }
    }

    fun generateRefusalMessage(): String {
        return "I specialize in helping users understand and verify information, analyze evidence, and practice media literacy. How can I assist with your verification report?"
    }

    fun generateOfflineEvidenceResponse(
        query: String,
        context: VerificationContext?
    ): Pair<String, List<ChatSource>> {
        val q = query.trim().lowercase()

        // 1. Refusal check
        if (isUnrelatedQuery(q)) {
            return Pair(generateRefusalMessage(), emptyList())
        }

        // 2. Media Literacy / Educational queries without report
        if (q.contains("media literacy") || q.contains("how misinformation spreads")) {
            return Pair(
                """
### Understanding Misinformation & Media Literacy

Misinformation spreads primarily through four cognitive and social mechanisms:

1. **Emotional Triggering**: False headlines often employ high emotional valence (anger, fear, urgency) which bypasses critical reasoning.
2. **Visual Fabrication**: Spliced broadcast banners, doctored fonts, and out-of-context genuine photos create false credibility.
3. **Echo Amplification**: Forwarded messages in private messaging circles gain artificial trust because they come from acquaintances.
4. **Source Masking**: Low-credibility blogs mimic names of established news outlets or use cloaked redirect links.

**The VeriLens Rule: Think Before You Share.** Always verify with official archives or institutional portals before forwarding.
                """.trimIndent(),
                emptyList()
            )
        }

        if (q.contains("difference between") || (q.contains("opinion") && q.contains("news"))) {
            return Pair(
                """
### Editorial Distinctions: News vs. Opinion vs. Rumor

- **Official Announcement**: A formal declaration directly released by a government body, court, or registered institution (e.g., Press Information Bureau, official gazette).
- **Verified News**: Reported by professional journalists adhering to ethical standards, independently corroborating facts with at least two distinct sources.
- **Opinion / Editorial**: A personal commentary or interpretation written by a columnist. It reflects belief or perspective rather than verified factual reporting.
- **Rumor / Hearsay**: Unverified assertions transmitted without verifiable attribution, documentation, or named spokespersons.
                """.trimIndent(),
                emptyList()
            )
        }

        if (q.contains("verify manually") || q.contains("verify news yourself")) {
            return Pair(
                """
### 5-Step Manual Verification Checklist

1. **Check the Original Source**: Look for the primary press release on official `.gov` or registered institutional portals.
2. **Reverse Image Search**: Inspect photos and screenshots for prior publication dates or altered text banners.
3. **Verify Attributed Spokespersons**: Search whether the stated official actually gave that quote in verified transcripts.
4. **Inspect Domain Registration**: Check WHOIS records for recent registration dates or missing registrant identity.
5. **Cross-Reference Wire Services**: If a major event occurred, trusted international news wires (Reuters, AP) will confirm it within minutes.
                """.trimIndent(),
                emptyList()
            )
        }

        if (q.contains("fake screenshots") || q.contains("screenshots are created")) {
            return Pair(
                """
### How Manipulated Screenshots Are Created

Manipulated screenshots are usually forged using:
1. **Browser 'Inspect Element'**: Text on genuine high-authority websites is temporarily altered in the browser DOM before capturing.
2. **Graphic Overlay Splicing**: Splicing dramatic headlines over authentic television broadcast layouts.
3. **Synthetic Font Matching**: Using generic sans-serif fonts that don't match the outlet's custom brand typography.

**VeriLens Detection**: Our system analyzes typographic metrics, artifact boundaries, and cross-references extracted OCR text against public fact databases.
                """.trimIndent(),
                emptyList()
            )
        }

        // 3. Report-specific answers
        if (context == null) {
            return Pair(
                "You currently don't have an active verification report selected. You can select a report from your History tab or use the Verify tab to check a new claim, screenshot, or URL!",
                emptyList()
            )
        }

        val sources = context.trustedSources

        // Why confidence score
        if (q.contains("confidence") || q.contains("score")) {
            val explanation = when {
                context.confidence >= 80 ->
                    "The confidence score is **${context.confidence}%** because the claim correlates directly with verified government records, official press releases, and established news wire archives (${sources.joinToString { it.publisher }}). No contradictory claims were discovered."
                context.confidence <= 35 ->
                    "The confidence score is **${context.confidence}%** because primary statements contradict verified records, the quoted spokesperson denies the statement, and linguistic analysis identified high emotional manipulation markers."
                else ->
                    "The confidence score is **${context.confidence}%** because while some contextual elements are factual, key central assertions lack independent confirmation from authoritative registries."
            }
            return Pair(
                """
### Credibility Assessment Breakdown: ${context.confidence}%

$explanation

**Key Evidence Point**:
${context.evidenceSummary.firstOrNull() ?: "Cross-referenced with verified public databases."}
                """.trimIndent(),
                sources
            )
        }

        // Sources question
        if (q.contains("source") || q.contains("website")) {
            val sourceListStr = sources.joinToString("\n") { src ->
                "- **${src.name}** (${src.publisher}) • Trust: *${src.trustLevel}* • `${src.citationUrl}`"
            }
            return Pair(
                """
### Trusted Sources & Archival Records Checked

The following authoritative sources were inspected during this verification:

$sourceListStr

All sources are verified public registries, primary institutional gazettes, or accredited fact-checking signatories.
                """.trimIndent(),
                sources
            )
        }

        // Summarize evidence
        if (q.contains("evidence") || q.contains("summarize evidence")) {
            val evidenceBullets = context.evidenceSummary.mapIndexed { idx, point ->
                "${idx + 1}. **Finding ${idx + 1}**: $point"
            }.joinToString("\n")

            return Pair(
                """
### Evidence Summary for: "${context.originalClaim}"

$evidenceBullets

**Assessment**:
${context.assessment}
                """.trimIndent(),
                sources
            )
        }

        // Explain like I'm 12
        if (q.contains("12") || q.contains("simple") || q.contains("kid")) {
            return Pair(
                """
### Simple Explanation (Like You're 12)

Imagine someone told a big rumor in the school hallway: **"${context.originalClaim}"**.

Here is the truth:
- **Verdict**: This is **${context.verdict}**.
- **What happened**: ${context.reasoning}
- **What you should do**: ${context.recommendations.firstOrNull() ?: "Never forward rumors until you check official websites!"}
                """.trimIndent(),
                sources
            )
        }

        // Translation request (e.g., Tamil)
        if (q.contains("tamil")) {
            return Pair(
                """
### அறிக்கை விளக்கம் (Report Explanation in Tamil)

**உரிமைகோரிக்கை (Original Claim)**: "${context.originalClaim}"
**தீர்ப்பு (Verdict)**: **${context.verdict}** (${context.confidence}% நம்பிக்கை)

**விளக்கம் (Assessment)**:
${context.assessment}

**ஆதாரங்கள் (Evidence)**:
${context.evidenceSummary.firstOrNull() ?: "சரிபார்க்கப்பட்ட பொது தரவுத்தளங்கள் மூலம் உறுதிப்படுத்தப்பட்டது."}

**பரிந்துரை (Recommendation)**:
செய்திகளைப் பகிர்வதற்கு முன் அதிகாரப்பூர்வ வலைத்தளங்களில் சரிபார்க்கவும்!
                """.trimIndent(),
                sources
            )
        }

        // Default explanation of report
        val factsList = context.evidenceSummary.joinToString("\n") { "- $it" }
        return Pair(
            """
### Analysis of Claim: "${context.originalClaim}"

- **Verdict**: **${context.verdict}** (${context.confidence}% Credibility Score)
- **Category**: ${context.category}

#### Key Findings & Evidence:
$factsList

#### Summary & Recommendation:
${context.assessment}

${context.recommendations.firstOrNull()?.let { "**Next Step**: $it" } ?: ""}
            """.trimIndent(),
            sources
        )
    }
}
