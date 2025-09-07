package com.vacationtracker.repositories

import androidx.lifecycle.LiveData
import com.vacationtracker.database.PhotoDao
import com.vacationtracker.models.Photo
import java.util.Date

class PhotoRepository(private val photoDao: PhotoDao) {
    
    fun getPhotosByTrip(tripId: Long): LiveData<List<Photo>> = 
        photoDao.getPhotosByTrip(tripId)
    
    suspend fun getPhotosByTripSync(tripId: Long): List<Photo> = 
        photoDao.getPhotosByTripSync(tripId)
    
    fun getPhotosByPlace(placeId: Long): LiveData<List<Photo>> = 
        photoDao.getPhotosByPlace(placeId)
    
    suspend fun getPhotoById(photoId: Long): Photo? = 
        photoDao.getPhotoById(photoId)
    
    suspend fun addPhoto(
        filePath: String,
        tripId: Long,
        placeId: Long? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        caption: String? = null,
        timestamp: Date = Date()
    ): Long {
        val photo = Photo(
            filePath = filePath,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp,
            placeId = placeId,
            tripId = tripId,
            caption = caption
        )
        return photoDao.insertPhoto(photo)
    }
    
    suspend fun updatePhoto(photo: Photo) = 
        photoDao.updatePhoto(photo)
    
    suspend fun deletePhoto(photo: Photo) = 
        photoDao.deletePhoto(photo)
    
    suspend fun deletePhotosByTrip(tripId: Long) = 
        photoDao.deletePhotosByTrip(tripId)
    
    suspend fun deletePhotosByPlace(placeId: Long) = 
        photoDao.deletePhotosByPlace(placeId)
    
    suspend fun getPhotoCountForTrip(tripId: Long): Int = 
        photoDao.getPhotoCountForTrip(tripId)
    
    suspend fun getPhotoCountForPlace(placeId: Long): Int = 
        photoDao.getPhotoCountForPlace(placeId)
}