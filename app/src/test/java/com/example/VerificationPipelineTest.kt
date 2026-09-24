package com.example

import com.example.feature.verification.claim.ClaimNormalizer
import com.example.feature.verification.engine.GeminiEvidenceReasoner
import com.example.feature.verification.model.CollectedEvidenceItem
import com.example.feature.verification.model.Verdict
import com.example.feature.verification.model.VerificationInputType
import com.example.feature.verification.trust.DomainTrustResolverImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificationPipelineTest {

    private val claimNormalizer = ClaimNormalizer()
    private val domainTrustResolver = DomainTrustResolverImpl()
    private val evidenceReasoner = GeminiEvidenceReasoner()

    @Test
    fun testCameraInputTypeExists() {
        assertEquals("CAMERA", VerificationInputType.CAMERA.name)
        val types = VerificationInputType.values().map { it.name }
        assertTrue(types.contains("CAMERA"))
        assertTrue(types.contains("SCREENSHOT"))
        assertTrue(types.contains("TEXT"))
        assertTrue(types.contains("URL"))
    }

    @Test
    fun testClaimNormalization() {
        val rawHeadline = "Government announces nationwide holiday for tech workers next week."
        val claim = claimNormalizer.normalize(rawHeadline)

        assertTrue(claim.isCheckable)
        assertEquals("FACTUAL", claim.claimType)
        assertTrue(claim.searchQueries.isNotEmpty())
    }

    @Test
    fun testScamDetection() {
        val scamMessage = "URGENT: Click here to claim your $5,000 prize or your bank account will be blocked!"
        val claim = claimNormalizer.normalize(scamMessage)

        assertEquals("SCAM", claim.claimType)

        val result = runBlocking {
            evidenceReasoner.synthesizeVerdict(claim, emptyList())
        }

        assertEquals(Verdict.SCAM_RISK, result.verdict)
        assertTrue(result.credibilityScore < 20)
    }

    @Test
    fun testAuthoritativeDomainResolution() {
        val trust = domainTrustResolver.resolveTrust("https://www.reuters.com/world/india/article-123")
        assertEquals("reuters.com", trust.domain)
        assertEquals("Authoritative", trust.trustLevel)
        assertTrue(trust.isFactChecker)
    }

    @Test
    fun testGovernmentDomainResolution() {
        val trust = domainTrustResolver.resolveTrust("https://pib.gov.in/PressReleasePage.aspx?PRID=12345")
        assertEquals("Authoritative", trust.trustLevel)
        assertTrue(trust.isGovernment)
    }

    @Test
    fun testDebunkEvidenceProducesContradictedVerdict() {
        val claim = claimNormalizer.normalize("Viral picture shows Eiffel Tower on fire in Paris")
        val evidence = listOf(
            CollectedEvidenceItem(
                title = "Fact Check: Viral Eiffel Tower Fire image is AI generated",
                publisher = "Reuters Fact Check",
                publicationDate = "2024",
                snippet = "False: There is no fire at the Eiffel Tower. The image was generated using AI tools.",
                url = "https://reuters.com/fact-check/eiffel-fire",
                trustLevel = "Authoritative",
                isDebunk = true
            )
        )

        val result = runBlocking {
            evidenceReasoner.synthesizeVerdict(claim, evidence)
        }

        assertEquals(Verdict.CONTRADICTED, result.verdict)
        assertTrue(result.credibilityScore < 30)
    }
}
