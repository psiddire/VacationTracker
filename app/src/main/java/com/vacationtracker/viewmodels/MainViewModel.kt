package com.vacationtracker.viewmodels

import androidx.lifecycle.*
import com.vacationtracker.models.LocationPoint
import com.vacationtracker.models.Place
import com.vacationtracker.models.Trip
import com.vacationtracker.repositories.LocationRepository
import com.vacationtracker.repositories.PlaceRepository
import com.vacationtracker.repositories.TripRepository
import kotlinx.coroutines.launch

class MainViewModel(
    private val tripRepository: TripRepository,
    private val locationRepository: LocationRepository,
    private val placeRepository: PlaceRepository
) : ViewModel() {
    
    val currentTrip: LiveData<Trip?> = tripRepository.getActiveTripLive()
    
    val currentTripLocations: LiveData<List<LocationPoint>> = currentTrip.switchMap { trip ->
        if (trip != null) {
            locationRepository.getLocationsByTrip(trip.id)
        } else {
            MutableLiveData(emptyList())
        }
    }
    
    val currentTripPlaces: LiveData<List<Place>> = currentTrip.switchMap { trip ->
        if (trip != null) {
            placeRepository.getPlacesByTrip(trip.id)
        } else {
            MutableLiveData(emptyList())
        }
    }
    
    val tripStatistics: LiveData<TripStatistics> = currentTrip.switchMap { trip ->
        if (trip != null) {
            liveData {
                val placesCount = placeRepository.getPlaceCountForTrip(trip.id)
                emit(TripStatistics(
                    distance = trip.totalDistance,
                    duration = trip.totalDuration,
                    placesCount = placesCount
                ))
            }
        } else {
            MutableLiveData(TripStatistics())
        }
    }
    
    fun startNewTrip(name: String, description: String? = null) {
        viewModelScope.launch {
            tripRepository.startNewTrip(name, description)
        }
    }
    
    fun endCurrentTrip() {
        viewModelScope.launch {
            currentTrip.value?.let { trip ->
                tripRepository.endTrip(trip.id)
            }
        }
    }
    
    fun addPlace(name: String, latitude: Double, longitude: Double, tripId: Long, address: String? = null, notes: String? = null) {
        viewModelScope.launch {
            placeRepository.addPlace(name, latitude, longitude, tripId, address, notes = notes)
        }
    }
    
    data class TripStatistics(
        val distance: Double = 0.0,
        val duration: Long = 0L,
        val placesCount: Int = 0
    )
    
    class Factory(
        private val tripRepository: TripRepository,
        private val locationRepository: LocationRepository,
        private val placeRepository: PlaceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(tripRepository, locationRepository, placeRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}