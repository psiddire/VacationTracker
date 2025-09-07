package com.vacationtracker.viewmodels

import androidx.lifecycle.*
import com.vacationtracker.models.Place
import com.vacationtracker.models.Trip
import com.vacationtracker.repositories.PlaceRepository
import com.vacationtracker.repositories.TripRepository

class ItineraryViewModel(
    private val tripRepository: TripRepository,
    private val placeRepository: PlaceRepository
) : ViewModel() {
    
    val currentTrip: LiveData<Trip?> = tripRepository.getActiveTripLive()
    
    val places: LiveData<List<Place>> = currentTrip.switchMap { trip ->
        if (trip != null) {
            placeRepository.getPlacesByTrip(trip.id)
        } else {
            MutableLiveData(emptyList())
        }
    }
    
    class Factory(
        private val tripRepository: TripRepository,
        private val placeRepository: PlaceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ItineraryViewModel::class.java)) {
                return ItineraryViewModel(tripRepository, placeRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}