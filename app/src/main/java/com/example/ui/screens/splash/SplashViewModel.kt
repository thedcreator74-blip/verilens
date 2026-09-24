package com.example.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SplashDestination {
    data object Loading : SplashDestination
    data object Onboarding : SplashDestination
    data object Home : SplashDestination
}

class SplashViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        determineStartDestination()
    }

    private fun determineStartDestination() {
        viewModelScope.launch {
            // Elegant brand splash delay (1400ms)
            delay(1400)
            val preferences = settingsRepository.userPreferences.first()
            if (preferences.isFirstLaunchCompleted) {
                _destination.value = SplashDestination.Home
            } else {
                _destination.value = SplashDestination.Onboarding
            }
        }
    }
}
