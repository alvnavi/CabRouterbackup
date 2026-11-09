package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val fare: Double,
    val notes: String = "",
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 0f,
    val addressSnippet: String = "",
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val tripCount: Int = 0
)
