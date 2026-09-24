package com.example.feature.chat

import com.example.feature.chat.engine.EvidenceAssistantEngine
import com.example.feature.chat.engine.PromptBuilder
import com.example.feature.chat.engine.SuggestedQuestionEngine
import com.example.feature.chat.model.ChatMessage
import com.example.feature.chat.model.ChatSource
import com.example.feature.chat.model.MessageSender
import com.example.feature.chat.model.VerificationContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AskVeriLensLogicTest {

    private val sampleContext = VerificationContext(
        reportId = 1L,
        originalClaim = "New education grant announced by ministry",
        verifiedInformation = "Official press release confirmed eligibility requirements.",
        assessment = "Claim holds true when checked with ministry gazette.",
        confidence = 94,
        verdict = "VERIFIED",
        evidenceSummary = listOf("Ministry released circular on portal", "Gazette publication confirms budget"),
        trustedSources = listOf(
            ChatSource("PIB Portal", "Govt", "Recent", "High", "https://pib.gov.in")
        ),
        recommendations = listOf("Safe to reference with direct link"),
        reasoning = "Corroborated across 2 primary registries.",
        category = "TEXT",
        timestamp = 1700000000000L
    )

    @Test
    fun `refuses unrelated chit-chat, poetry, and code requests`() {
        val codingQuery = "write python code for a calculator"
        val poemQuery = "write me a poem about love"
        val jokeQuery = "tell me a joke"

        assertTrue(EvidenceAssistantEngine.isUnrelatedQuery(codingQuery))
        assertTrue(EvidenceAssistantEngine.isUnrelatedQuery(poemQuery))
        assertTrue(EvidenceAssistantEngine.isUnrelatedQuery(jokeQuery))

        val (response, sources) = EvidenceAssistantEngine.generateOfflineEvidenceResponse(codingQuery, sampleContext)
        assertTrue(response.contains("I specialize in helping users understand and verify information"))
        assertTrue(sources.isEmpty())
    }

    @Test
    fun `answers evidence-based and confidence questions using report context`() {
        val query = "Why is the confidence score 94%?"
        assertFalse(EvidenceAssistantEngine.isUnrelatedQuery(query))

        val (response, sources) = EvidenceAssistantEngine.generateOfflineEvidenceResponse(query, sampleContext)
        assertTrue(response.contains("94%"))
        assertTrue(sources.isNotEmpty())
        assertEquals("PIB Portal", sources.first().name)
    }

    @Test
    fun `provides educational media literacy answers without hallucinations`() {
        val query = "Teach me media literacy"
        val (response, _) = EvidenceAssistantEngine.generateOfflineEvidenceResponse(query, null)
        assertTrue(response.contains("Emotional Triggering") || response.contains("Misinformation"))
    }

    @Test
    fun `builds user prompt with full verification report context`() {
        val prompt = PromptBuilder.buildUserPrompt(
            query = "What sources did you check?",
            context = sampleContext,
            history = listOf(
                ChatMessage("1", MessageSender.USER, "Hello"),
                ChatMessage("2", MessageSender.ASSISTANT, "Welcome")
            )
        )

        assertTrue(prompt.contains("New education grant announced by ministry"))
        assertTrue(prompt.contains("PIB Portal"))
        assertTrue(prompt.contains("What sources did you check?"))
    }

    @Test
    fun `suggested question engine generates context-relevant questions`() {
        val suggestions = SuggestedQuestionEngine.getSuggestedQuestions(sampleContext)
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.contains("confidence") || it.contains("report") })
    }
}
