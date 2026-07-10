package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HeadphoneDao {
    @Query("SELECT * FROM headphone_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<HeadphoneSettings?>

    @Query("SELECT * FROM headphone_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): HeadphoneSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: HeadphoneSettings)
}
