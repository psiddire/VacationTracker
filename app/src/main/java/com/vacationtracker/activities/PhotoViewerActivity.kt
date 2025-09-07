package com.vacationtracker.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.chrisbanes.photoview.PhotoView
import com.vacationtracker.databinding.ActivityPhotoViewerBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoViewerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPhotoViewerBinding
    
    companion object {
        const val EXTRA_PHOTO_PATH = "photo_path"
        const val EXTRA_PHOTO_TIMESTAMP = "photo_timestamp"
        const val EXTRA_PHOTO_CAPTION = "photo_caption"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Enable immersive full-screen mode
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
        
        setupUI()
        loadPhoto()
    }
    
    private fun setupUI() {
        // Configure PhotoView for zoom functionality
        binding.photoView.apply {
            // Enable zoom
            isZoomable = true
            
            // Set minimum and maximum scale
            minimumScale = 0.5f
            maximumScale = 5.0f
            
            // Set medium scale (double tap zoom level)
            mediumScale = 2.0f
            
            // Toggle UI visibility on single tap (PhotoView handles this differently)
            setOnPhotoTapListener { view, x, y ->
                toggleUIVisibility()
            }
            
            // Optional: Handle view tap (tap on areas outside the photo)
            setOnViewTapListener { view, x, y ->
                toggleUIVisibility()
            }
        }
        
        // Close button
        binding.btnClose.setOnClickListener {
            finish()
        }
        
        // Share button (optional for future enhancement)
        binding.btnShare.setOnClickListener {
            // TODO: Implement photo sharing
        }
    }
    
    private fun loadPhoto() {
        val photoPath = intent.getStringExtra(EXTRA_PHOTO_PATH)
        val timestamp = intent.getLongExtra(EXTRA_PHOTO_TIMESTAMP, 0L)
        val caption = intent.getStringExtra(EXTRA_PHOTO_CAPTION)
        
        if (photoPath != null) {
            val photoFile = File(photoPath)
            
            if (photoFile.exists()) {
                Glide.with(this)
                    .load(photoFile)
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.photoView)
            }
        }
        
        // Set photo info
        if (timestamp > 0) {
            val dateFormatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
            binding.textPhotoDate.text = dateFormatter.format(Date(timestamp))
        }
        
        if (!caption.isNullOrBlank()) {
            binding.textPhotoCaption.text = caption
            binding.textPhotoCaption.visibility = View.VISIBLE
        } else {
            binding.textPhotoCaption.visibility = View.GONE
        }
    }
    
    private fun toggleUIVisibility() {
        if (binding.layoutControls.visibility == View.VISIBLE) {
            // Hide UI
            binding.layoutControls.visibility = View.GONE
            binding.layoutPhotoInfo.visibility = View.GONE
        } else {
            // Show UI
            binding.layoutControls.visibility = View.VISIBLE
            binding.layoutPhotoInfo.visibility = View.VISIBLE
        }
    }
}