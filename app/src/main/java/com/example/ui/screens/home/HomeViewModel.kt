package com.example.ui.screens.home

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.entities.HistoryEntity
import com.example.data.repository.SettingsRepository
import com.example.data.repository.VerificationRepository
import com.example.overlay.OverlayManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val verificationRepository: VerificationRepository,
    private val settingsRepository: SettingsRepository,
    private val overlayManager: OverlayManager
) : ViewModel() {

    val recentHistory: StateFlow<List<HistoryEntity>> = verificationRepository.getAllHistory().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stats: StateFlow<HomeStats> = recentHistory.map { list ->
        if (list.isEmpty()) {
            HomeStats(0, 0, 0)
        } else {
            val total = list.size
            val misleading = list.count { it.verdict == "CONTRADICTED" || it.verdict == "MISLEADING" || it.verdict == "SCAM_RISK" }
            val avg = list.map { it.credibilityScore }.average().toInt()
            HomeStats(total, misleading, avg)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeStats()
    )

    private val _navigateToReport = MutableSharedFlow<Long>()
    val navigateToReport: SharedFlow<Long> = _navigateToReport.asSharedFlow()

    fun verifyText(text: String) {
        viewModelScope.launch {
            val id = verificationRepository.verifyText(text)
            _navigateToReport.emit(id)
        }
    }

    fun verifyLink(url: String) {
        viewModelScope.launch {
            val id = verificationRepository.verifyLink(url)
            _navigateToReport.emit(id)
        }
    }

    fun verifyScreenshotUri(uri: Uri) {
        viewModelScope.launch {
            val id = verificationRepository.verifyScreenshot(uri, null)
            _navigateToReport.emit(id)
        }
    }

    fun verifyScreenshotBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            val id = verificationRepository.verifyScreenshot(null, bitmap)
            _navigateToReport.emit(id)
        }
    }
}
