package com.example.headphonecompanion.profiles

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "headphone_profiles")
data class HeadphoneProfile(
    @PrimaryKey @ColumnInfo(name = "model") val model: String,
    @ColumnInfo(name = "vendor") val vendor: String,
    @ColumnInfo(name = "measured_by") val measuredBy: String?,
    @ColumnInfo(name = "measurement_date") val measurementDate: String?,
    @ColumnInfo(name = "frequency_response_url") val frequencyResponseUrl: String?,
    @ColumnInfo(name = "compensation_type") val compensationType: String?,
    @ColumnInfo(name = "compensation_data") val compensationData: String?
)
