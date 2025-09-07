package com.vacationtracker.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.vacationtracker.adapters.PhotoItemAdapter
import com.vacationtracker.adapters.PlaceItemAdapter
import com.vacationtracker.database.VacationDatabase
import com.vacationtracker.databinding.ActivityTripDetailBinding
import com.vacationtracker.repositories.LocationRepository
import com.vacationtracker.repositories.PhotoRepository
import com.vacationtracker.repositories.PlaceRepository
import com.vacationtracker.repositories.TripRepository
import com.vacationtracker.viewmodels.TripDetailViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class TripDetailActivity : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var binding: ActivityTripDetailBinding
    private lateinit var placeAdapter: PlaceItemAdapter
    private lateinit var photoAdapter: PhotoItemAdapter
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private var highlightedMarker: com.google.android.gms.maps.model.Marker? = null
    private var routePolyline: com.google.android.gms.maps.model.Polyline? = null
    private val placeMarkers = mutableListOf<com.google.android.gms.maps.model.Marker>()
    private var startMarker: com.google.android.gms.maps.model.Marker? = null
    private var endMarker: com.google.android.gms.maps.model.Marker? = null
    private var selectedPlace: com.vacationtracker.models.Place? = null
    private var allPhotos: List<com.vacationtracker.models.Photo> = emptyList()
    
    private val viewModel: TripDetailViewModel by viewModels {
        val database = VacationDatabase.getDatabase(this)
        val tripId = intent.getLongExtra("trip_id", -1L)
        TripDetailViewModel.Factory(
            tripId,
            TripRepository(database.tripDao()),
            LocationRepository(database.locationDao()),
            PlaceRepository(database.placeDao()),
            PhotoRepository(database.photoDao())
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupMapView(savedInstanceState)
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Trip Details"
    }
    
    private fun setupMapView(savedInstanceState: Bundle?) {
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        
        // Click listeners will be set up in setupClickListeners()
    }
    
    private fun setupRecyclerViews() {
        // Setup places RecyclerView
        placeAdapter = PlaceItemAdapter { place ->
            showPlaceOnMap(place)
        }
        
        binding.recyclerViewPlaces.apply {
            layoutManager = LinearLayoutManager(this@TripDetailActivity)
            adapter = placeAdapter
        }
        
        // Setup photos RecyclerView
        photoAdapter = PhotoItemAdapter { photo ->
            openPhotoViewer(photo)
        }
        
        binding.recyclerViewPhotos.apply {
            layoutManager = GridLayoutManager(this@TripDetailActivity, 3)
            adapter = photoAdapter
        }
    }
    
    private fun setupClickListeners() {
        // Set up click listener for the Trip Route card
        binding.cardTripRoute.setOnClickListener {
            android.util.Log.d("TripDetailActivity", "Trip Route card clicked!")
            openExpandedMap()
        }
        
        // Set up collapse/expand for Places section
        var isPlacesExpanded = true
        binding.layoutPlacesHeader.setOnClickListener {
            isPlacesExpanded = !isPlacesExpanded
            
            if (isPlacesExpanded) {
                // Show content with animation
                binding.layoutPlacesContent.visibility = android.view.View.VISIBLE
                binding.layoutPlacesContent.alpha = 0f
                binding.layoutPlacesContent.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
                
                binding.iconPlacesCollapse.setImageResource(R.drawable.ic_expand_less)
                binding.iconPlacesCollapse.contentDescription = "Collapse places"
            } else {
                // Hide content with animation
                binding.layoutPlacesContent.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        binding.layoutPlacesContent.visibility = android.view.View.GONE
                    }
                    .start()
                
                binding.iconPlacesCollapse.setImageResource(R.drawable.ic_expand_more)
                binding.iconPlacesCollapse.contentDescription = "Expand places"
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.trip.observe(this) { trip ->
            trip?.let {
                supportActionBar?.title = it.name
                
                val dateFormatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                binding.textStartDate.text = "Started: ${dateFormatter.format(it.startDate)}"
                
                if (it.endDate != null) {
                    binding.textEndDate.text = "Ended: ${dateFormatter.format(it.endDate)}"
                    
                    // Calculate duration
                    val durationMillis = it.endDate.time - it.startDate.time
                    val days = TimeUnit.MILLISECONDS.toDays(durationMillis)
                    val hours = TimeUnit.MILLISECONDS.toHours(durationMillis) % 24
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
                    
                    if (days > 0) {
                        binding.textDuration.text = "Duration: ${days}d ${hours}h ${minutes}m"
                    } else {
                        binding.textDuration.text = "Duration: ${hours}h ${minutes}m"
                    }
                } else {
                    binding.textEndDate.text = "Status: Active trip"
                    binding.textDuration.text = "Duration: In progress"
                }
                
                binding.textDistance.text = "Distance: ${String.format("%.1f", it.totalDistance / 1000)} km"
                
                if (it.description.isNullOrBlank()) {
                    binding.textDescription.visibility = android.view.View.GONE
                } else {
                    binding.textDescription.text = it.description
                    binding.textDescription.visibility = android.view.View.VISIBLE
                }
            }
        }
        
        
        viewModel.placesCount.observe(this) { count ->
            binding.textPlacesCount.text = "Total Places: $count"
        }
        
        viewModel.importantPlaces.observe(this) { places ->
            placeAdapter.submitList(places)
            
            if (places.isEmpty()) {
                binding.textNoPlaces.visibility = android.view.View.VISIBLE
                binding.textNoPlaces.text = "No significant places visited yet"
                binding.recyclerViewPlaces.visibility = android.view.View.GONE
            } else {
                binding.textNoPlaces.visibility = android.view.View.GONE
                binding.recyclerViewPlaces.visibility = android.view.View.VISIBLE
            }
        }
        
        viewModel.photos.observe(this) { photos ->
            allPhotos = photos
            updatePhotoDisplay()
        }
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Configure map settings
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isCompassEnabled = true
            uiSettings.isMyLocationButtonEnabled = false
        }
        
        // Set up map click listener to clear place selection
        googleMap?.setOnMapClickListener {
            clearPlaceSelection()
        }
        
        // Load trip data onto map
        loadTripDataOnMap()
    }
    
    private fun loadTripDataOnMap() {
        // Observe location points for route
        viewModel.locations.observe(this) { locations ->
            if (locations.isNotEmpty()) {
                drawRoute(locations)
                binding.mapView.visibility = android.view.View.VISIBLE
                binding.textNoRoute.visibility = android.view.View.GONE
            } else {
                binding.mapView.visibility = android.view.View.GONE
                binding.textNoRoute.visibility = android.view.View.VISIBLE
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
                .width(8f)
            
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
            
            // Fit camera to show entire route
            if (routePoints.size > 1) {
                val boundsBuilder = LatLngBounds.Builder()
                routePoints.forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()
                val padding = 100 // pixels
                
                // Use post to ensure map layout is complete
                binding.mapView.post {
                    try {
                        // First try the simple method
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    } catch (e: IllegalStateException) {
                        // If that fails, use the method with explicit dimensions
                        val width = binding.mapView.width
                        val height = binding.mapView.height
                        if (width > 0 && height > 0) {
                            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding))
                        }
                    }
                }
            } else if (routePoints.isNotEmpty()) {
                binding.mapView.post {
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
                        .snippet(place.address ?: "")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                )
                marker?.let { placeMarkers.add(it) }
            }
        }
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
    
    private fun showPlaceOnMap(place: com.vacationtracker.models.Place) {
        // Set selected place and update photo display
        selectedPlace = place
        updatePhotoDisplay()
        
        googleMap?.let { map ->
            // Remove previous highlight marker
            highlightedMarker?.remove()
            
            // Create highlighted marker for the selected place
            val placeLocation = LatLng(place.latitude, place.longitude)
            highlightedMarker = map.addMarker(
                MarkerOptions()
                    .position(placeLocation)
                    .title(place.name)
                    .snippet("${place.address ?: ""}\nTap for details\nPhotos filtered by this location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
            )
            
            // Animate camera to the place location
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(placeLocation, 16f))
            
            // Show info window
            highlightedMarker?.showInfoWindow()
            
            // Set marker click listener to open place details
            highlightedMarker?.let { marker ->
                map.setOnMarkerClickListener { clickedMarker ->
                    if (clickedMarker == marker) {
                        // Open place detail activity when the highlighted marker is clicked
                        val intent = Intent(this, PlaceDetailActivity::class.java)
                        intent.putExtra("place_id", place.id)
                        startActivity(intent)
                        true
                    } else {
                        false
                    }
                }
            }
            
            // Scroll to map view to ensure it's visible
            binding.scrollView.post {
                binding.scrollView.smoothScrollTo(0, binding.mapView.top)
            }
        }
    }
    
    private fun updatePhotoDisplay() {
        val filteredPhotos = if (selectedPlace != null) {
            filterPhotosByPlace(allPhotos, selectedPlace!!)
        } else {
            allPhotos
        }
        
        photoAdapter.submitList(filteredPhotos)
        
        // Update section title to show filter state
        binding.textPhotosTitle.text = if (selectedPlace != null) {
            "Photos at ${selectedPlace!!.name} (${filteredPhotos.size})"
        } else {
            "Trip Photos (${filteredPhotos.size})"
        }
        
        if (filteredPhotos.isEmpty()) {
            binding.textNoPhotos.visibility = android.view.View.VISIBLE
            binding.recyclerViewPhotos.visibility = android.view.View.GONE
            
            // Update message based on filter state
            binding.textNoPhotos.text = if (selectedPlace != null) {
                "No photos found at ${selectedPlace!!.name}\nTap map to show all photos"
            } else {
                "No photos added yet"
            }
        } else {
            binding.textNoPhotos.visibility = android.view.View.GONE
            binding.recyclerViewPhotos.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun filterPhotosByPlace(photos: List<com.vacationtracker.models.Photo>, place: com.vacationtracker.models.Place): List<com.vacationtracker.models.Photo> {
        return photos.filter { photo ->
            // Check if photo has location data
            if (photo.latitude != null && photo.longitude != null) {
                // Calculate distance between photo location and place location
                val photoLocation = android.location.Location("photo").apply {
                    latitude = photo.latitude
                    longitude = photo.longitude
                }
                
                val placeLocation = android.location.Location("place").apply {
                    latitude = place.latitude
                    longitude = place.longitude
                }
                
                // Consider photos within 200 meters of the place
                val distance = photoLocation.distanceTo(placeLocation)
                distance <= 200.0 // 200 meters threshold
            } else {
                // For photos without GPS data, check if they're specifically linked to this place
                photo.placeId == place.id
            }
        }
    }
    
    private fun openExpandedMap() {
        android.util.Log.d("TripDetailActivity", "Opening expanded map")
        val tripId = intent.getLongExtra("trip_id", -1L)
        val tripName = viewModel.trip.value?.name ?: "Trip Route"
        android.util.Log.d("TripDetailActivity", "Trip ID: $tripId, Trip Name: $tripName")
        val intent = Intent(this, TripMapActivity::class.java).apply {
            putExtra(TripMapActivity.EXTRA_TRIP_ID, tripId)
            putExtra(TripMapActivity.EXTRA_TRIP_NAME, tripName)
        }
        startActivity(intent)
    }
    
    private fun clearPlaceSelection() {
        selectedPlace = null
        highlightedMarker?.remove()
        highlightedMarker = null
        updatePhotoDisplay()
    }
    
    private fun openPhotoViewer(photo: com.vacationtracker.models.Photo) {
        val intent = Intent(this, PhotoViewerActivity::class.java).apply {
            putExtra(PhotoViewerActivity.EXTRA_PHOTO_PATH, photo.filePath)
            putExtra(PhotoViewerActivity.EXTRA_PHOTO_TIMESTAMP, photo.timestamp.time)
            putExtra(PhotoViewerActivity.EXTRA_PHOTO_CAPTION, photo.caption)
        }
        startActivity(intent)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}