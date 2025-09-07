package com.vacationtracker.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.vacationtracker.R
import com.vacationtracker.activities.MainActivity
import com.vacationtracker.activities.PhotoSuggestionActivity
import com.vacationtracker.database.VacationDatabase
import com.vacationtracker.repositories.LocationRepository
import com.vacationtracker.repositories.PlaceRepository
import com.vacationtracker.repositories.TripRepository
import com.vacationtracker.utils.PhotoLocationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PhotoMonitoringService : Service() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var tripRepository: TripRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var placeRepository: PlaceRepository
    private var photoObserver: ContentObserver? = null
    
    companion object {
        const val ACTION_START_MONITORING = "START_PHOTO_MONITORING"
        const val ACTION_STOP_MONITORING = "STOP_PHOTO_MONITORING"
        const val ACTION_ADD_SUGGESTED_PHOTO = "ADD_SUGGESTED_PHOTO"
        
        const val EXTRA_PHOTO_URI = "photo_uri"
        const val EXTRA_TRIP_ID = "trip_id"
        
        private const val SUGGESTION_NOTIFICATION_ID = 1002
        private const val SUGGESTION_CHANNEL_ID = "photo_suggestion_channel"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        val database = VacationDatabase.getDatabase(this)
        tripRepository = TripRepository(database.tripDao())
        locationRepository = LocationRepository(database.locationDao())
        placeRepository = PlaceRepository(database.placeDao())
        
        createNotificationChannels()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startPhotoMonitoring()
            ACTION_STOP_MONITORING -> stopPhotoMonitoring()
            ACTION_ADD_SUGGESTED_PHOTO -> handleAddSuggestedPhoto(intent)
        }
        return START_STICKY
    }
    
    private fun startPhotoMonitoring() {
        // Note: Not using foreground service to avoid permission issues
        // This service will run in background and monitor photos during active trips
        
        // Register content observer for new photos
        photoObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                uri?.let { photoUri ->
                    serviceScope.launch {
                        checkNewPhoto(photoUri)
                    }
                }
            }
        }
        
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            photoObserver!!
        )
    }
    
    private fun stopPhotoMonitoring() {
        photoObserver?.let { observer ->
            contentResolver.unregisterContentObserver(observer)
            photoObserver = null
        }
        stopSelf()
    }
    
    private suspend fun checkNewPhoto(photoUri: Uri) {
        try {
            // Get current active trip
            val activeTrip = tripRepository.getActiveTrip() ?: return
            
            // Extract photo location data
            val photoData = PhotoLocationUtils.extractPhotoLocationData(this, photoUri) ?: return
            
            // Check if photo was taken during trip timeframe
            if (!PhotoLocationUtils.isPhotoTakenDuringTrip(
                    photoData.timestamp,
                    activeTrip.startDate,
                    activeTrip.endDate
                )) {
                return
            }
            
            // Get trip locations and places
            val tripLocations = locationRepository.getLocationsByTripSync(activeTrip.id)
            val tripPlaces = placeRepository.getPlacesByTripSync(activeTrip.id)
            
            // Check if photo is near trip locations
            if (PhotoLocationUtils.isPhotoNearTripLocation(photoData, tripLocations, tripPlaces)) {
                // Find closest place for context
                val closestPlace = PhotoLocationUtils.findClosestPlace(photoData, tripPlaces)
                
                // Show suggestion notification
                showPhotoSuggestionNotification(photoUri, activeTrip.id, closestPlace?.name)
            }
            
        } catch (e: Exception) {
            // Handle errors silently - don't interrupt user experience
        }
    }
    
    private fun handleAddSuggestedPhoto(intent: Intent) {
        val photoUriString = intent.getStringExtra(EXTRA_PHOTO_URI)
        val tripId = intent.getLongExtra(EXTRA_TRIP_ID, -1L)
        
        if (photoUriString != null && tripId != -1L) {
            serviceScope.launch {
                try {
                    val photoUri = Uri.parse(photoUriString)
                    val photoData = PhotoLocationUtils.extractPhotoLocationData(this@PhotoMonitoringService, photoUri)
                    
                    if (photoData != null) {
                        // Add photo to trip
                        // This would integrate with your existing photo management system
                        // For now, just dismiss the notification
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(SUGGESTION_NOTIFICATION_ID)
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Suggestion channel
            val suggestionChannel = NotificationChannel(
                SUGGESTION_CHANNEL_ID,
                "Photo Suggestions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Suggests adding photos to your trip"
            }
            
            notificationManager.createNotificationChannel(suggestionChannel)
        }
    }
    
    
    private fun showPhotoSuggestionNotification(photoUri: Uri, tripId: Long, placeName: String?) {
        val intent = Intent(this, PhotoSuggestionActivity::class.java).apply {
            putExtra(PhotoSuggestionActivity.EXTRA_PHOTO_URI, photoUri.toString())
            putExtra(PhotoSuggestionActivity.EXTRA_TRIP_ID, tripId)
            putExtra(PhotoSuggestionActivity.EXTRA_PLACE_NAME, placeName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = "Add Photo to Trip?"
        val text = if (placeName != null) {
            "Photo taken near $placeName - Add to your trip?"
        } else {
            "Photo taken during your trip - Add it?"
        }
        
        val notification = NotificationCompat.Builder(this, SUGGESTION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_camera)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_add,
                "Add to Trip",
                pendingIntent
            )
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SUGGESTION_NOTIFICATION_ID, notification)
    }
    
    private fun createMainActivityIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}