package com.vacationtracker.repositories

import androidx.lifecycle.LiveData
import com.vacationtracker.database.PlaceDao
import com.vacationtracker.models.Place
import java.util.Date

class PlaceRepository(private val placeDao: PlaceDao) {
    
    fun getPlacesByTrip(tripId: Long): LiveData<List<Place>> = 
        placeDao.getPlacesByTrip(tripId)
    
    suspend fun getPlacesByTripSync(tripId: Long): List<Place> = 
        placeDao.getPlacesByTripSync(tripId)
    
    suspend fun getPlaceById(placeId: Long): Place? = 
        placeDao.getPlaceById(placeId)
    
    suspend fun addPlace(
        name: String,
        latitude: Double,
        longitude: Double,
        tripId: Long,
        address: String? = null,
        placeType: String? = null,
        notes: String? = null
    ): Long {
        val place = Place(
            name = name,
            latitude = latitude,
            longitude = longitude,
            address = address,
            placeType = placeType,
            visitedAt = Date(),
            notes = notes,
            tripId = tripId
        )
        return placeDao.insertPlace(place)
    }
    
    suspend fun updatePlace(place: Place) = 
        placeDao.updatePlace(place)
    
    suspend fun deletePlace(place: Place) = 
        placeDao.deletePlace(place)
    
    suspend fun deletePlacesByTrip(tripId: Long) = 
        placeDao.deletePlacesByTrip(tripId)
    
    suspend fun getPlaceCountForTrip(tripId: Long): Int = 
        placeDao.getPlaceCountForTrip(tripId)
    
    suspend fun getPlacesInBounds(
        tripId: Long,
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): List<Place> = placeDao.getPlacesInBounds(tripId, minLat, maxLat, minLng, maxLng)
}