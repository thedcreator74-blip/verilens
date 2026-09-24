package com.example.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verification_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val snippet: String,
    val inputType: String, // "SCREENSHOT", "TEXT", "LINK"
    val credibilityScore: Int, // 0 - 100
    val verdict: String, // "VERIFIED", "QUESTIONABLE", "MISLEADING", "INCONCLUSIVE"
    val summary: String,
    val sourcesCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false,
    val originalClaim: String = "",
    val verifiedInformation: String = "",
    val reasoning: String = "",
    val category: String = "GENERAL",
    val evidenceSummaryJson: String = "[]",
    val sourcesJson: String = "[]",
    val recommendationsJson: String = "[]",
    val disclaimer: String = "",
    val imageUri: String? = null
)

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey
    val key: String,
    val value: String
)

@Entity(tableName = "recent_inputs")
data class RecentInputEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "SCREENSHOT", "TEXT", "LINK"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_preferences")
data class AppPreferenceEntity(
    @PrimaryKey
    val key: String,
    val stringValue: String? = null,
    val booleanValue: Boolean? = null,
    val longValue: Long? = null
)
