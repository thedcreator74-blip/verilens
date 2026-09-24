package com.example.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_inputs")
data class RecentInputEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
