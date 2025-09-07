package com.vacationtracker.viewmodels

import androidx.lifecycle.*
import com.vacationtracker.models.Trip
import com.vacationtracker.repositories.LocationRepository
import com.vacationtracker.repositories.PhotoRepository
import com.vacationtracker.repositories.PlaceRepository
import com.vacationtracker.repositories.TripRepository
import kotlinx.coroutines.launch

class TripHistoryViewModel(
    private val tripRepository: TripRepository,
    private val locationRepository: LocationRepository,
    private val placeRepository: PlaceRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {
    
    val allTrips: LiveData<List<Trip>> = tripRepository.getAllTrips()
    
    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            // Delete photos and their files
            val photos = photoRepository.getPhotosByTripSync(tripId)
            photos.forEach { photo ->
                // Delete the actual file
                val file = java.io.File(photo.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            photoRepository.deletePhotosByTrip(tripId)
            
            // Delete places
            placeRepository.deletePlacesByTrip(tripId)
            
            // Delete location points  
            locationRepository.deleteLocationsByTrip(tripId)
            
            // Finally delete the trip
            val trip = tripRepository.getTripById(tripId)
            trip?.let { tripRepository.deleteTrip(it) }
        }
    }
    
    fun renameTrip(tripId: Long, newName: String) {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId)
            trip?.let {
                val updatedTrip = it.copy(name = newName.trim())
                tripRepository.updateTrip(updatedTrip)
            }
        }
    }
    
    suspend fun generateTripSummary(tripId: Long): String? {
        val trip = tripRepository.getTripById(tripId) ?: return null
        val places = placeRepository.getPlacesByTripSync(tripId)
        val locationCount = locationRepository.getLocationCountForTrip(tripId)
        val photoCount = photoRepository.getPhotoCountForTrip(tripId)
        
        val dateFormatter = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
        
        return buildString {
            appendLine("🗺️ Trip Summary: ${trip.name}")
            appendLine("=" .repeat(30))
            appendLine()
            
            // Trip dates
            appendLine("📅 Started: ${dateFormatter.format(trip.startDate)}")
            if (trip.endDate != null) {
                appendLine("🏁 Ended: ${dateFormatter.format(trip.endDate)}")
                
                // Duration calculation
                val durationMillis = trip.endDate.time - trip.startDate.time
                val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(durationMillis)
                val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(durationMillis) % 24
                val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
                
                if (days > 0) {
                    appendLine("⏱️ Duration: ${days}d ${hours}h ${minutes}m")
                } else {
                    appendLine("⏱️ Duration: ${hours}h ${minutes}m")
                }
            } else {
                appendLine("⏱️ Status: Active trip")
            }
            
            // Trip stats
            appendLine()
            appendLine("📊 Trip Statistics:")
            appendLine("🚗 Distance: ${String.format("%.1f", trip.totalDistance / 1000)} km")
            appendLine("📍 Places visited: ${places.size}")
            appendLine("📸 Photos taken: $photoCount")
            appendLine("🗺️ Location points recorded: $locationCount")
            
            // Description
            if (!trip.description.isNullOrBlank()) {
                appendLine()
                appendLine("📝 Description:")
                appendLine(trip.description)
            }
            
            // Places visited
            if (places.isNotEmpty()) {
                appendLine()
                appendLine("📍 Places Visited:")
                places.forEachIndexed { index, place ->
                    val placeDate = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(place.visitedAt)
                    appendLine("${index + 1}. ${place.name}")
                    if (!place.address.isNullOrBlank()) {
                        appendLine("   📍 ${place.address}")
                    }
                    appendLine("   🕐 Visited: $placeDate")
                    if (!place.notes.isNullOrBlank()) {
                        appendLine("   📝 ${place.notes}")
                    }
                    appendLine()
                }
            }
            
            appendLine()
            appendLine("Generated by Vacation Tracker")
        }
    }
    
    class Factory(
        private val tripRepository: TripRepository,
        private val locationRepository: LocationRepository,
        private val placeRepository: PlaceRepository,
        private val photoRepository: PhotoRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TripHistoryViewModel::class.java)) {
                return TripHistoryViewModel(tripRepository, locationRepository, placeRepository, photoRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}