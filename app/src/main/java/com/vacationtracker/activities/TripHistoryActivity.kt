package com.vacationtracker.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import java.io.File
import com.vacationtracker.adapters.PhotoSelectionAdapter
import com.vacationtracker.adapters.TripAdapter
import com.vacationtracker.database.VacationDatabase
import com.vacationtracker.databinding.ActivityTripHistoryBinding
import com.vacationtracker.databinding.DialogPhotoSelectionBinding
import com.vacationtracker.repositories.TripRepository
import com.vacationtracker.repositories.LocationRepository
import com.vacationtracker.repositories.PhotoRepository
import com.vacationtracker.repositories.PlaceRepository
import com.vacationtracker.viewmodels.TripHistoryViewModel

class TripHistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTripHistoryBinding
    private lateinit var tripAdapter: TripAdapter
    
    private val viewModel: TripHistoryViewModel by viewModels {
        val database = VacationDatabase.getDatabase(this)
        TripHistoryViewModel.Factory(
            TripRepository(database.tripDao()),
            LocationRepository(database.locationDao()),
            PlaceRepository(database.placeDao()),
            PhotoRepository(database.photoDao())
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Trip History"
    }
    
    private fun setupRecyclerView() {
        tripAdapter = TripAdapter(
            onTripClick = { trip ->
                // Open trip detail view
                try {
                    val intent = Intent(this, TripDetailActivity::class.java)
                    intent.putExtra("trip_id", trip.id)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback: just show a toast for now
                    android.widget.Toast.makeText(this, "Trip: ${trip.name}", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onShareClick = { trip ->
                shareTrip(trip)
            },
            onRenameClick = { trip ->
                showRenameDialog(trip)
            },
            onDeleteClick = { trip ->
                showDeleteConfirmationDialog(trip)
            }
        )
        
        binding.recyclerViewTrips.apply {
            layoutManager = LinearLayoutManager(this@TripHistoryActivity)
            adapter = tripAdapter
        }
    }
    
    private fun shareTrip(trip: com.vacationtracker.models.Trip) {
        lifecycleScope.launch {
            try {
                // Get photos for this trip
                val database = VacationDatabase.getDatabase(this@TripHistoryActivity)
                val photoRepository = com.vacationtracker.repositories.PhotoRepository(database.photoDao())
                val photos = photoRepository.getPhotosByTripSync(trip.id)
                
                if (photos.isEmpty()) {
                    // No photos, share text only
                    shareTextOnly(trip)
                } else {
                    // Show photo selection dialog
                    showPhotoSelectionDialog(trip, photos)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(this@TripHistoryActivity, "Error loading photos: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun shareTextOnly(trip: com.vacationtracker.models.Trip) {
        try {
            val tripSummary = viewModel.generateTripSummary(trip.id)
            if (tripSummary != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, tripSummary)
                    putExtra(Intent.EXTRA_SUBJECT, "Trip Summary: ${trip.name}")
                }
                
                val chooserIntent = Intent.createChooser(shareIntent, "Share trip via...")
                startActivity(chooserIntent)
            } else {
                android.widget.Toast.makeText(this@TripHistoryActivity, "Unable to generate trip summary", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this@TripHistoryActivity, "Error sharing trip: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showPhotoSelectionDialog(trip: com.vacationtracker.models.Trip, photos: List<com.vacationtracker.models.Photo>) {
        val dialogBinding = DialogPhotoSelectionBinding.inflate(LayoutInflater.from(this))
        
        val photoSelectionAdapter = PhotoSelectionAdapter { selectedCount ->
            dialogBinding.textSelectedCount.text = "$selectedCount photos selected"
        }
        
        val selectablePhotos = photos.map { PhotoSelectionAdapter.SelectablePhoto(it, false) }
        photoSelectionAdapter.submitList(selectablePhotos)
        
        dialogBinding.recyclerViewPhotos.apply {
            layoutManager = GridLayoutManager(this@TripHistoryActivity, 3)
            adapter = photoSelectionAdapter
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Share") { _, _ ->
                val selectedPhotos = photoSelectionAdapter.getSelectedPhotos()
                lifecycleScope.launch {
                    shareWithPhotos(trip, selectedPhotos)
                }
            }
            .setNeutralButton("WhatsApp") { _, _ ->
                val selectedPhotos = photoSelectionAdapter.getSelectedPhotos()
                if (isWhatsAppAvailable()) {
                    lifecycleScope.launch {
                        shareDirectlyToWhatsApp(trip, selectedPhotos)
                    }
                } else {
                    android.widget.Toast.makeText(this, "WhatsApp not installed", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialogBinding.buttonSelectAll.setOnClickListener {
            photoSelectionAdapter.selectAll()
        }
        
        dialogBinding.buttonClearAll.setOnClickListener {
            photoSelectionAdapter.clearAll()
        }
        
        dialog.show()
    }
    
    private suspend fun shareWithPhotos(trip: com.vacationtracker.models.Trip, selectedPhotos: List<com.vacationtracker.models.Photo>) {
        try {
            val photoUris = mutableListOf<Uri>()
            
            // Convert photo file paths to content URIs
            selectedPhotos.forEach { photo ->
                val photoFile = File(photo.filePath)
                if (photoFile.exists()) {
                    try {
                        val photoUri = FileProvider.getUriForFile(
                            this,
                            "com.vacationtracker.fileprovider",
                            photoFile
                        )
                        photoUris.add(photoUri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            if (photoUris.isEmpty()) {
                android.widget.Toast.makeText(this, "No photos available to share", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            val shareIntent = Intent().apply {
                if (photoUris.size == 1) {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, photoUris[0])
                    type = "image/*"
                } else {
                    // WhatsApp optimization: Use ACTION_SEND_MULTIPLE for grouped photos
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(photoUris))
                    type = "image/*"
                    
                    // Add WhatsApp-specific optimizations
                    // Set package to specifically target WhatsApp if user chooses it
                    addCategory(Intent.CATEGORY_DEFAULT)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                // Grant permission to read the files
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Create chooser with WhatsApp optimization
            val chooserIntent = Intent.createChooser(shareIntent, "Share photos via...")
            
            // Add WhatsApp as preferred option if available
            if (isWhatsAppAvailable()) {
                val whatsappIntent = Intent(shareIntent).apply {
                    setPackage("com.whatsapp")
                }
                try {
                    if (whatsappIntent.resolveActivity(packageManager) != null) {
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(whatsappIntent))
                    }
                } catch (e: Exception) {
                    // WhatsApp not available, continue with regular chooser
                }
            }
            
            startActivity(chooserIntent)
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Error sharing photos: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun isWhatsAppAvailable(): Boolean {
        return try {
            packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun shareDirectlyToWhatsApp(trip: com.vacationtracker.models.Trip, selectedPhotos: List<com.vacationtracker.models.Photo>) {
        try {
            val photoUris = mutableListOf<Uri>()
            
            // Convert photo file paths to content URIs
            selectedPhotos.forEach { photo ->
                val photoFile = File(photo.filePath)
                if (photoFile.exists()) {
                    try {
                        val photoUri = FileProvider.getUriForFile(
                            this,
                            "com.vacationtracker.fileprovider",
                            photoFile
                        )
                        photoUris.add(photoUri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            if (photoUris.isEmpty()) {
                android.widget.Toast.makeText(this, "No photos available to share", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            val whatsappIntent = Intent().apply {
                setPackage("com.whatsapp")
                
                if (photoUris.size == 1) {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, photoUris[0])
                    type = "image/*"
                } else {
                    // WhatsApp specific: Send multiple photos as album/group
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(photoUris))
                    type = "image/*"
                    
                    // WhatsApp-specific flags for better grouping
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                // Grant permission to read the files
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Check if WhatsApp can handle this intent
            if (whatsappIntent.resolveActivity(packageManager) != null) {
                startActivity(whatsappIntent)
            } else {
                android.widget.Toast.makeText(this, "Cannot share to WhatsApp", android.widget.Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Error sharing to WhatsApp: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showRenameDialog(trip: com.vacationtracker.models.Trip) {
        val editText = EditText(this).apply {
            setText(trip.name)
            setSelectAllOnFocus(true)
            hint = "Enter trip name"
            setPadding(50, 40, 50, 40)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Rename Trip")
            .setMessage("Enter a new name for this trip:")
            .setView(editText)
            .setPositiveButton("Rename") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != trip.name) {
                    viewModel.renameTrip(trip.id, newName)
                    android.widget.Toast.makeText(this, "Trip renamed", android.widget.Toast.LENGTH_SHORT).show()
                } else if (newName.isEmpty()) {
                    android.widget.Toast.makeText(this, "Trip name cannot be empty", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_menu_edit)
            .show()
    }
    
    private fun showDeleteConfirmationDialog(trip: com.vacationtracker.models.Trip) {
        AlertDialog.Builder(this)
            .setTitle("Delete Trip")
            .setMessage("Are you sure you want to delete \"${trip.name}\"?\n\nThis will permanently delete all trip data including places visited, photos, and location history. This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTrip(trip.id)
                android.widget.Toast.makeText(this, "Trip deleted", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }
    
    private fun observeViewModel() {
        try {
            viewModel.allTrips.observe(this) { trips ->
                trips?.let {
                    tripAdapter.submitList(it)
                    
                    if (it.isEmpty()) {
                        binding.textEmptyState.visibility = android.view.View.VISIBLE
                        binding.recyclerViewTrips.visibility = android.view.View.GONE
                    } else {
                        binding.textEmptyState.visibility = android.view.View.GONE
                        binding.recyclerViewTrips.visibility = android.view.View.VISIBLE
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.textEmptyState.text = "Error loading trips: ${e.message}"
            binding.textEmptyState.visibility = android.view.View.VISIBLE
            binding.recyclerViewTrips.visibility = android.view.View.GONE
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}