package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.datastore.UserPreferences
import com.example.data.repository.SettingsRepository
import com.example.data.repository.VerificationRepository
import com.example.overlay.OverlayManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val verificationRepository: VerificationRepository,
    private val overlayManager: OverlayManager
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(lang)
        }
    }

    fun isOverlayPermitted(): Boolean = overlayManager.isOverlayPermitted()

    fun toggleOverlay(enable: Boolean): Boolean {
        return if (enable) {
            val started = overlayManager.startOverlay()
            if (started) {
                viewModelScope.launch {
                    settingsRepository.setOverlayEnabled(true)
                }
            }
            started
        } else {
            overlayManager.stopOverlay()
            viewModelScope.launch {
                settingsRepository.setOverlayEnabled(false)
            }
            true
        }
    }

    fun clearHistory(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            verificationRepository.clearAllHistory()
            onDone()
        }
    }
}
