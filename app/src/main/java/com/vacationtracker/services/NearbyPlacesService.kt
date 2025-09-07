package com.vacationtracker.services

import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.tasks.await

data class NearbyPlace(
    val id: String,
    val name: String,
    val address: String?,
    val placeTypes: List<String>,
    val latitude: Double,
    val longitude: Double,
    val rating: Double?,
    val likelihood: Double
)

class NearbyPlacesService(private val context: Context) {
    
    private val placesClient: PlacesClient by lazy {
        if (!Places.isInitialized()) {
            Places.initialize(context, getApiKey())
        }
        Places.createClient(context)
    }
    
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    private fun getApiKey(): String {
        // Get API key from local.properties or BuildConfig
        val packageInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_META_DATA
        )
        return packageInfo.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
    }
    
    suspend fun getNearbyPlaces(): Result<List<NearbyPlace>> {
        return try {
            // Check if we have location permission
            if (!hasLocationPermission()) {
                return Result.failure(SecurityException("Location permission not granted"))
            }
            
            // Define place fields we want to retrieve
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.TYPES,
                Place.Field.RATING
            )
            
            // Create the request
            val request = FindCurrentPlaceRequest.newInstance(placeFields)
            
            // Get current place predictions
            val response: FindCurrentPlaceResponse = placesClient.findCurrentPlace(request).await()
            
            // Filter and convert to our data class
            val nearbyPlaces = response.placeLikelihoods
                .filter { isRelevantPlace(it.place) }
                .sortedByDescending { it.likelihood }
                .take(10) // Limit to top 10 results
                .map { placeLikelihood ->
                    val place = placeLikelihood.place
                    NearbyPlace(
                        id = place.id ?: "",
                        name = place.name ?: "Unknown Place",
                        address = place.address,
                        placeTypes = place.types?.map { it.name } ?: emptyList(),
                        latitude = place.latLng?.latitude ?: 0.0,
                        longitude = place.latLng?.longitude ?: 0.0,
                        rating = place.rating,
                        likelihood = placeLikelihood.likelihood
                    )
                }
            
            Result.success(nearbyPlaces)
            
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
    
    private fun isRelevantPlace(place: Place): Boolean {
        val relevantTypes = setOf(
            "restaurant",
            "tourist_attraction", 
            "museum",
            "park",
            "lodging",
            "shopping_mall",
            "store",
            "cafe",
            "bar",
            "night_club",
            "amusement_park",
            "zoo",
            "church",
            "stadium",
            "movie_theater",
            "bowling_alley",
            "casino",
            "spa",
            "gym",
            "library",
            "hospital",
            "airport",
            "subway_station",
            "train_station",
            "gas_station"
        )
        
        return place.types?.any { placeType ->
            relevantTypes.contains(placeType.name.lowercase())
        } ?: false
    }
    
    private fun hasLocationPermission(): Boolean {
        return try {
            val permission = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            permission == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getCurrentLocation(): Location? {
        return try {
            if (!hasLocationPermission()) {
                return null
            }
            
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }
}