package com.example.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.example.data.local.database.daos.HistoryDao
import com.example.data.local.database.daos.RecentInputDao
import com.example.data.local.database.entities.HistoryEntity
import com.example.data.local.database.entities.RecentInputEntity
import com.example.feature.verification.model.VerificationInputType
import com.example.feature.verification.model.VerificationProgress
import com.example.feature.verification.pipeline.VerificationPipeline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class VerificationRepository(
    private val historyDao: HistoryDao,
    private val recentInputDao: RecentInputDao,
    private val verificationPipeline: VerificationPipeline
) {
    val verificationProgress: StateFlow<VerificationProgress> = verificationPipeline.progressState

    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    fun getHistoryByIdFlow(id: Long): Flow<HistoryEntity?> = historyDao.getHistoryByIdFlow(id)

    suspend fun getHistoryById(id: Long): HistoryEntity? = historyDao.getHistoryById(id)

    fun getRecentInputs(): Flow<List<RecentInputEntity>> = recentInputDao.getRecentInputs()

    suspend fun verifyScreenshot(uri: Uri?, bitmap: Bitmap?): Long {
        return verificationPipeline.verifyScreenshotInput(uri, bitmap, VerificationInputType.SCREENSHOT)
    }

    suspend fun verifyCameraImage(bitmap: Bitmap): Long {
        return verificationPipeline.verifyScreenshotInput(null, bitmap, VerificationInputType.CAMERA)
    }

    suspend fun verifyText(text: String): Long {
        return verificationPipeline.verifyTextInput(text)
    }

    suspend fun verifyLink(url: String): Long {
        return verificationPipeline.verifyLinkInput(url)
    }

    suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }

    suspend fun deleteHistory(id: Long) {
        historyDao.deleteHistoryById(id)
    }
}
