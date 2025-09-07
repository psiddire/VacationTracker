package com.vacationtracker.viewmodels

import androidx.lifecycle.*
import com.vacationtracker.models.Photo
import com.vacationtracker.models.Place
import com.vacationtracker.repositories.PhotoRepository
import com.vacationtracker.repositories.PlaceRepository
import kotlinx.coroutines.launch

class PlaceDetailViewModel(
    private val placeId: Long,
    private val placeRepository: PlaceRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {
    
    private val _place = MutableLiveData<Place?>()
    val place: LiveData<Place?> = _place
    
    val photos: LiveData<List<Photo>> = photoRepository.getPhotosByPlace(placeId)
    
    init {
        loadPlace()
    }
    
    private fun loadPlace() {
        viewModelScope.launch {
            _place.value = placeRepository.getPlaceById(placeId)
        }
    }
    
    fun addPhoto(filePath: String, caption: String? = null) {
        viewModelScope.launch {
            val currentPlace = _place.value
            if (currentPlace != null) {
                photoRepository.addPhoto(
                    filePath = filePath,
                    tripId = currentPlace.tripId,
                    placeId = placeId,
                    latitude = currentPlace.latitude,
                    longitude = currentPlace.longitude,
                    caption = caption
                )
            }
        }
    }
    
    fun deletePhoto(photo: Photo) {
        viewModelScope.launch {
            photoRepository.deletePhoto(photo)
        }
    }
    
    class Factory(
        private val placeId: Long,
        private val placeRepository: PlaceRepository,
        private val photoRepository: PhotoRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlaceDetailViewModel::class.java)) {
                return PlaceDetailViewModel(placeId, placeRepository, photoRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}