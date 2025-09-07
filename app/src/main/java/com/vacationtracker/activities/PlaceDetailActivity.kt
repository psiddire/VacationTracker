package com.vacationtracker.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.vacationtracker.adapters.PhotoAdapter
import com.vacationtracker.database.VacationDatabase
import com.vacationtracker.databinding.ActivityPlaceDetailBinding
import com.vacationtracker.models.Photo
import com.vacationtracker.repositories.PhotoRepository
import com.vacationtracker.repositories.PlaceRepository
import com.vacationtracker.utils.PhotoUtils
import com.vacationtracker.viewmodels.PlaceDetailViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PlaceDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPlaceDetailBinding
    private lateinit var photoAdapter: PhotoAdapter
    private var currentPhotoPath: String? = null
    private var currentPhotoFile: File? = null
    
    private val viewModel: PlaceDetailViewModel by viewModels {
        val database = VacationDatabase.getDatabase(this)
        val placeId = intent.getLongExtra("place_id", -1L)
        PlaceDetailViewModel.Factory(
            placeId,
            PlaceRepository(database.placeDao()),
            PhotoRepository(database.photoDao())
        )
    }
    
    // Camera permission launcher
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission required to take photos", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Camera launcher
    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoFile != null) {
            // Save photo to database
            viewModel.addPhoto(currentPhotoPath!!)
        }
    }
    
    // Gallery launcher
    private val pickFromGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Copy the selected image to our app's directory
            val photoFile = PhotoUtils.createImageFile(this)
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    photoFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.addPhoto(photoFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupPhotoGrid()
        setupFAB()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Place Details"
    }
    
    private fun setupPhotoGrid() {
        photoAdapter = PhotoAdapter { photo ->
            // TODO: Open photo in full screen
            Toast.makeText(this, "Photo: ${photo.caption ?: "No caption"}", Toast.LENGTH_SHORT).show()
        }
        
        binding.recyclerViewPhotos.apply {
            layoutManager = GridLayoutManager(this@PlaceDetailActivity, 2)
            adapter = photoAdapter
        }
    }
    
    private fun setupFAB() {
        binding.fabAddPhoto.setOnClickListener {
            showPhotoOptions()
        }
    }
    
    private fun showPhotoOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndTakePhoto()
                    1 -> pickFromGallery.launch("image/*")
                }
            }
            .show()
    }
    
    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == 
                PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun openCamera() {
        try {
            currentPhotoFile = PhotoUtils.createImageFile(this)
            currentPhotoPath = currentPhotoFile!!.absolutePath
            
            val photoUri = FileProvider.getUriForFile(
                this,
                "com.vacationtracker.fileprovider",
                currentPhotoFile!!
            )
            
            takePicture.launch(photoUri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeViewModel() {
        viewModel.place.observe(this) { place ->
            place?.let {
                supportActionBar?.title = it.name
                binding.textPlaceName.text = it.name
                binding.textPlaceAddress.text = it.address ?: "Unknown location"
                
                val dateFormatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                binding.textVisitedAt.text = "Visited: ${dateFormatter.format(it.visitedAt)}"
                
                if (it.notes.isNullOrBlank()) {
                    binding.textPlaceNotes.visibility = android.view.View.GONE
                } else {
                    binding.textPlaceNotes.text = it.notes
                    binding.textPlaceNotes.visibility = android.view.View.VISIBLE
                }
                
                if (it.rating != null && it.rating > 0) {
                    binding.ratingBar.rating = it.rating
                    binding.ratingBar.visibility = android.view.View.VISIBLE
                } else {
                    binding.ratingBar.visibility = android.view.View.GONE
                }
            }
        }
        
        viewModel.photos.observe(this) { photos ->
            photoAdapter.submitList(photos)
            
            if (photos.isEmpty()) {
                binding.textNoPhotos.visibility = android.view.View.VISIBLE
            } else {
                binding.textNoPhotos.visibility = android.view.View.GONE
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}