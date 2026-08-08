package com.example.headphonecompanion.profiles

import android.content.Context

class ProfileRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.profileDao()

    suspend fun getAllProfiles(): List<HeadphoneProfile> = dao.getAll()
    suspend fun getProfile(model: String): HeadphoneProfile? = dao.getByModel(model)
    suspend fun upsert(profile: HeadphoneProfile) = dao.insert(profile)
}
