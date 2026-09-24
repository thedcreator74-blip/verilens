package com.example.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "verilens_preferences")

data class UserPreferences(
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val language: String = "English",
    val isOverlayEnabled: Boolean = false,
    val isFirstLaunchCompleted: Boolean = false,
    val isOverlayPermissionGranted: Boolean = false,
    val isNotificationPermissionGranted: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val FIRST_LAUNCH_COMPLETED = booleanPreferencesKey("first_launch_completed")
        val OVERLAY_PERMISSION_GRANTED = booleanPreferencesKey("overlay_permission_granted")
        val NOTIFICATION_PERMISSION_GRANTED = booleanPreferencesKey("notification_permission_granted")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
                language = preferences[PreferencesKeys.LANGUAGE] ?: "English",
                isOverlayEnabled = preferences[PreferencesKeys.OVERLAY_ENABLED] ?: false,
                isFirstLaunchCompleted = preferences[PreferencesKeys.FIRST_LAUNCH_COMPLETED] ?: false,
                isOverlayPermissionGranted = preferences[PreferencesKeys.OVERLAY_PERMISSION_GRANTED] ?: false,
                isNotificationPermissionGranted = preferences[PreferencesKeys.NOTIFICATION_PERMISSION_GRANTED] ?: false
            )
        }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = lang
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun setFirstLaunchCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_LAUNCH_COMPLETED] = completed
        }
    }

    suspend fun setOverlayPermissionGranted(granted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERLAY_PERMISSION_GRANTED] = granted
        }
    }

    suspend fun setNotificationPermissionGranted(granted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_PERMISSION_GRANTED] = granted
        }
    }
}
