package com.example.data.local.datastore

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@JsonClass(generateAdapter = true)
data class UserPreferences(
    val hasCompletedOnboarding: Boolean = false,
    val isFloatingAssistantEnabled: Boolean = false,
    val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
    val language: String = "en",
    val autoScanClipboard: Boolean = true,
    val showDisclaimer: Boolean = true
)

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("verilens_user_prefs", Context.MODE_PRIVATE)

    private val _userPreferencesFlow = MutableStateFlow(loadPreferences())
    val userPreferences: Flow<UserPreferences> = _userPreferencesFlow.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        return UserPreferences(
            hasCompletedOnboarding = prefs.getBoolean("has_completed_onboarding", false),
            isFloatingAssistantEnabled = prefs.getBoolean("is_floating_assistant_enabled", false),
            themeMode = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM",
            language = prefs.getString("language", "en") ?: "en",
            autoScanClipboard = prefs.getBoolean("auto_scan_clipboard", true),
            showDisclaimer = prefs.getBoolean("show_disclaimer", true)
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("has_completed_onboarding", completed).apply()
        _userPreferencesFlow.value = loadPreferences()
    }

    suspend fun setFloatingAssistantEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_floating_assistant_enabled", enabled).apply()
        _userPreferencesFlow.value = loadPreferences()
    }

    suspend fun setThemeMode(theme: String) {
        prefs.edit().putString("theme_mode", theme).apply()
        _userPreferencesFlow.value = loadPreferences()
    }

    suspend fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _userPreferencesFlow.value = loadPreferences()
    }
}
