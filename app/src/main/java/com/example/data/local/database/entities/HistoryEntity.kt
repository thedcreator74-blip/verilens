package com.example.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verification_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val originalClaim: String,
    val verifiedInformation: String,
    val inputType: String, // SCREENSHOT, IMAGE, TEXT, URL, CAMERA
    val timestamp: Long = System.currentTimeMillis(),
    val credibilityScore: Int,
    val verdict: String, // SUPPORTED, CONTRADICTED, MISLEADING, INSUFFICIENT_EVIDENCE, SCAM_RISK
    val summary: String,
    val reasoning: String,
    val category: String = "General",
    val sourcesCount: Int = 0,
    val snippet: String = "",
    val evidenceSummaryJson: String = "[]",
    val sourcesJson: String = "[]",
    val recommendationsJson: String = "[]",
    val disclaimer: String = "",
    val imageUri: String? = null
)
