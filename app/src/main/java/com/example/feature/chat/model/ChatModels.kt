package com.example.feature.chat.model

/**
 * Context payload containing the verification report information
 * passed from the Verification Engine to Ask VeriLens.
 */
data class VerificationContext(
    val reportId: Long,
    val originalClaim: String,
    val verifiedInformation: String,
    val assessment: String,
    val confidence: Int,
    val verdict: String,
    val evidenceSummary: List<String>,
    val trustedSources: List<ChatSource>,
    val recommendations: List<String>,
    val reasoning: String,
    val category: String,
    val timestamp: Long
)

/**
 * A trusted evidence source referenced by the verification report.
 */
data class ChatSource(
    val name: String,
    val publisher: String,
    val publicationDate: String,
    val trustLevel: String, // "High", "Medium", "Questionable"
    val citationUrl: String
)

/**
 * Chat message representing user or AI turns.
 */
data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sources: List<ChatSource> = emptyList(),
    val isStreaming: Boolean = false,
    val isRefusal: Boolean = false
)

enum class MessageSender {
    USER,
    ASSISTANT
}
