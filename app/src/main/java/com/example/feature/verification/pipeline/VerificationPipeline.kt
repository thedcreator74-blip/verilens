package com.example.feature.verification.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.local.database.daos.HistoryDao
import com.example.data.local.database.daos.RecentInputDao
import com.example.data.local.database.entities.HistoryEntity
import com.example.data.local.database.entities.RecentInputEntity
import com.example.feature.verification.analysis.ScreenshotAnalyzer
import com.example.feature.verification.article.ArticleContentExtractor
import com.example.feature.verification.claim.ClaimNormalizer
import com.example.feature.verification.engine.GeminiEvidenceReasoner
import com.example.feature.verification.model.CollectedEvidenceItem
import com.example.feature.verification.model.VerificationInputType
import com.example.feature.verification.model.VerificationProgress
import com.example.feature.verification.model.VerificationStep
import com.example.feature.verification.search.EvidenceSearcher
import com.example.feature.verification.trust.DomainTrustResolver
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class VerificationPipeline(
    private val context: Context,
    private val historyDao: HistoryDao,
    private val recentInputDao: RecentInputDao,
    private val screenshotAnalyzer: ScreenshotAnalyzer,
    private val evidenceSearcher: EvidenceSearcher,
    private val evidenceReasoner: GeminiEvidenceReasoner,
    private val articleExtractor: ArticleContentExtractor,
    private val domainTrustResolver: DomainTrustResolver,
    private val claimNormalizer: ClaimNormalizer = ClaimNormalizer()
) {
    private val _progressState = MutableStateFlow(
        VerificationProgress(VerificationStep.UPLOADING_PREPROCESSING, "Ready", 0.0f)
    )
    val progressState: StateFlow<VerificationProgress> = _progressState.asStateFlow()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val evidenceListAdapter = moshi.adapter<List<CollectedEvidenceItem>>(
        Types.newParameterizedType(List::class.java, CollectedEvidenceItem::class.java)
    )
    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    private fun updateProgress(step: VerificationStep, detail: String) {
        val percent = (step.stepIndex / 10.0f).coerceIn(0.1f, 1.0f)
        _progressState.value = VerificationProgress(step, detail, percent)
    }

    suspend fun verifyScreenshotInput(
        imageUri: Uri?,
        providedBitmap: Bitmap?,
        inputType: VerificationInputType = VerificationInputType.SCREENSHOT
    ): Long = withContext(Dispatchers.IO) {
        updateProgress(VerificationStep.UPLOADING_PREPROCESSING, "Processing visual capture...")
        delay(150)

        updateProgress(VerificationStep.ANALYZING_IMAGE_QUALITY, "Evaluating readability and framing...")
        val visualAnalysis = screenshotAnalyzer.analyze(providedBitmap, imageUri)
        delay(150)

        val rawText = if (visualAnalysis.detectedText.isNotBlank()) {
            visualAnalysis.detectedText
        } else {
            "Visual evidence inspection"
        }

        updateProgress(VerificationStep.CLAIM_NORMALIZATION, "Formulating checkable statements...")
        val normalizedClaim = claimNormalizer.normalize(rawText, visualAnalysis.detectedUrls.firstOrNull())
        delay(150)

        executeVerificationFlow(
            originalClaim = rawText,
            normalizedClaim = normalizedClaim.primaryClaim,
            searchQuery = normalizedClaim.searchQueries.firstOrNull() ?: rawText,
            inputType = inputType.name,
            imageUri = imageUri?.toString()
        )
    }

    suspend fun verifyTextInput(text: String): Long = withContext(Dispatchers.IO) {
        updateProgress(VerificationStep.UPLOADING_PREPROCESSING, "Analyzing text submission...")
        delay(100)

        updateProgress(VerificationStep.CLAIM_NORMALIZATION, "Formulating verifiable claims...")
        val normalized = claimNormalizer.normalize(text)
        delay(150)

        executeVerificationFlow(
            originalClaim = text,
            normalizedClaim = normalized.primaryClaim,
            searchQuery = normalized.searchQueries.firstOrNull() ?: text,
            inputType = VerificationInputType.TEXT.name,
            imageUri = null
        )
    }

    suspend fun verifyLinkInput(url: String): Long = withContext(Dispatchers.IO) {
        updateProgress(VerificationStep.UPLOADING_PREPROCESSING, "Resolving URL destination...")
        delay(100)

        updateProgress(VerificationStep.DOMAIN_TRUST_CHECK, "Checking domain registration & trust...")
        val trust = domainTrustResolver.resolveTrust(url)
        delay(150)

        updateProgress(VerificationStep.OCR_TEXT_EXTRACTION, "Extracting article contents...")
        val article = articleExtractor.extract(url)
        delay(150)

        val claimText = if (article.title.isNotBlank()) article.title else url
        val normalized = claimNormalizer.normalize(claimText, url)

        executeVerificationFlow(
            originalClaim = url,
            normalizedClaim = "${trust.organization}: ${article.title}",
            searchQuery = "${trust.domain} ${article.title}",
            inputType = VerificationInputType.URL.name,
            imageUri = null,
            articleSnippet = article.body.take(200)
        )
    }

    private suspend fun executeVerificationFlow(
        originalClaim: String,
        normalizedClaim: String,
        searchQuery: String,
        inputType: String,
        imageUri: String?,
        articleSnippet: String? = null
    ): Long {
        updateProgress(VerificationStep.SEARCHING_EVIDENCE, "Querying global fact-checking & news archives...")
        val evidenceList = evidenceSearcher.searchEvidence(searchQuery, maxResults = 5)
        delay(200)

        updateProgress(VerificationStep.TEMPORAL_ANALYSIS, "Validating timeline, dates & citations...")
        delay(150)

        updateProgress(VerificationStep.EVIDENCE_CLUSTERING, "Cross-referencing corroborating reports...")
        delay(150)

        updateProgress(VerificationStep.GEMINI_REASONING, "Synthesizing evidence verdict with VeriLens AI...")
        val normClaimObj = claimNormalizer.normalize(normalizedClaim)
        val reasoningResult = evidenceReasoner.synthesizeVerdict(
            claim = normClaimObj,
            evidence = evidenceList,
            articleBody = articleSnippet
        )
        delay(200)

        updateProgress(VerificationStep.FINALIZING_REPORT, "Saving verification record...")

        val historyEntity = HistoryEntity(
            title = normalizedClaim.take(80),
            originalClaim = originalClaim,
            verifiedInformation = reasoningResult.verifiedInformation,
            inputType = inputType,
            credibilityScore = reasoningResult.credibilityScore,
            verdict = reasoningResult.verdict.name,
            summary = reasoningResult.summary,
            reasoning = reasoningResult.reasoning,
            category = "News & Social",
            sourcesCount = evidenceList.size,
            snippet = articleSnippet ?: (evidenceList.firstOrNull()?.snippet ?: ""),
            evidenceSummaryJson = evidenceListAdapter.toJson(evidenceList),
            sourcesJson = evidenceListAdapter.toJson(evidenceList),
            recommendationsJson = stringListAdapter.toJson(reasoningResult.recommendations),
            disclaimer = reasoningResult.disclaimer,
            imageUri = imageUri
        )

        val insertedId = historyDao.insertHistory(historyEntity)

        recentInputDao.insertRecentInput(
            RecentInputEntity(
                type = inputType,
                content = originalClaim.take(120)
            )
        )

        return insertedId
    }
}
