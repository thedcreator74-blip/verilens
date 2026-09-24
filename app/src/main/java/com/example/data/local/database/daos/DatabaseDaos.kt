package com.example.data.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.database.entities.AppPreferenceEntity
import com.example.data.local.database.entities.HistoryEntity
import com.example.data.local.database.entities.RecentInputEntity
import com.example.data.local.database.entities.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM verification_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM verification_history WHERE id = :id LIMIT 1")
    suspend fun getHistoryById(id: Long): HistoryEntity?

    @Query("SELECT * FROM verification_history WHERE inputType = :type ORDER BY timestamp DESC")
    fun getHistoryByType(type: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM verification_history WHERE title LIKE '%' || :query || '%' OR snippet LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HistoryEntity>)

    @Update
    suspend fun update(item: HistoryEntity)

    @Query("DELETE FROM verification_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM verification_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM verification_history")
    fun getCount(): Flow<Int>
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: SettingsEntity)

    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<SettingsEntity>>
}

@Dao
interface RecentInputDao {
    @Query("SELECT * FROM recent_inputs ORDER BY timestamp DESC LIMIT 20")
    fun getRecentInputs(): Flow<List<RecentInputEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInput(input: RecentInputEntity): Long

    @Query("DELETE FROM recent_inputs")
    suspend fun clearRecentInputs()
}

@Dao
interface AppPreferenceDao {
    @Query("SELECT * FROM app_preferences WHERE `key` = :key LIMIT 1")
    suspend fun getPreference(key: String): AppPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(pref: AppPreferenceEntity)

    @Query("DELETE FROM app_preferences WHERE `key` = :key")
    suspend fun deletePreference(key: String)
}
