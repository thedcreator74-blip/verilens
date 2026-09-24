package com.example.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.example.data.local.database.daos.HistoryDao
import com.example.data.local.database.daos.RecentInputDao
import com.example.data.local.database.entities.HistoryEntity
import com.example.data.local.database.entities.RecentInputEntity
import com.example.feature.verification.model.VerificationProgress
import com.example.feature.verification.pipeline.VerificationPipeline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VerificationRepository {
    val allHistory: Flow<List<HistoryEntity>>
    val historyCount: Flow<Int>
    val recentInputs: Flow<List<RecentInputEntity>>
    val verificationProgress: StateFlow<VerificationProgress>

    fun getHistoryByType(type: String): Flow<List<HistoryEntity>>
    fun searchHistory(query: String): Flow<List<HistoryEntity>>
    suspend fun getHistoryById(id: Long): HistoryEntity?
    suspend fun insertHistory(item: HistoryEntity): Long
    suspend fun deleteHistory(id: Long)
    suspend fun clearAllHistory()
    suspend fun recordRecentInput(type: String, content: String)
    suspend fun verifyText(text: String): Long
    suspend fun verifyLink(url: String): Long
    suspend fun verifyScreenshot(label: String): Long
    suspend fun verifyScreenshotUri(uri: Uri): Long
    suspend fun verifyScreenshotBitmap(bitmap: Bitmap): Long
}

class VerificationRepositoryImpl(
    private val historyDao: HistoryDao,
    private val recentInputDao: RecentInputDao,
    private val verificationPipeline: VerificationPipeline
) : VerificationRepository {

    override val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    override val historyCount: Flow<Int> = historyDao.getCount()
    override val recentInputs: Flow<List<RecentInputEntity>> = recentInputDao.getRecentInputs()
    override val verificationProgress: StateFlow<VerificationProgress> = verificationPipeline.progressState

    override fun getHistoryByType(type: String): Flow<List<HistoryEntity>> {
        return historyDao.getHistoryByType(type)
    }

    override fun searchHistory(query: String): Flow<List<HistoryEntity>> {
        return historyDao.searchHistory(query)
    }

    override suspend fun getHistoryById(id: Long): HistoryEntity? {
        return historyDao.getHistoryById(id)
    }

    override suspend fun insertHistory(item: HistoryEntity): Long {
        return historyDao.insert(item)
    }

    override suspend fun deleteHistory(id: Long) {
        historyDao.deleteById(id)
    }

    override suspend fun clearAllHistory() {
        historyDao.clearAll()
        recentInputDao.clearRecentInputs()
    }

    override suspend fun recordRecentInput(type: String, content: String) {
        recentInputDao.insertInput(
            RecentInputEntity(
                type = type,
                content = content
            )
        )
    }

    override suspend fun verifyText(text: String): Long {
        return verificationPipeline.verifyTextInput(text)
    }

    override suspend fun verifyLink(url: String): Long {
        return verificationPipeline.verifyLinkInput(url)
    }

    override suspend fun verifyScreenshot(label: String): Long {
        return verificationPipeline.verifyTextInput(label)
    }

    override suspend fun verifyScreenshotUri(uri: Uri): Long {
        return verificationPipeline.verifyScreenshotInput(imageUri = uri)
    }

    override suspend fun verifyScreenshotBitmap(bitmap: Bitmap): Long {
        return verificationPipeline.verifyScreenshotInput(providedBitmap = bitmap)
    }
}
