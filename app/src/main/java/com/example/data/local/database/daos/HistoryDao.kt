package com.example.data.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.database.entities.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM verification_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM verification_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): HistoryEntity?

    @Query("SELECT * FROM verification_history WHERE id = :id")
    fun getHistoryByIdFlow(id: Long): Flow<HistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @Update
    suspend fun updateHistory(history: HistoryEntity)

    @Query("DELETE FROM verification_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM verification_history")
    suspend fun clearAllHistory()
}
