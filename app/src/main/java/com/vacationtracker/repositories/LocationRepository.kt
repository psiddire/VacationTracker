package com.vacationtracker.repositories

import androidx.lifecycle.LiveData
import com.vacationtracker.database.LocationDao
import com.vacationtracker.models.LocationPoint

class LocationRepository(private val locationDao: LocationDao) {
    
    fun getLocationsByTrip(tripId: Long): LiveData<List<LocationPoint>> = 
        locationDao.getLocationsByTrip(tripId)
    
    suspend fun getLocationsByTripSync(tripId: Long): List<LocationPoint> = 
        locationDao.getLocationsByTripSync(tripId)
    
    suspend fun getLastLocationForTrip(tripId: Long): LocationPoint? = 
        locationDao.getLastLocationForTrip(tripId)
    
    suspend fun getLastLocation(): LocationPoint? = 
        locationDao.getLastLocation()
    
    suspend fun insertLocation(location: LocationPoint): Long = 
        locationDao.insertLocation(location)
    
    suspend fun insertLocations(locations: List<LocationPoint>) = 
        locationDao.insertLocations(locations)
    
    suspend fun deleteLocationsByTrip(tripId: Long) = 
        locationDao.deleteLocationsByTrip(tripId)
    
    suspend fun getLocationCountForTrip(tripId: Long): Int = 
        locationDao.getLocationCountForTrip(tripId)
    
    suspend fun getLocationsBetween(tripId: Long, startTime: Long, endTime: Long): List<LocationPoint> =
        locationDao.getLocationsBetween(tripId, startTime, endTime)
}