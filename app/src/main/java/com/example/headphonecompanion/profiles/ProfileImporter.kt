package com.example.headphonecompanion.profiles

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader

/**
 * Simple ProfileImporter that reads a profile JSON from assets and upserts it into Room.
 * Expected JSON keys: vendor, model, measured_by, measurement_date, frequency_response_url,
 * compensation_type, compensation_data, battery_curve (optional)
 */
class ProfileImporter(private val context: Context) {
    private val repo = ProfileRepository(context)

    suspend fun importFromAssets(assetPath: String): HeadphoneProfile? {
        return try {
            val input = context.assets.open(assetPath)
            val reader = BufferedReader(input.reader())
            val text = reader.use { it.readText() }
            val obj = JSONObject(text)
            val profile = HeadphoneProfile(
                model = obj.optString("model", "unknown"),
                vendor = obj.optString("vendor", "unknown"),
                measuredBy = obj.optString("measured_by", null),
                measurementDate = obj.optString("measurement_date", null),
                frequencyResponseUrl = obj.optString("frequency_response_url", null),
                compensationType = obj.optString("compensation_type", null),
                compensationData = obj.optString("compensation_data", null)
            )
            repo.upsert(profile)
            profile
        } catch (e: Exception) {
            Log.e("ProfileImporter", "Failed to import profile $assetPath", e)
            null
        }
    }
}
