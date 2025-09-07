package com.vacationtracker.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.vacationtracker.R
import com.vacationtracker.database.VacationDatabase
import com.vacationtracker.databinding.ActivityTripMapBinding
import com.vacationtracker.repositories.LocationRepository
import com.vacationtracker.repositories.PlaceRepository
import com.vacationtracker.repositories.TripRepository
import com.vacationtracker.viewmodels.TripDetailViewModel
import java.util.concurrent.TimeUnit

class TripMapActivity : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var binding: ActivityTripMapBinding
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private var routePolyline: com.google.android.gms.maps.model.Polyline? = null
    private val placeMarkers = mutableListOf<com.google.android.gms.maps.model.Marker>()
    private var startMarker: com.google.android.gms.maps.model.Marker? = null
    private var endMarker: com.google.android.gms.maps.model.Marker? = null
    
    private val viewModel: TripDetailViewModel by viewModels {
        val database = VacationDatabase.getDatabase(this)
        val tripId = intent.getLongExtra("trip_id", -1L)
        TripDetailViewModel.Factory(
            tripId,
            TripRepository(database.tripDao()),
            LocationRepository(database.locationDao()),
            PlaceRepository(database.placeDao()),
            com.vacationtracker.repositories.PhotoRepository(database.photoDao())
        )
    }
    
    companion object {
        const val EXTRA_TRIP_ID = "trip_id"
        const val EXTRA_TRIP_NAME = "trip_name"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupMapView(savedInstanceState)
        observeViewModel()
        setupZoomControls()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        val tripName = intent.getStringExtra(EXTRA_TRIP_NAME) ?: "Trip Route"
        supportActionBar?.title = "$tripName - Map"
    }
    
    private fun setupMapView(savedInstanceState: Bundle?) {
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }
    
    private fun setupZoomControls() {
        binding.buttonZoomIn.setOnClickListener {
            googleMap?.let { map ->
                val currentZoom = map.cameraPosition.zoom
                map.animateCamera(CameraUpdateFactory.zoomTo(currentZoom + 1))
            }
        }
        
        binding.buttonZoomOut.setOnClickListener {
            googleMap?.let { map ->
                val currentZoom = map.cameraPosition.zoom
                map.animateCamera(CameraUpdateFactory.zoomTo(currentZoom - 1))
            }
        }
        
        binding.buttonFitRoute.setOnClickListener {
            fitRouteToView()
        }
        
        binding.buttonCloseInfo.setOnClickListener {
            binding.cardTripInfo.visibility = android.view.View.GONE
        }
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Configure map settings for full screen experience
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = false // We have custom controls
            uiSettings.isCompassEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
            uiSettings.isRotateGesturesEnabled = true
            uiSettings.isTiltGesturesEnabled = true
            uiSettings.isScrollGesturesEnabled = true
            uiSettings.isZoomGesturesEnabled = true
            mapType = GoogleMap.MAP_TYPE_NORMAL
        }
        
        // Load trip data onto map
        loadTripDataOnMap()
    }
    
    private fun loadTripDataOnMap() {
        // Observe location points for route
        viewModel.locations.observe(this) { locations ->
            if (locations.isNotEmpty()) {
                drawRoute(locations)
            }
        }
        
        // Observe places for markers
        viewModel.places.observe(this) { places ->
            addPlaceMarkers(places)
        }
    }
    
    private fun drawRoute(locations: List<com.vacationtracker.models.LocationPoint>) {
        googleMap?.let { map ->
            // Clear existing route elements
            routePolyline?.remove()
            startMarker?.remove()
            endMarker?.remove()
            
            // Create route polyline
            val routePoints = locations.map { LatLng(it.latitude, it.longitude) }
            
            val polylineOptions = PolylineOptions()
                .addAll(routePoints)
                .color(resources.getColor(R.color.route_color, theme))
                .width(10f) // Slightly thicker for full screen view
            
            routePolyline = map.addPolyline(polylineOptions)
            
            // Add start marker
            if (routePoints.isNotEmpty()) {
                startMarker = map.addMarker(
                    MarkerOptions()
                        .position(routePoints.first())
                        .title("Trip Start")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
                
                // Add end marker if different from start
                if (routePoints.size > 1) {
                    endMarker = map.addMarker(
                        MarkerOptions()
                            .position(routePoints.last())
                            .title("Trip End")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                }
            }
            
            // Fit camera to show entire route with some delay to ensure map is ready
            binding.root.post {
                fitRouteToView()
            }
        }
    }
    
    private fun fitRouteToView() {
        googleMap?.let { map ->
            viewModel.locations.value?.let { locations ->
                val routePoints = locations.map { LatLng(it.latitude, it.longitude) }
                
                if (routePoints.size > 1) {
                    val boundsBuilder = LatLngBounds.Builder()
                    routePoints.forEach { boundsBuilder.include(it) }
                    val bounds = boundsBuilder.build()
                    val padding = 150 // More padding for full screen view
                    
                    try {
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    } catch (e: IllegalStateException) {
                        // If that fails, use the method with explicit dimensions
                        val width = binding.mapView.width
                        val height = binding.mapView.height
                        if (width > 0 && height > 0) {
                            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding))
                        }
                    }
                } else if (routePoints.isNotEmpty()) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(routePoints.first(), 15f))
                }
            }
        }
    }
    
    private fun addPlaceMarkers(places: List<com.vacationtracker.models.Place>) {
        googleMap?.let { map ->
            // Clear existing place markers
            placeMarkers.forEach { it.remove() }
            placeMarkers.clear()
            
            // Add place markers
            places.forEach { place ->
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(place.latitude, place.longitude))
                        .title(place.name)
                        .snippet(place.address ?: "Tap for more details")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                )
                marker?.let { placeMarkers.add(it) }
            }
            
            // Set marker click listener
            map.setOnMarkerClickListener { clickedMarker ->
                // Find the place corresponding to this marker
                val clickedPlace = places.find { place ->
                    val markerPosition = clickedMarker.position
                    markerPosition.latitude == place.latitude && markerPosition.longitude == place.longitude
                }
                
                clickedPlace?.let { place ->
                    // Show info window with more details
                    clickedMarker.title = place.name
                    clickedMarker.snippet = "${place.address ?: ""}\n${place.placeType ?: ""}"
                    clickedMarker.showInfoWindow()
                }
                
                true // Consume the click event
            }
        }
    }
    
    private fun observeViewModel() {
        var currentTrip: com.vacationtracker.models.Trip? = null
        
        viewModel.trip.observe(this) { trip ->
            trip?.let {
                supportActionBar?.title = "${it.name} - Map"
                currentTrip = it
                updateTripInfo()
            }
        }
        
        viewModel.places.observe(this) { places ->
            currentTrip?.let { updateTripInfo() }
        }
    }
    
    private fun updateTripInfo() {
        val trip = viewModel.trip.value ?: return
        val places = viewModel.places.value ?: emptyList()
        
        // Update trip name in overlay
        binding.textTripName.text = trip.name
        
        // Update trip stats
        val distance = String.format("%.1f", trip.totalDistance / 1000)
        val placesCount = places.size
        
        val duration = if (trip.endDate != null) {
            val durationMillis = trip.endDate.time - trip.startDate.time
            val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(durationMillis)
            val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(durationMillis) % 24
            
            when {
                days > 0 -> "${days}d ${hours}h"
                hours > 0 -> "${hours}h"
                else -> "<1h"
            }
        } else {
            "Active"
        }
        
        binding.textTripStats.text = "$distance km • $placesCount places • $duration"
    }
    
    // MapView lifecycle methods
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}