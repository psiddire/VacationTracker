package com.vacationtracker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vacationtracker.databinding.ItemPlaceBinding
import com.vacationtracker.models.Place
import java.text.SimpleDateFormat
import java.util.Locale

class PlaceAdapter(
    private val onPlaceClick: (Place) -> Unit
) : ListAdapter<Place, PlaceAdapter.PlaceViewHolder>(PlaceDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemPlaceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaceViewHolder(binding, onPlaceClick)
    }
    
    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class PlaceViewHolder(
        private val binding: ItemPlaceBinding,
        private val onPlaceClick: (Place) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        
        fun bind(place: Place) {
            binding.apply {
                textPlaceName.text = place.name
                textPlaceAddress.text = place.address ?: "Unknown location"
                textVisitedAt.text = "Visited: ${dateFormatter.format(place.visitedAt)}"
                
                if (place.notes.isNullOrBlank()) {
                    textPlaceNotes.visibility = android.view.View.GONE
                } else {
                    textPlaceNotes.visibility = android.view.View.VISIBLE
                    textPlaceNotes.text = place.notes
                }
                
                if (place.placeType != null) {
                    textPlaceType.visibility = android.view.View.VISIBLE
                    textPlaceType.text = place.placeType
                } else {
                    textPlaceType.visibility = android.view.View.GONE
                }
                
                if (place.rating != null && place.rating > 0) {
                    ratingBar.visibility = android.view.View.VISIBLE
                    ratingBar.rating = place.rating
                } else {
                    ratingBar.visibility = android.view.View.GONE
                }
                
                root.setOnClickListener { onPlaceClick(place) }
            }
        }
    }
    
    private class PlaceDiffCallback : DiffUtil.ItemCallback<Place>() {
        override fun areItemsTheSame(oldItem: Place, newItem: Place): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Place, newItem: Place): Boolean {
            return oldItem == newItem
        }
    }
}