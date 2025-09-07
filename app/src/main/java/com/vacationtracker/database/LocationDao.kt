package com.vacationtracker.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vacationtracker.models.LocationPoint

@Dao
interface LocationDao {
    
    @Query("SELECT * FROM location_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getLocationsByTrip(tripId: Long): LiveData<List<LocationPoint>>
    
    @Query("SELECT * FROM location_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getLocationsByTripSync(tripId: Long): List<LocationPoint>
    
    @Query("SELECT * FROM location_points WHERE tripId = :tripId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLocationForTrip(tripId: Long): LocationPoint?
    
    @Query("SELECT * FROM location_points ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLocation(): LocationPoint?
    
    @Insert
    suspend fun insertLocation(location: LocationPoint): Long
    
    @Insert
    suspend fun insertLocations(locations: List<LocationPoint>)
    
    @Query("DELETE FROM location_points WHERE tripId = :tripId")
    suspend fun deleteLocationsByTrip(tripId: Long)
    
    @Query("SELECT COUNT(*) FROM location_points WHERE tripId = :tripId")
    suspend fun getLocationCountForTrip(tripId: Long): Int
    
    @Query("""
        SELECT * FROM location_points 
        WHERE tripId = :tripId 
        AND timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp ASC
    """)
    suspend fun getLocationsBetween(tripId: Long, startTime: Long, endTime: Long): List<LocationPoint>
}