package com.example.feature.verification.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.local.database.daos.HistoryDao
import com.example.data.local.database.daos.RecentInputDao
import com.example.data.local.database.entities.HistoryEntity
import com.example.data.local.database.entities.RecentInputEntity
import com.example.feature.verification.analysis.ScreenshotAnalyzer
import com.example.feature.verification.claim.ClaimExtractor
import com.example.feature.verification.engine.GeminiEvidenceReasoner
import com.example.feature.verification.model.CollectedEvidenceItem
import com.example.feature.verification.model.ExtractedVisualAnalysis
import com.example.feature.verification.model.VerificationProgress
import com.example.feature.verification.model.VerificationStep
import com.example.feature.verification.search.InternetEvidenceSearcher
import com.example.feature.verification.strategy.VerificationStrategyEngine
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
    private val screenshotAnalyzer: ScreenshotAnalyzer = ScreenshotAnalyzer(context),
    private val evidenceSearcher: InternetEvidenceSearcher = InternetEvidenceSearcher(),
    private val evidenceReasoner: GeminiEvidenceReasoner = GeminiEvidenceReasoner()
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listStringType = Types.newParameterizedType(List::class.java, String::class.java)
    private val listEvidenceType = Types.newParameterizedType(List::class.java, CollectedEvidenceItem::class.java)

    private val stringListAdapter = moshi.adapter<List<String>>(listStringType)
    private val evidenceListAdapter = moshi.adapter<List<CollectedEvidenceItem>>(listEvidenceType)

    private val _progressState = MutableStateFlow(VerificationProgress(VerificationStep.UPLOADING_PREPROCESSING, "Idle", 0.0f))
    val progressState: StateFlow<VerificationProgress> = _progressState.asStateFlow()

    private fun updateProgress(step: VerificationStep, detail: String) {
        val percent = (step.stepIndex.coerceAtLeast(1).toFloat() / 10f).coerceIn(0.1f, 1.0f)
        _progressState.value = VerificationProgress(step, detail, percent)
    }

    /**
     * Complete evidence-based verification for an uploaded screenshot (Uri or Bitmap).
     */
    suspend fun verifyScreenshotInput(
        imageUri: Uri? = null,
        providedBitmap: Bitmap? = null
    ): Long = withContext(Dispatchers.IO) {
        try {
            // Step 1: Uploading & Preprocessing
            updateProgress(VerificationStep.UPLOADING_PREPROCESSING, "Standardizing image resolution & preparing visual tensors...")
            val bitmap = providedBitmap ?: imageUri?.let { screenshotAnalyzer.loadBitmapFromUri(it) }
                ?: throw IllegalArgumentException("Could not decode screenshot image.")
            val (scaledBitmap, jpegBytes) = screenshotAnalyzer.preprocessBitmap(bitmap)
            delay(150)

            // Step 2: Extracting Text (OCR)
            updateProgress(VerificationStep.EXTRACTING_TEXT, "Scanning text lines and typographic structure on-device...")
            val ocrText = screenshotAnalyzer.extractOcrText(scaledBitmap)
            delay(150)

            // Step 3: Analyzing Screenshot (Gemini Vision / Visual Audit)
            updateProgress(VerificationStep.ANALYZING_SCREENSHOT, "Auditing layout, logos, digital compression & manipulation signs...")
            val visualAnalysis = screenshotAnalyzer.analyzeWithGeminiVision(jpegBytes, ocrText)
            delay(150)

            // Step 4: Identifying Primary Claim
            updateProgress(VerificationStep.IDENTIFYING_CLAIM, "Removing UI noise, status bars & isolating primary proposition...")
            val primaryClaim = ClaimExtractor.extractPrimaryClaim(
                rawOcr = ocrText,
                visualHeadline = visualAnalysis.headline,
                visualClaim = visualAnalysis.primaryClaim
            )
            delay(150)

            // Record recent input
            recentInputDao.insertInput(RecentInputEntity(type = "SCREENSHOT", content = primaryClaim))

            // Execute common verification for this claim
            return@withContext executeVerificationForClaim(
                primaryClaim = primaryClaim,
                inputType = "SCREENSHOT",
                visualAnalysis = visualAnalysis,
                imageUriString = imageUri?.toString()
            )
        } catch (e: Exception) {
            updateProgress(VerificationStep.FAILED, "Verification failed: ${e.message ?: "Unknown error"}")
            throw e
        }
    }

    /**
     * Complete evidence-based verification for text input.
     */
    suspend fun verifyTextInput(text: String): Long = withContext(Dispatchers.IO) {
        try {
            updateProgress(VerificationStep.UPLOADING_PREPROCESSING, "Parsing input claim...")
            delay(100)

            updateProgress(VerificationStep.IDENTIFYING_CLAIM, "Isolating core factual statement...")
            val primaryClaim = ClaimExtractor.extractPrimaryClaim(
                rawOcr = text,
                visualHeadline = "",
                visualClaim = ""
            )
            delay(150)

            recentInputDao.insertInput(RecentInputEntity(type = "TEXT", content = primaryClaim))

            return@withContext executeVerificationForClaim(
                primaryClaim = primaryClaim,
                inputType = "TEXT",
                visualAnalysis = null,
                imageUriString = null
            )
        } catch (e: Exception) {
            updateProgress(VerificationStep.FAILED, "Verification failed: ${e.message ?: "Unknown error"}")
            throw e
        }
    }

    /**
     * Complete evidence-based verification for web link input.
     */
    suspend fun verifyLinkInput(url: String): Long = withContext(Dispatchers.IO) {
        try {
            updateProgress(VerificationStep.UPLOADING_PREPROCESSING, "Inspecting host domain and URL structure...")
            delay(100)

            val cleanHost = url.removePrefix("https://").removePrefix("http://").substringBefore("/")
            val linkClaim = "Inspection of source domain $cleanHost and referenced web resource: $url"

            recentInputDao.insertInput(RecentInputEntity(type = "LINK", content = url))

            return@withContext executeVerificationForClaim(
                primaryClaim = linkClaim,
                inputType = "LINK",
                visualAnalysis = null,
                imageUriString = null
            )
        } catch (e: Exception) {
            updateProgress(VerificationStep.FAILED, "Verification failed: ${e.message ?: "Unknown error"}")
            throw e
        }
    }

    /**
     * Core verification pipeline executed once claim is isolated.
     */
    private suspend fun executeVerificationForClaim(
        primaryClaim: String,
        inputType: String,
        visualAnalysis: ExtractedVisualAnalysis?,
        imageUriString: String?
    ): Long {
        // Step 5: Detecting Category
        updateProgress(VerificationStep.DETECTING_CATEGORY, "Determining claim domain and verification strategy...")
        val category = VerificationStrategyEngine.detectCategory(primaryClaim)
        delay(150)

        // Step 6: Searching Trusted Sources
        updateProgress(VerificationStep.SEARCHING_TRUSTED_SOURCES, "Querying official registries, databases & wire archives for ${category.displayName}...")
        val evidenceList = evidenceSearcher.searchAndCollectEvidence(primaryClaim, category)
        delay(150)

        // Step 7: Collecting Evidence
        updateProgress(VerificationStep.COLLECTING_EVIDENCE, "Gathering verified citations, publication dates & excerpts (${evidenceList.size} records found)...")
        delay(150)

        // Step 8: Comparing Evidence
        updateProgress(VerificationStep.COMPARING_EVIDENCE, "Corroborating factual assertions against collected evidence...")
        delay(150)

        // Step 9: Generating Report via Gemini Evidence Reasoner
        updateProgress(VerificationStep.GENERATING_REPORT, "Synthesizing transparent, evidence-backed verification report...")
        val reportPayload = evidenceReasoner.synthesizeEvidenceReport(
            claim = primaryClaim,
            category = category.name,
            collectedEvidence = evidenceList,
            visualAnalysis = visualAnalysis
        )
        delay(150)

        // Save to Room database
        val titleText = if (primaryClaim.length > 55) primaryClaim.take(52) + "..." else primaryClaim
        val entity = HistoryEntity(
            title = titleText,
            snippet = primaryClaim,
            inputType = inputType,
            credibilityScore = reportPayload.confidence,
            verdict = reportPayload.verdict,
            summary = reportPayload.assessment,
            sourcesCount = evidenceList.size,
            timestamp = System.currentTimeMillis(),
            isBookmarked = false,
            originalClaim = primaryClaim,
            verifiedInformation = reportPayload.verifiedInformation,
            reasoning = reportPayload.reasoning,
            category = category.name,
            evidenceSummaryJson = stringListAdapter.toJson(reportPayload.evidenceSummary),
            sourcesJson = evidenceListAdapter.toJson(evidenceList),
            recommendationsJson = stringListAdapter.toJson(reportPayload.recommendations),
            disclaimer = reportPayload.disclaimer,
            imageUri = imageUriString
        )

        val generatedId = historyDao.insert(entity)

        // Step 10: Completed
        updateProgress(VerificationStep.COMPLETED, "Verification complete. Rendering report.")
        delay(100)

        return generatedId
    }
}
