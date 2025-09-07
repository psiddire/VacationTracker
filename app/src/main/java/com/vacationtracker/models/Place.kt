package com.vacationtracker.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "places")
data class Place(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val placeType: String? = null, // restaurant, hotel, attraction, etc.
    val visitedAt: Date,
    val departedAt: Date? = null,
    val notes: String? = null,
    val rating: Float? = null, // 1-5 stars
    val tripId: Long,
    val photos: List<String> = emptyList() // photo file paths
)