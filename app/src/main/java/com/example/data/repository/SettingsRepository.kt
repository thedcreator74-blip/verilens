package com.example.data.repository

import com.example.data.local.datastore.UserPreferences
import com.example.data.local.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val userPreferencesRepository: UserPreferencesRepository) {
    val userPreferences: Flow<UserPreferences> = userPreferencesRepository.userPreferences

    suspend fun completeOnboarding() {
        userPreferencesRepository.setOnboardingCompleted(true)
    }

    suspend fun setFloatingAssistant(enabled: Boolean) {
        userPreferencesRepository.setFloatingAssistantEnabled(enabled)
    }

    suspend fun setThemeMode(mode: String) {
        userPreferencesRepository.setThemeMode(mode)
    }

    suspend fun setLanguage(language: String) {
        userPreferencesRepository.setLanguage(language)
    }
}
