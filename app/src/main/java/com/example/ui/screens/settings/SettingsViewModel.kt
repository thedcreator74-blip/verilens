package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.datastore.UserPreferences
import com.example.data.repository.SettingsRepository
import com.example.overlay.OverlayManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val overlayManager: OverlayManager
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    fun setFloatingAssistant(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFloatingAssistant(enabled)
            if (enabled) {
                overlayManager.startOverlay()
            } else {
                overlayManager.stopOverlay()
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }
}
