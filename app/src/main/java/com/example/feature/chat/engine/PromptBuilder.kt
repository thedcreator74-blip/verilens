package com.example.feature.chat.engine

import com.example.feature.chat.model.ChatMessage
import com.example.feature.chat.model.VerificationContext

/**
 * Builds rigorous, specialized prompts for the Evidence Verification Assistant.
 * Strictly forbids general AI chit-chat, poetry, coding, and hallucinations.
 */
object PromptBuilder {

    private const val SYSTEM_INSTRUCTION = """
You are Ask VeriLens, an Evidence Verification Assistant integrated with the VeriLens AI platform.
You are NOT a general-purpose AI chatbot or ChatGPT.
Your sole mission is to help users better understand verification reports, evidence, trusted sources, and media literacy.

SPECIALIZATION:
- News verification
- Claim explanation
- Evidence interpretation
- Media literacy education
- Source credibility analysis
- Government announcements and public records
- Understanding VeriLens confidence scores and reports
- Manual verification guidance

STRICT LIMITATIONS & REFUSAL POLICY:
If the user asks for general tasks unrelated to verification (e.g. writing code, poems, creative fiction, general trivia, jokes, personal chit-chat, homework solvers):
You MUST politely refuse in a single professional sentence such as:
"I specialize in helping users understand and verify information, analyze evidence, and practice media literacy. How can I assist with your verification report?"
Never fulfill unrelated requests.

EVIDENCE & SOURCE CITATION MANDATES:
1. Every factual claim must be derived from the current verification report and provided evidence points.
2. Never invent or hallucinate sources. Only cite the trusted sources provided in the context.
3. If information or evidence is insufficient or missing, state so transparently.
4. Maintain a neutral, professional, educational, and objective tone.
5. If the user asks for explanations in another language (e.g., Tamil, Hindi, Spanish), translate your evidence-based explanation accurately while keeping the factual evidence intact.
6. When explaining like they are 12 ("Explain like I'm 12"), break down complicated jargon into clear, accessible concepts without compromising factual truth.
"""

    fun buildSystemInstruction(): String = SYSTEM_INSTRUCTION.trimIndent()

    fun buildUserPrompt(
        query: String,
        context: VerificationContext?,
        history: List<ChatMessage>
    ): String {
        val sb = StringBuilder()

        if (context != null) {
            sb.appendLine("=== CURRENT VERIFICATION REPORT CONTEXT ===")
            sb.appendLine("Original Claim: \"${context.originalClaim}\"")
            sb.appendLine("Category: ${context.category}")
            sb.appendLine("Verdict: ${context.verdict}")
            sb.appendLine("Credibility Score / Confidence: ${context.confidence}%")
            sb.appendLine("Assessment Summary: ${context.assessment}")
            sb.appendLine("Reasoning: ${context.reasoning}")
            sb.appendLine("Verified Information / Facts:")
            context.evidenceSummary.forEachIndexed { i, fact ->
                sb.appendLine("  ${i + 1}. $fact")
            }
            sb.appendLine("Trusted Sources Checked:")
            context.trustedSources.forEachIndexed { i, src ->
                sb.appendLine("  ${i + 1}. ${src.name} (${src.publisher}) - Trust: ${src.trustLevel} - URL: ${src.citationUrl} - Date: ${src.publicationDate}")
            }
            sb.appendLine("Recommendations:")
            context.recommendations.forEachIndexed { i, rec ->
                sb.appendLine("  ${i + 1}. $rec")
            }
            sb.appendLine("===========================================")
            sb.appendLine()
        } else {
            sb.appendLine("=== NO ACTIVE VERIFICATION REPORT SELECTED ===")
            sb.appendLine("The user is in general media literacy / verification methodology mode.")
            sb.appendLine("==============================================")
            sb.appendLine()
        }

        if (history.isNotEmpty()) {
            sb.appendLine("=== CONVERSATION HISTORY ===")
            // Include up to last 8 messages for memory
            val recent = history.takeLast(8)
            for (msg in recent) {
                val role = if (msg.sender == com.example.feature.chat.model.MessageSender.USER) "User" else "Assistant"
                sb.appendLine("$role: ${msg.text}")
            }
            sb.appendLine("============================")
            sb.appendLine()
        }

        sb.appendLine("Current User Question: $query")
        return sb.toString()
    }
}
