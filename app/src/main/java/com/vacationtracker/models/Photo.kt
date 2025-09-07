package com.vacationtracker.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Date,
    val placeId: Long? = null,
    val tripId: Long,
    val caption: String? = null
)