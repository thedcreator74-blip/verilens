package com.example.ui.screens.home

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.entities.HistoryEntity
import com.example.data.repository.SettingsRepository
import com.example.data.repository.VerificationRepository
import com.example.feature.verification.model.VerificationProgress
import com.example.overlay.OverlayManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeStats(
    val totalVerified: Int = 0,
    val averageScore: Int = 0,
    val misleadingBlocked: Int = 0
)

class HomeViewModel(
    private val verificationRepository: VerificationRepository,
    private val settingsRepository: SettingsRepository,
    private val overlayManager: OverlayManager
) : ViewModel() {

    val allHistory: StateFlow<List<HistoryEntity>> = verificationRepository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    val verificationProgress: StateFlow<VerificationProgress> = verificationRepository.verificationProgress

    val filteredRecentHistory: StateFlow<List<HistoryEntity>> = combine(
        allHistory,
        _searchQuery
    ) { history, query ->
        if (query.isBlank()) {
            history.take(4)
        } else {
            history.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.snippet.contains(query, ignoreCase = true) ||
                        it.inputType.contains(query, ignoreCase = true)
            }.take(6)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stats: StateFlow<HomeStats> = allHistory.combine(_searchQuery) { list, _ ->
        if (list.isEmpty()) {
            HomeStats(totalVerified = 0, averageScore = 0, misleadingBlocked = 0)
        } else {
            val avg = list.map { it.credibilityScore }.average().toInt()
            val misleading = list.count { it.verdict == "MISLEADING" }
            HomeStats(
                totalVerified = list.size,
                averageScore = avg,
                misleadingBlocked = misleading
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeStats(4, 64, 2)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun isOverlayPermitted(): Boolean = overlayManager.isOverlayPermitted()

    fun verifyText(text: String, onResult: (Long) -> Unit) {
        viewModelScope.launch {
            _isVerifying.value = true
            try {
                val id = verificationRepository.verifyText(text)
                onResult(id)
            } finally {
                _isVerifying.value = false
            }
        }
    }

    fun verifyLink(url: String, onResult: (Long) -> Unit) {
        viewModelScope.launch {
            _isVerifying.value = true
            try {
                val id = verificationRepository.verifyLink(url)
                onResult(id)
            } finally {
                _isVerifying.value = false
            }
        }
    }

    fun verifyScreenshot(label: String = "Active Screen", onResult: (Long) -> Unit) {
        viewModelScope.launch {
            _isVerifying.value = true
            try {
                val id = verificationRepository.verifyScreenshot(label)
                onResult(id)
            } finally {
                _isVerifying.value = false
            }
        }
    }

    fun verifyScreenshotUri(uri: Uri, onResult: (Long) -> Unit) {
        viewModelScope.launch {
            _isVerifying.value = true
            try {
                val id = verificationRepository.verifyScreenshotUri(uri)
                onResult(id)
            } finally {
                _isVerifying.value = false
            }
        }
    }

    fun verifyScreenshotBitmap(bitmap: Bitmap, onResult: (Long) -> Unit) {
        viewModelScope.launch {
            _isVerifying.value = true
            try {
                val id = verificationRepository.verifyScreenshotBitmap(bitmap)
                onResult(id)
            } finally {
                _isVerifying.value = false
            }
        }
    }

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
}
