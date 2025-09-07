package com.vacationtracker.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vacationtracker.adapters.PlaceAdapter
import com.vacationtracker.database.VacationDatabase
import com.vacationtracker.databinding.ActivityItineraryBinding
import com.vacationtracker.repositories.PlaceRepository
import com.vacationtracker.repositories.TripRepository
import com.vacationtracker.viewmodels.ItineraryViewModel

class ItineraryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityItineraryBinding
    private lateinit var placeAdapter: PlaceAdapter
    
    private val viewModel: ItineraryViewModel by viewModels {
        val database = VacationDatabase.getDatabase(this)
        ItineraryViewModel.Factory(
            TripRepository(database.tripDao()),
            PlaceRepository(database.placeDao())
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItineraryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Trip Itinerary"
    }
    
    private fun setupRecyclerView() {
        placeAdapter = PlaceAdapter { place ->
            // Open place detail view
            val intent = Intent(this, PlaceDetailActivity::class.java)
            intent.putExtra("place_id", place.id)
            startActivity(intent)
        }
        
        binding.recyclerViewPlaces.apply {
            layoutManager = LinearLayoutManager(this@ItineraryActivity)
            adapter = placeAdapter
        }
    }
    
    private fun observeViewModel() {
        viewModel.currentTrip.observe(this) { trip ->
            supportActionBar?.title = trip?.name ?: "Trip Itinerary"
        }
        
        viewModel.places.observe(this) { places ->
            placeAdapter.submitList(places)
            
            if (places.isEmpty()) {
                binding.textEmptyState.visibility = android.view.View.VISIBLE
                binding.recyclerViewPlaces.visibility = android.view.View.GONE
            } else {
                binding.textEmptyState.visibility = android.view.View.GONE
                binding.recyclerViewPlaces.visibility = android.view.View.VISIBLE
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}