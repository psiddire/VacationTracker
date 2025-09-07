package com.vacationtracker.repositories

import androidx.lifecycle.LiveData
import com.vacationtracker.database.TripDao
import com.vacationtracker.database.LocationDao
import com.vacationtracker.database.PlaceDao
import com.vacationtracker.database.PhotoDao
import com.vacationtracker.models.Trip
import java.io.File
import java.util.Date

class TripRepository(
    private val tripDao: TripDao,
    private val locationDao: LocationDao? = null,
    private val placeDao: PlaceDao? = null,
    private val photoDao: PhotoDao? = null
) {
    
    fun getAllTrips(): LiveData<List<Trip>> = tripDao.getAllTrips()
    
    fun getActiveTripLive(): LiveData<Trip?> = tripDao.getActiveTripLive()
    
    suspend fun getActiveTrip(): Trip? = tripDao.getActiveTrip()
    
    suspend fun getTripById(tripId: Long): Trip? = tripDao.getTripById(tripId)
    
    suspend fun startNewTrip(name: String, description: String? = null): Long {
        // Deactivate any existing active trips
        tripDao.deactivateAllTrips()
        
        val trip = Trip(
            name = name,
            startDate = Date(),
            endDate = null,
            description = description,
            isActive = true
        )
        
        return tripDao.insertTrip(trip)
    }
    
    suspend fun endTrip(tripId: Long) {
        tripDao.endTrip(tripId, System.currentTimeMillis())
    }
    
    suspend fun activateTrip(tripId: Long) {
        tripDao.deactivateAllTrips()
        tripDao.activateTrip(tripId)
    }
    
    suspend fun updateTrip(trip: Trip) {
        tripDao.updateTrip(trip)
    }
    
    suspend fun deleteTrip(trip: Trip) {
        tripDao.deleteTrip(trip)
    }
    
    suspend fun deleteTripCompletely(tripId: Long) {
        // Delete photos and their files
        photoDao?.let { photoDao ->
            val photos = photoDao.getPhotosByTripSync(tripId)
            photos.forEach { photo ->
                // Delete the actual file
                val file = File(photo.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            photoDao.deletePhotosByTrip(tripId)
        }
        
        // Delete places
        placeDao?.deletePlacesByTrip(tripId)
        
        // Delete location points
        locationDao?.deleteLocationsByTrip(tripId)
        
        // Finally delete the trip
        val trip = tripDao.getTripById(tripId)
        trip?.let { tripDao.deleteTrip(it) }
    }
}