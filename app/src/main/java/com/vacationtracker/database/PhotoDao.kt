package com.vacationtracker.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vacationtracker.models.Photo

@Dao
interface PhotoDao {
    
    @Query("SELECT * FROM photos WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getPhotosByTrip(tripId: Long): LiveData<List<Photo>>
    
    @Query("SELECT * FROM photos WHERE tripId = :tripId ORDER BY timestamp DESC")
    suspend fun getPhotosByTripSync(tripId: Long): List<Photo>
    
    @Query("SELECT * FROM photos WHERE placeId = :placeId ORDER BY timestamp DESC")
    fun getPhotosByPlace(placeId: Long): LiveData<List<Photo>>
    
    @Query("SELECT * FROM photos WHERE id = :photoId")
    suspend fun getPhotoById(photoId: Long): Photo?
    
    @Insert
    suspend fun insertPhoto(photo: Photo): Long
    
    @Update
    suspend fun updatePhoto(photo: Photo)
    
    @Delete
    suspend fun deletePhoto(photo: Photo)
    
    @Query("DELETE FROM photos WHERE tripId = :tripId")
    suspend fun deletePhotosByTrip(tripId: Long)
    
    @Query("DELETE FROM photos WHERE placeId = :placeId")
    suspend fun deletePhotosByPlace(placeId: Long)
    
    @Query("SELECT COUNT(*) FROM photos WHERE tripId = :tripId")
    suspend fun getPhotoCountForTrip(tripId: Long): Int
    
    @Query("SELECT COUNT(*) FROM photos WHERE placeId = :placeId")
    suspend fun getPhotoCountForPlace(placeId: Long): Int
}