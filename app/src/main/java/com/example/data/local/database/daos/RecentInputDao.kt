package com.example.data.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.database.entities.RecentInputEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentInputDao {
    @Query("SELECT * FROM recent_inputs ORDER BY timestamp DESC LIMIT 20")
    fun getRecentInputs(): Flow<List<RecentInputEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentInput(input: RecentInputEntity): Long

    @Query("DELETE FROM recent_inputs")
    suspend fun clearRecentInputs()
}
