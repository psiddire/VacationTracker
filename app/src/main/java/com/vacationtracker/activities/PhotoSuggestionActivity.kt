package com.vacationtracker.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.vacationtracker.database.VacationDatabase
import com.vacationtracker.databinding.DialogPhotoSuggestionBinding
import com.vacationtracker.repositories.PhotoRepository
import com.vacationtracker.repositories.PlaceRepository
import com.vacationtracker.repositories.TripRepository
import com.vacationtracker.services.PhotoMonitoringService
import com.vacationtracker.utils.PhotoLocationUtils
import kotlinx.coroutines.launch

class PhotoSuggestionActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_PHOTO_URI = "photo_uri"
        const val EXTRA_TRIP_ID = "trip_id"
        const val EXTRA_PLACE_NAME = "place_name"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val photoUriString = intent.getStringExtra(EXTRA_PHOTO_URI)
        val tripId = intent.getLongExtra(EXTRA_TRIP_ID, -1L)
        val placeName = intent.getStringExtra(EXTRA_PLACE_NAME)
        
        if (photoUriString != null && tripId != -1L) {
            showPhotoSuggestionDialog(Uri.parse(photoUriString), tripId, placeName)
        } else {
            finish()
        }
    }
    
    private fun showPhotoSuggestionDialog(photoUri: Uri, tripId: Long, placeName: String?) {
        val binding = DialogPhotoSuggestionBinding.inflate(layoutInflater)
        
        // Load photo preview
        Glide.with(this)
            .load(photoUri)
            .centerCrop()
            .into(binding.imagePhotoPreview)
        
        // Set text
        val message = if (placeName != null) {
            "This photo was taken near $placeName during your trip. Would you like to add it?"
        } else {
            "This photo was taken during your trip. Would you like to add it?"
        }
        binding.textMessage.text = message
        
        val dialog = AlertDialog.Builder(this)
            .setView(binding.root)
            .setTitle("Add Photo to Trip")
            .setPositiveButton("Add to Trip") { _, _ ->
                addPhotoToTrip(photoUri, tripId)
            }
            .setNegativeButton("Not Now") { _, _ ->
                finish()
            }
            .setOnDismissListener {
                finish()
            }
            .create()
        
        dialog.show()
    }
    
    private fun addPhotoToTrip(photoUri: Uri, tripId: Long) {
        lifecycleScope.launch {
            try {
                val database = VacationDatabase.getDatabase(this@PhotoSuggestionActivity)
                val photoRepository = PhotoRepository(database.photoDao())
                
                // Extract photo location data
                val photoData = PhotoLocationUtils.extractPhotoLocationData(this@PhotoSuggestionActivity, photoUri)
                
                // Get real path from URI (this is simplified - you might need more robust URI handling)
                val realPath = getRealPathFromUri(photoUri)
                
                if (realPath != null) {
                    // Add photo to database
                    photoRepository.addPhoto(
                        filePath = realPath,
                        tripId = tripId,
                        latitude = photoData?.latitude,
                        longitude = photoData?.longitude,
                        timestamp = photoData?.timestamp ?: java.util.Date()
                    )
                }
                
                finish()
            } catch (e: Exception) {
                // Handle error
                finish()
            }
        }
    }
    
    private fun getRealPathFromUri(uri: Uri): String? {
        // This is a simplified implementation
        // In a real app, you'd want more robust URI to path conversion
        return try {
            contentResolver.query(uri, arrayOf(android.provider.MediaStore.Images.Media.DATA), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA)
                    cursor.getString(columnIndex)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}