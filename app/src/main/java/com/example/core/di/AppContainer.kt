package com.example.core.di

import android.content.Context
import com.example.data.local.database.AppDatabase
import com.example.data.local.datastore.UserPreferencesRepository
import com.example.data.network.VerificationApiService
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SettingsRepositoryImpl
import com.example.data.repository.VerificationRepository
import com.example.data.repository.VerificationRepositoryImpl
import com.example.feature.chat.repository.ChatRepository
import com.example.feature.chat.repository.ChatRepositoryImpl
import com.example.feature.verification.pipeline.VerificationPipeline
import com.example.overlay.OverlayManager
import com.example.permissions.PermissionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val database: AppDatabase
    val userPreferencesRepository: UserPreferencesRepository
    val verificationRepository: VerificationRepository
    val settingsRepository: SettingsRepository
    val permissionManager: PermissionManager
    val overlayManager: OverlayManager
    val verificationApiService: VerificationApiService
    val chatRepository: ChatRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    val verificationPipeline: VerificationPipeline by lazy {
        VerificationPipeline(
            context = context,
            historyDao = database.historyDao(),
            recentInputDao = database.recentInputDao()
        )
    }

    override val verificationRepository: VerificationRepository by lazy {
        VerificationRepositoryImpl(
            historyDao = database.historyDao(),
            recentInputDao = database.recentInputDao(),
            verificationPipeline = verificationPipeline
        )
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(
            preferencesRepository = userPreferencesRepository,
            settingsDao = database.settingsDao()
        )
    }

    override val permissionManager: PermissionManager by lazy {
        PermissionManager(context)
    }

    override val overlayManager: OverlayManager by lazy {
        OverlayManager(context)
    }

    // Prepared Retrofit client (offline / mock base URL for architecture readiness)
    override val verificationApiService: VerificationApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.verilens.ai/") // Placeholder endpoint
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(VerificationApiService::class.java)
    }

    override val chatRepository: ChatRepository by lazy {
        ChatRepositoryImpl(
            verificationRepository = verificationRepository
        )
    }
}
