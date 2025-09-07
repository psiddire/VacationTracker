package com.vacationtracker.utils

import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

data class PhotoLocationData(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Date,
    val hasLocation: Boolean
)

object PhotoLocationUtils {
    
    private const val PROXIMITY_THRESHOLD_METERS = 200.0 // 200 meters
    
    /**
     * Extract GPS coordinates and timestamp from photo EXIF data
     */
    fun extractPhotoLocationData(context: Context, photoUri: Uri): PhotoLocationData? {
        return try {
            context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                
                // Extract GPS coordinates
                val latLong = FloatArray(2)
                val hasGps = exif.getLatLong(latLong)
                
                // Extract timestamp
                val dateTimeString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                
                val timestamp = parsePhotoTimestamp(dateTimeString) ?: Date()
                
                if (hasGps) {
                    PhotoLocationData(
                        latitude = latLong[0].toDouble(),
                        longitude = latLong[1].toDouble(),
                        timestamp = timestamp,
                        hasLocation = true
                    )
                } else {
                    PhotoLocationData(
                        latitude = 0.0,
                        longitude = 0.0,
                        timestamp = timestamp,
                        hasLocation = false
                    )
                }
            }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }
    
    /**
     * Parse photo timestamp from EXIF data
     */
    private fun parsePhotoTimestamp(dateTimeString: String?): Date? {
        if (dateTimeString.isNullOrBlank()) return null
        
        return try {
            val formats = listOf(
                "yyyy:MM:dd HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy:MM:dd"
            )
            
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    return sdf.parse(dateTimeString)
                } catch (e: Exception) {
                    continue
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if photo location is within proximity of trip route or places
     */
    fun isPhotoNearTripLocation(
        photoLocation: PhotoLocationData,
        tripLocations: List<com.vacationtracker.models.LocationPoint>,
        tripPlaces: List<com.vacationtracker.models.Place>
    ): Boolean {
        if (!photoLocation.hasLocation) return false
        
        val photoLatLng = Location("photo").apply {
            latitude = photoLocation.latitude
            longitude = photoLocation.longitude
        }
        
        // Check proximity to trip route points
        tripLocations.forEach { point ->
            val routeLocation = Location("route").apply {
                latitude = point.latitude
                longitude = point.longitude
            }
            
            if (photoLatLng.distanceTo(routeLocation) <= PROXIMITY_THRESHOLD_METERS) {
                return true
            }
        }
        
        // Check proximity to visited places
        tripPlaces.forEach { place ->
            val placeLocation = Location("place").apply {
                latitude = place.latitude
                longitude = place.longitude
            }
            
            if (photoLatLng.distanceTo(placeLocation) <= PROXIMITY_THRESHOLD_METERS) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Find the closest place to the photo location
     */
    fun findClosestPlace(
        photoLocation: PhotoLocationData,
        tripPlaces: List<com.vacationtracker.models.Place>
    ): com.vacationtracker.models.Place? {
        if (!photoLocation.hasLocation || tripPlaces.isEmpty()) return null
        
        val photoLatLng = Location("photo").apply {
            latitude = photoLocation.latitude
            longitude = photoLocation.longitude
        }
        
        return tripPlaces.minByOrNull { place ->
            val placeLocation = Location("place").apply {
                latitude = place.latitude
                longitude = place.longitude
            }
            photoLatLng.distanceTo(placeLocation)
        }?.let { closestPlace ->
            val placeLocation = Location("place").apply {
                latitude = closestPlace.latitude
                longitude = closestPlace.longitude
            }
            
            // Only return if within threshold
            if (photoLatLng.distanceTo(placeLocation) <= PROXIMITY_THRESHOLD_METERS) {
                closestPlace
            } else {
                null
            }
        }
    }
    
    /**
     * Check if photo was taken during trip timeframe
     */
    fun isPhotoTakenDuringTrip(
        photoTimestamp: Date,
        tripStartDate: Date,
        tripEndDate: Date?
    ): Boolean {
        val endDate = tripEndDate ?: Date() // Use current time if trip is ongoing
        return photoTimestamp.after(tripStartDate) && photoTimestamp.before(endDate)
    }
}