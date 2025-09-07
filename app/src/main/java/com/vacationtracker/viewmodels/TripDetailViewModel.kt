package com.vacationtracker.viewmodels

import androidx.lifecycle.*
import com.vacationtracker.models.Photo
import com.vacationtracker.models.Place
import com.vacationtracker.models.Trip
import com.vacationtracker.repositories.LocationRepository
import com.vacationtracker.repositories.PhotoRepository
import com.vacationtracker.repositories.PlaceRepository
import com.vacationtracker.repositories.TripRepository
import kotlinx.coroutines.launch

class TripDetailViewModel(
    private val tripId: Long,
    private val tripRepository: TripRepository,
    private val locationRepository: LocationRepository,
    private val placeRepository: PlaceRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {
    
    private val _trip = MutableLiveData<Trip?>()
    val trip: LiveData<Trip?> = _trip
    
    
    private val _placesCount = MutableLiveData<Int>()
    val placesCount: LiveData<Int> = _placesCount
    
    val places: LiveData<List<Place>> = placeRepository.getPlacesByTrip(tripId)
    
    // Filtered important places based on photo count (significance indicator)
    val importantPlaces: LiveData<List<Place>> = places.switchMap { allPlaces ->
        filterImportantPlaces(allPlaces)
    }
    val photos: LiveData<List<Photo>> = photoRepository.getPhotosByTrip(tripId)
    val locations: LiveData<List<com.vacationtracker.models.LocationPoint>> = locationRepository.getLocationsByTrip(tripId)
    
    init {
        loadTripDetails()
    }
    
    private fun loadTripDetails() {
        viewModelScope.launch {
            _trip.value = tripRepository.getTripById(tripId)
            _placesCount.value = placeRepository.getPlaceCountForTrip(tripId)
        }
    }
    
    private fun filterImportantPlaces(places: List<Place>): LiveData<List<Place>> {
        val result = MutableLiveData<List<Place>>()
        
        viewModelScope.launch {
            val importantPlaces = places.mapNotNull { place ->
                val photoCount = photoRepository.getPhotoCountForPlace(place.id)
                if (photoCount > 0) {
                    place to photoCount
                } else {
                    null
                }
            }.sortedByDescending { (_, photoCount) ->
                photoCount
            }.map { (place, _) -> 
                place
            }
            
            result.value = importantPlaces
        }
        
        return result
    }
    
    class Factory(
        private val tripId: Long,
        private val tripRepository: TripRepository,
        private val locationRepository: LocationRepository,
        private val placeRepository: PlaceRepository,
        private val photoRepository: PhotoRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TripDetailViewModel::class.java)) {
                return TripDetailViewModel(tripId, tripRepository, locationRepository, placeRepository, photoRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}