package com.vacationtracker.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startDate: Date,
    val endDate: Date?,
    val description: String? = null,
    val isActive: Boolean = false,
    val totalDistance: Double = 0.0, // in meters
    val totalDuration: Long = 0 // in milliseconds
)