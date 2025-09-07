package com.vacationtracker.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vacationtracker.models.Place

@Dao
interface PlaceDao {
    
    @Query("SELECT * FROM places WHERE tripId = :tripId ORDER BY visitedAt DESC")
    fun getPlacesByTrip(tripId: Long): LiveData<List<Place>>
    
    @Query("SELECT * FROM places WHERE tripId = :tripId ORDER BY visitedAt DESC")
    suspend fun getPlacesByTripSync(tripId: Long): List<Place>
    
    @Query("SELECT * FROM places WHERE id = :placeId")
    suspend fun getPlaceById(placeId: Long): Place?
    
    @Insert
    suspend fun insertPlace(place: Place): Long
    
    @Update
    suspend fun updatePlace(place: Place)
    
    @Delete
    suspend fun deletePlace(place: Place)
    
    @Query("DELETE FROM places WHERE tripId = :tripId")
    suspend fun deletePlacesByTrip(tripId: Long)
    
    @Query("SELECT COUNT(*) FROM places WHERE tripId = :tripId")
    suspend fun getPlaceCountForTrip(tripId: Long): Int
    
    @Query("""
        SELECT * FROM places 
        WHERE latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLng AND :maxLng
        AND tripId = :tripId
    """)
    suspend fun getPlacesInBounds(
        tripId: Long,
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): List<Place>
}