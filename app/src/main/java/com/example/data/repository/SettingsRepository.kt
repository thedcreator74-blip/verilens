package com.example.data.repository

import com.example.data.local.database.daos.SettingsDao
import com.example.data.local.database.entities.SettingsEntity
import com.example.data.local.datastore.UserPreferences
import com.example.data.local.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun setThemeMode(mode: String)
    suspend fun setLanguage(language: String)
    suspend fun setOverlayEnabled(enabled: Boolean)
    suspend fun setFirstLaunchCompleted(completed: Boolean)
    suspend fun setCustomSetting(key: String, value: String)
    suspend fun getCustomSetting(key: String): String?
}

class SettingsRepositoryImpl(
    private val preferencesRepository: UserPreferencesRepository,
    private val settingsDao: SettingsDao
) : SettingsRepository {

    override val userPreferences: Flow<UserPreferences> = preferencesRepository.userPreferencesFlow

    override suspend fun setThemeMode(mode: String) {
        preferencesRepository.setThemeMode(mode)
    }

    override suspend fun setLanguage(language: String) {
        preferencesRepository.setLanguage(language)
    }

    override suspend fun setOverlayEnabled(enabled: Boolean) {
        preferencesRepository.setOverlayEnabled(enabled)
    }

    override suspend fun setFirstLaunchCompleted(completed: Boolean) {
        preferencesRepository.setFirstLaunchCompleted(completed)
    }

    override suspend fun setCustomSetting(key: String, value: String) {
        settingsDao.setSetting(SettingsEntity(key = key, value = value))
    }

    override suspend fun getCustomSetting(key: String): String? {
        return settingsDao.getSetting(key)?.value
    }
}
