package com.example.core.di

import android.content.Context
import com.example.data.local.database.AppDatabase
import com.example.data.local.datastore.UserPreferencesRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.VerificationRepository
import com.example.feature.chat.repository.ChatRepository
import com.example.feature.verification.analysis.ScreenshotAnalyzer
import com.example.feature.verification.article.ArticleContentExtractor
import com.example.feature.verification.article.ArticleContentExtractorImpl
import com.example.feature.verification.capture.ScreenCaptureManager
import com.example.feature.verification.engine.GeminiEvidenceReasoner
import com.example.feature.verification.pipeline.VerificationPipeline
import com.example.feature.verification.search.CompositeEvidenceSearcher
import com.example.feature.verification.search.EvidenceSearcher
import com.example.feature.verification.trust.DomainTrustResolver
import com.example.feature.verification.trust.DomainTrustResolverImpl
import com.example.overlay.OverlayManager
import com.example.permissions.PermissionManager

interface AppContainer {
    val context: Context
    val database: AppDatabase
    val userPreferencesRepository: UserPreferencesRepository
    val settingsRepository: SettingsRepository
    val verificationRepository: VerificationRepository
    val verificationPipeline: VerificationPipeline
    val domainTrustResolver: DomainTrustResolver
    val articleExtractor: ArticleContentExtractor
    val evidenceSearcher: EvidenceSearcher
    val evidenceReasoner: GeminiEvidenceReasoner
    val screenshotAnalyzer: ScreenshotAnalyzer
    val screenCaptureManager: ScreenCaptureManager
    val permissionManager: PermissionManager
    val overlayManager: OverlayManager
    val chatRepository: ChatRepository
}

class DefaultAppContainer(override val context: Context) : AppContainer {
    override val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(userPreferencesRepository)
    }

    override val domainTrustResolver: DomainTrustResolver by lazy {
        DomainTrustResolverImpl()
    }

    override val articleExtractor: ArticleContentExtractor by lazy {
        ArticleContentExtractorImpl()
    }

    override val evidenceSearcher: EvidenceSearcher by lazy {
        CompositeEvidenceSearcher(domainTrustResolver = domainTrustResolver)
    }

    override val evidenceReasoner: GeminiEvidenceReasoner by lazy {
        GeminiEvidenceReasoner()
    }

    override val screenshotAnalyzer: ScreenshotAnalyzer by lazy {
        ScreenshotAnalyzer(context)
    }

    override val screenCaptureManager: ScreenCaptureManager by lazy {
        ScreenCaptureManager(context)
    }

    override val permissionManager: PermissionManager by lazy {
        PermissionManager(context)
    }

    override val overlayManager: OverlayManager by lazy {
        OverlayManager(context)
    }

    override val chatRepository: ChatRepository by lazy {
        ChatRepository()
    }

    override val verificationPipeline: VerificationPipeline by lazy {
        VerificationPipeline(
            context = context,
            historyDao = database.historyDao(),
            recentInputDao = database.recentInputDao(),
            screenshotAnalyzer = screenshotAnalyzer,
            evidenceSearcher = evidenceSearcher,
            evidenceReasoner = evidenceReasoner,
            articleExtractor = articleExtractor,
            domainTrustResolver = domainTrustResolver
        )
    }

    override val verificationRepository: VerificationRepository by lazy {
        VerificationRepository(
            historyDao = database.historyDao(),
            recentInputDao = database.recentInputDao(),
            verificationPipeline = verificationPipeline
        )
    }
}
