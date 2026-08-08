package com.example.headphonecompanion.profiles

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProfileDao {
    @Query("SELECT * FROM headphone_profiles")
    suspend fun getAll(): List<HeadphoneProfile>

    @Query("SELECT * FROM headphone_profiles WHERE model = :model LIMIT 1")
    suspend fun getByModel(model: String): HeadphoneProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: HeadphoneProfile)
}
