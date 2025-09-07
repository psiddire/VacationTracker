package com.vacationtracker.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.vacationtracker.R
import com.vacationtracker.activities.MainActivity
import com.vacationtracker.database.VacationDatabase
import com.vacationtracker.models.LocationPoint
import com.vacationtracker.repositories.TripRepository
import kotlinx.coroutines.*
import java.util.Date

class LocationTrackingService : Service() {
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var database: VacationDatabase
    private lateinit var tripRepository: TripRepository
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        const val CHANNEL_ID = "LocationTrackingChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START_TRACKING = "START_TRACKING"
        const val ACTION_STOP_TRACKING = "STOP_TRACKING"
        
        // Location update parameters
        const val UPDATE_INTERVAL = 30000L // 30 seconds
        const val FASTEST_UPDATE_INTERVAL = 15000L // 15 seconds
        const val MIN_DISTANCE_CHANGE = 10f // 10 meters
    }
    
    override fun onCreate() {
        super.onCreate()
        
        database = VacationDatabase.getDatabase(this)
        tripRepository = TripRepository(database.tripDao())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        createNotificationChannel()
        setupLocationCallback()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startLocationTracking()
            ACTION_STOP_TRACKING -> stopLocationTracking()
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Tracks your location during vacation"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vacation Tracker")
            .setContentText("Recording your vacation route...")
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                
                locationResult.lastLocation?.let { location ->
                    serviceScope.launch {
                        saveLocationPoint(location)
                    }
                }
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
            setMinUpdateDistanceMeters(MIN_DISTANCE_CHANGE)
            setWaitForAccurateLocation(false)
        }.build()
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: SecurityException) {
            // Location permission not granted
            stopSelf()
        }
    }
    
    private fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private suspend fun saveLocationPoint(location: Location) {
        try {
            val activeTrip = tripRepository.getActiveTrip()
            if (activeTrip != null) {
                val locationPoint = LocationPoint(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = Date(),
                    accuracy = location.accuracy,
                    altitude = if (location.hasAltitude()) location.altitude else null,
                    speed = if (location.hasSpeed()) location.speed else null,
                    bearing = if (location.hasBearing()) location.bearing else null,
                    tripId = activeTrip.id
                )
                
                database.locationDao().insertLocation(locationPoint)
                
                // Update trip statistics
                updateTripStatistics(activeTrip.id)
            }
        } catch (e: Exception) {
            // Log error but continue tracking
            e.printStackTrace()
        }
    }
    
    private suspend fun updateTripStatistics(tripId: Long) {
        try {
            val locations = database.locationDao().getLocationsByTripSync(tripId)
            if (locations.size >= 2) {
                var totalDistance = 0.0
                
                for (i in 1 until locations.size) {
                    val prev = locations[i - 1]
                    val current = locations[i]
                    
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        prev.latitude, prev.longitude,
                        current.latitude, current.longitude,
                        results
                    )
                    totalDistance += results[0]
                }
                
                val trip = database.tripDao().getTripById(tripId)
                trip?.let {
                    val updatedTrip = it.copy(
                        totalDistance = totalDistance,
                        totalDuration = System.currentTimeMillis() - it.startDate.time
                    )
                    database.tripDao().updateTrip(updatedTrip)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}