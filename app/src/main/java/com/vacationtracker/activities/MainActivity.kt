package com.vacationtracker.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.vacationtracker.R
import com.vacationtracker.adapters.NearbyPlaceAdapter
import com.vacationtracker.database.VacationDatabase
import com.vacationtracker.databinding.ActivityMainBinding
import com.vacationtracker.databinding.DialogPlaceSelectionBinding
import com.vacationtracker.models.LocationPoint
import com.vacationtracker.models.Trip
import com.vacationtracker.repositories.*
import com.vacationtracker.services.LocationTrackingService
import com.vacationtracker.services.NearbyPlace
import com.vacationtracker.services.NearbyPlacesService
import com.vacationtracker.services.PhotoMonitoringService
import com.vacationtracker.utils.PermissionUtils
import com.vacationtracker.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var nearbyPlacesService: NearbyPlacesService
    
    private val viewModel: MainViewModel by viewModels {
        val database = VacationDatabase.getDatabase(this)
        MainViewModel.Factory(
            TripRepository(database.tripDao()),
            LocationRepository(database.locationDao()),
            PlaceRepository(database.placeDao())
        )
    }
    
    private var routePolyline: Polyline? = null
    private val placeMarkers = mutableListOf<Marker>()
    
    // Permission launcher
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                enableLocationFeatures()
            }
            else -> {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Camera variables
    private var currentPhotoPath: String? = null
    private var currentPhotoPlaceId: Long = -1L
    
    // Camera result launcher
    private val cameraResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            handleCameraResult()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        nearbyPlacesService = NearbyPlacesService(this)
        
        setupMap()
        setupUI()
        observeViewModel()
        checkPermissions()
    }
    
    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Configure map
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = false
            isCompassEnabled = true
            isZoomGesturesEnabled = true
            isScrollGesturesEnabled = true
            isTiltGesturesEnabled = true
            isRotateGesturesEnabled = true
        }
        
        // Set default location (San Diego)
        val defaultLocation = LatLng(32.7157, -117.1611)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
        
        // Enable location if permission granted
        enableLocationFeatures()
        
        // Map click listener for adding places
        googleMap.setOnMapClickListener { latLng ->
            viewModel.currentTrip.value?.let { trip ->
                showAddPlaceDialog(latLng, trip.id)
            }
        }
    }
    
    private fun setupUI() {
        binding.iconTripAction.setOnClickListener {
            showStartTripDialog()
        }
        
        binding.iconHistory.setOnClickListener {
            val intent = Intent(this, TripHistoryActivity::class.java)
            startActivity(intent)
        }
        
        binding.iconGeofence.setOnClickListener {
            showNearbyPlacesDialog()
        }
        
        binding.iconCamera.setOnClickListener {
            takePictureForRecentPlace()
        }
        
        binding.fabMyLocation.setOnClickListener {
            moveToCurrentLocation()
        }
        
        binding.fabMyLocation.setOnLongClickListener {
            zoomToFitRoute()
            true
        }
        
        binding.fabZoomIn.setOnClickListener {
            zoomIn()
        }
        
        binding.fabZoomOut.setOnClickListener {
            zoomOut()
        }
    }
    
    private fun observeViewModel() {
        viewModel.currentTrip.observe(this) { trip ->
            updateUI(trip)
        }
        
        viewModel.currentTripLocations.observe(this) { locations ->
            updateRouteOnMap(locations)
        }
        
        viewModel.currentTripPlaces.observe(this) { places ->
            updatePlacesOnMap(places)
        }
        
        viewModel.tripStatistics.observe(this) { stats ->
            updateTripStats(stats)
        }
    }
    
    private fun updateUI(trip: Trip?) {
        if (trip != null) {
            binding.tvCurrentTrip.text = trip.name
            binding.iconTripAction.setImageResource(R.drawable.ic_pause)
            binding.iconTripAction.contentDescription = "End Trip"
            binding.iconGeofence.isEnabled = true
            binding.iconGeofence.alpha = 1.0f
            binding.iconCamera.isEnabled = true
            binding.iconCamera.alpha = 1.0f
            binding.layoutTripStats.visibility = android.view.View.VISIBLE
        } else {
            binding.tvCurrentTrip.text = "No active trip"
            binding.iconTripAction.setImageResource(R.drawable.ic_play)
            binding.iconTripAction.contentDescription = "Start Trip"
            binding.iconGeofence.isEnabled = false
            binding.iconGeofence.alpha = 0.5f
            binding.iconCamera.isEnabled = false
            binding.iconCamera.alpha = 0.5f
            binding.layoutTripStats.visibility = android.view.View.GONE
        }
    }
    
    private fun updateRouteOnMap(locations: List<LocationPoint>) {
        // Remove existing polyline
        routePolyline?.remove()
        
        if (locations.size >= 2) {
            val points = locations.map { LatLng(it.latitude, it.longitude) }
            
            routePolyline = googleMap.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .color(Color.BLUE)
                    .width(8f)
                    .pattern(listOf(Dash(20f), Gap(10f)))
            )
        }
    }
    
    private fun updatePlacesOnMap(places: List<com.vacationtracker.models.Place>) {
        // Clear existing markers
        placeMarkers.forEach { it.remove() }
        placeMarkers.clear()
        
        places.forEach { place ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(place.latitude, place.longitude))
                    .title(place.name)
                    .snippet(place.notes ?: "Visited at ${place.visitedAt}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            marker?.let { placeMarkers.add(it) }
        }
    }
    
    private fun updateTripStats(stats: MainViewModel.TripStatistics) {
        binding.tvDistance.text = "Distance: ${String.format("%.1f", stats.distance / 1000)} km"
        
        val hours = stats.duration / (1000 * 60 * 60)
        val minutes = (stats.duration % (1000 * 60 * 60)) / (1000 * 60)
        binding.tvDuration.text = "Duration: ${hours}h ${minutes}m"
        
        binding.tvPlaces.text = "Places: ${stats.placesCount}"
    }
    
    private fun showStartTripDialog() {
        val currentTrip = viewModel.currentTrip.value
        
        if (currentTrip != null) {
            // End current trip
            AlertDialog.Builder(this)
                .setTitle("End Trip")
                .setMessage("Are you sure you want to end '${currentTrip.name}'?")
                .setPositiveButton("End") { _, _ ->
                    lifecycleScope.launch {
                        viewModel.endCurrentTrip()
                        stopLocationTracking()
                        stopPhotoMonitoring()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Start new trip
            val editText = android.widget.EditText(this)
            editText.hint = "Trip name"
            
            AlertDialog.Builder(this)
                .setTitle("Start New Trip")
                .setView(editText)
                .setPositiveButton("Start") { _, _ ->
                    val tripName = editText.text.toString().trim()
                    if (tripName.isNotEmpty()) {
                        lifecycleScope.launch {
                            viewModel.startNewTrip(tripName)
                            startLocationTracking()
                            startPhotoMonitoring()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    
    private fun showAddPlaceDialog(latLng: LatLng, tripId: Long) {
        val editText = android.widget.EditText(this)
        editText.hint = "Place name"
        
        AlertDialog.Builder(this)
            .setTitle("Add Place")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val placeName = editText.text.toString().trim()
                if (placeName.isNotEmpty()) {
                    lifecycleScope.launch {
                        viewModel.addPlace(placeName, latLng.latitude, latLng.longitude, tripId)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showNearbyPlacesDialog() {
        if (!PermissionUtils.hasLocationPermission(this)) {
            Toast.makeText(this, "Location permission required to find nearby places", Toast.LENGTH_SHORT).show()
            return
        }
        
        val trip = viewModel.currentTrip.value
        if (trip == null) {
            Toast.makeText(this, "No active trip", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialogBinding = DialogPlaceSelectionBinding.inflate(LayoutInflater.from(this))
        
        // Create dialog first
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setNegativeButton("Cancel", null)
            .create()
        
        val nearbyPlaceAdapter = NearbyPlaceAdapter { selectedPlace ->
            // Add the selected place to the trip
            lifecycleScope.launch {
                viewModel.addPlace(
                    selectedPlace.name, 
                    selectedPlace.latitude, 
                    selectedPlace.longitude, 
                    trip.id,
                    selectedPlace.address
                )
                Toast.makeText(this@MainActivity, "Added: ${selectedPlace.name}", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        
        dialogBinding.recyclerViewPlaces.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = nearbyPlaceAdapter
        }
        
        // Setup button listeners
        dialogBinding.buttonManualEntry.setOnClickListener {
            dialog.dismiss()
            getCurrentLocationAndAddPlaceManually()
        }
        
        dialogBinding.buttonRefresh.setOnClickListener {
            loadNearbyPlaces(dialogBinding, nearbyPlaceAdapter)
        }
        
        // Load nearby places
        loadNearbyPlaces(dialogBinding, nearbyPlaceAdapter)
        
        dialog.show()
    }
    
    private fun getCurrentLocationAndAddPlaceManually() {
        if (PermissionUtils.hasLocationPermission(this)) {
            getCurrentLocation { location ->
                viewModel.currentTrip.value?.let { trip ->
                    showAddPlaceDialog(LatLng(location.latitude, location.longitude), trip.id)
                }
            }
        }
    }
    
    private fun loadNearbyPlaces(dialogBinding: DialogPlaceSelectionBinding, adapter: NearbyPlaceAdapter) {
        lifecycleScope.launch {
            // Show loading state
            dialogBinding.textLoading.visibility = android.view.View.VISIBLE
            dialogBinding.recyclerViewPlaces.visibility = android.view.View.GONE
            dialogBinding.textNoPlaces.visibility = android.view.View.GONE
            
            try {
                val result = nearbyPlacesService.getNearbyPlaces()
                
                if (result.isSuccess) {
                    val places = result.getOrNull() ?: emptyList()
                    
                    if (places.isNotEmpty()) {
                        adapter.submitList(places)
                        dialogBinding.textLoading.visibility = android.view.View.GONE
                        dialogBinding.recyclerViewPlaces.visibility = android.view.View.VISIBLE
                        dialogBinding.textNoPlaces.visibility = android.view.View.GONE
                    } else {
                        dialogBinding.textLoading.visibility = android.view.View.GONE
                        dialogBinding.recyclerViewPlaces.visibility = android.view.View.GONE
                        dialogBinding.textNoPlaces.visibility = android.view.View.VISIBLE
                    }
                } else {
                    // Show error
                    dialogBinding.textLoading.visibility = android.view.View.GONE
                    dialogBinding.recyclerViewPlaces.visibility = android.view.View.GONE
                    dialogBinding.textNoPlaces.visibility = android.view.View.VISIBLE
                    
                    val error = result.exceptionOrNull()
                    Toast.makeText(this@MainActivity, "Error: ${error?.message}", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                dialogBinding.textLoading.visibility = android.view.View.GONE
                dialogBinding.recyclerViewPlaces.visibility = android.view.View.GONE
                dialogBinding.textNoPlaces.visibility = android.view.View.VISIBLE
                
                Toast.makeText(this@MainActivity, "Error finding places: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun zoomIn() {
        googleMap?.let { map ->
            val currentZoom = map.cameraPosition.zoom
            if (currentZoom < map.maxZoomLevel) {
                map.animateCamera(
                    CameraUpdateFactory.zoomTo(currentZoom + 1f),
                    300, // 300ms animation
                    null
                )
            }
        }
    }
    
    private fun zoomOut() {
        googleMap?.let { map ->
            val currentZoom = map.cameraPosition.zoom
            if (currentZoom > map.minZoomLevel) {
                map.animateCamera(
                    CameraUpdateFactory.zoomTo(currentZoom - 1f),
                    300, // 300ms animation
                    null
                )
            }
        }
    }
    
    private fun zoomToFitRoute() {
        viewModel.currentTripLocations.value?.let { locations ->
            if (locations.isNotEmpty()) {
                val builder = LatLngBounds.Builder()
                locations.forEach { location ->
                    builder.include(LatLng(location.latitude, location.longitude))
                }
                
                // Also include current places
                viewModel.currentTripPlaces.value?.let { places ->
                    places.forEach { place ->
                        builder.include(LatLng(place.latitude, place.longitude))
                    }
                }
                
                try {
                    val bounds = builder.build()
                    val padding = 100 // 100 pixels padding
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(bounds, padding),
                        500, // 500ms animation
                        null
                    )
                } catch (e: IllegalStateException) {
                    // Handle case where bounds are not valid
                }
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(callback: (Location) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { callback(it) }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun moveToCurrentLocation() {
        if (PermissionUtils.hasLocationPermission(this)) {
            getCurrentLocation { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun enableLocationFeatures() {
        if (PermissionUtils.hasLocationPermission(this)) {
            googleMap.isMyLocationEnabled = true
        }
    }
    
    private fun startLocationTracking() {
        if (PermissionUtils.hasLocationPermission(this)) {
            val intent = Intent(this, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START_TRACKING
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
    
    private fun stopLocationTracking() {
        val intent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_TRACKING
        }
        startService(intent)
    }
    
    private fun startPhotoMonitoring() {
        if (PermissionUtils.hasLocationPermission(this)) {
            val intent = Intent(this, PhotoMonitoringService::class.java).apply {
                action = PhotoMonitoringService.ACTION_START_MONITORING
            }
            startService(intent)
        }
    }
    
    private fun stopPhotoMonitoring() {
        val intent = Intent(this, PhotoMonitoringService::class.java).apply {
            action = PhotoMonitoringService.ACTION_STOP_MONITORING
        }
        startService(intent)
    }
    
    private fun takePictureForRecentPlace() {
        // Check if we have camera permission
        if (!PermissionUtils.hasCameraPermission(this)) {
            android.widget.Toast.makeText(this, "Camera permission required to take photos", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get the most recently added place
        lifecycleScope.launch {
            val currentTrip = viewModel.currentTrip.value
            if (currentTrip == null) {
                android.widget.Toast.makeText(this@MainActivity, "No active trip", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val database = com.vacationtracker.database.VacationDatabase.getDatabase(this@MainActivity)
            val places = database.placeDao().getPlacesByTripSync(currentTrip.id)
            
            if (places.isEmpty()) {
                android.widget.Toast.makeText(this@MainActivity, "No places added yet. Add a place first.", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            // Get the most recent place (last added)
            val recentPlace = places.maxByOrNull { it.visitedAt }
            if (recentPlace != null) {
                launchCameraForPlace(recentPlace)
            }
        }
    }
    
    private fun launchCameraForPlace(place: com.vacationtracker.models.Place) {
        try {
            val photoFile = com.vacationtracker.utils.PhotoUtils.createImageFile(this)
            val photoUri = com.vacationtracker.utils.PhotoUtils.getPhotoUri(this, photoFile)
            
            val cameraIntent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri)
                putExtra("place_id", place.id)
                putExtra("photo_path", photoFile.absolutePath)
            }
            
            if (cameraIntent.resolveActivity(packageManager) != null) {
                cameraResultLauncher.launch(cameraIntent)
                currentPhotoPath = photoFile.absolutePath
                currentPhotoPlaceId = place.id
                android.widget.Toast.makeText(this, "Taking photo for: ${place.name}", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(this, "Camera app not found", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Error launching camera: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleCameraResult() {
        val photoPath = currentPhotoPath
        val placeId = currentPhotoPlaceId
        
        if (photoPath != null && placeId != -1L) {
            // Save photo to database
            lifecycleScope.launch {
                try {
                    val database = com.vacationtracker.database.VacationDatabase.getDatabase(this@MainActivity)
                    val photoRepository = com.vacationtracker.repositories.PhotoRepository(database.photoDao())
                    
                    photoRepository.addPhoto(
                        filePath = photoPath,
                        tripId = viewModel.currentTrip.value?.id ?: -1L,
                        placeId = placeId,
                        timestamp = java.util.Date()
                    )
                    android.widget.Toast.makeText(this@MainActivity, "Photo saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this@MainActivity, "Error saving photo: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Reset values
        currentPhotoPath = null
        currentPhotoPlaceId = -1L
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (!PermissionUtils.hasLocationPermission(this)) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        
        // Photo access permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        if (permissions.isNotEmpty()) {
            requestPermissions.launch(permissions.toTypedArray())
        }
    }
}