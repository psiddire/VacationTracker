package com.vacationtracker.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vacationtracker.models.Trip

@Dao
interface TripDao {
    
    @Query("SELECT * FROM trips ORDER BY startDate DESC")
    fun getAllTrips(): LiveData<List<Trip>>
    
    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): Trip?
    
    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTrip(): Trip?
    
    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    fun getActiveTripLive(): LiveData<Trip?>
    
    @Insert
    suspend fun insertTrip(trip: Trip): Long
    
    @Update
    suspend fun updateTrip(trip: Trip)
    
    @Delete
    suspend fun deleteTrip(trip: Trip)
    
    @Query("UPDATE trips SET isActive = 0")
    suspend fun deactivateAllTrips()
    
    @Query("UPDATE trips SET isActive = 1 WHERE id = :tripId")
    suspend fun activateTrip(tripId: Long)
    
    @Query("UPDATE trips SET endDate = :endDate, isActive = 0 WHERE id = :tripId")
    suspend fun endTrip(tripId: Long, endDate: Long)
}