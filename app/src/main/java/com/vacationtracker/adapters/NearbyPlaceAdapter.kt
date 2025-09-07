package com.vacationtracker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vacationtracker.databinding.ItemNearbyPlaceBinding
import com.vacationtracker.services.NearbyPlace

class NearbyPlaceAdapter(
    private val onPlaceClick: (NearbyPlace) -> Unit
) : ListAdapter<NearbyPlace, NearbyPlaceAdapter.NearbyPlaceViewHolder>(NearbyPlaceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NearbyPlaceViewHolder {
        val binding = ItemNearbyPlaceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NearbyPlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NearbyPlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NearbyPlaceViewHolder(
        private val binding: ItemNearbyPlaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(place: NearbyPlace) {
            binding.textPlaceName.text = place.name
            binding.textPlaceAddress.text = place.address ?: "Address not available"
            
            // Set place type (take first relevant type)
            val displayType = getDisplayType(place.placeTypes)
            binding.textPlaceType.text = displayType
            
            // Set icon based on place type
            binding.textPlaceIcon.text = getPlaceIcon(place.placeTypes)
            
            // Show rating if available
            if (place.rating != null && place.rating > 0) {
                binding.textPlaceRating.text = "⭐ ${String.format("%.1f", place.rating)}"
                binding.textPlaceRating.visibility = android.view.View.VISIBLE
            } else {
                binding.textPlaceRating.visibility = android.view.View.GONE
            }
            
            // Show likelihood percentage
            val likelihoodPercent = (place.likelihood * 100).toInt()
            binding.textLikelihood.text = "$likelihoodPercent%"
            
            binding.root.setOnClickListener {
                onPlaceClick(place)
            }
        }
        
        private fun getDisplayType(placeTypes: List<String>): String {
            val typeMapping = mapOf(
                "restaurant" to "Restaurant",
                "tourist_attraction" to "Attraction", 
                "museum" to "Museum",
                "park" to "Park",
                "lodging" to "Hotel",
                "shopping_mall" to "Shopping",
                "store" to "Store",
                "cafe" to "Cafe",
                "bar" to "Bar",
                "night_club" to "Nightlife",
                "amusement_park" to "Amusement",
                "zoo" to "Zoo",
                "church" to "Religious",
                "stadium" to "Stadium",
                "movie_theater" to "Cinema",
                "bowling_alley" to "Bowling",
                "casino" to "Casino",
                "spa" to "Spa",
                "gym" to "Gym",
                "library" to "Library",
                "hospital" to "Hospital",
                "airport" to "Airport",
                "subway_station" to "Transit",
                "train_station" to "Transit",
                "gas_station" to "Gas Station"
            )
            
            for (type in placeTypes) {
                val displayType = typeMapping[type.lowercase()]
                if (displayType != null) {
                    return displayType
                }
            }
            
            return "Place"
        }
        
        private fun getPlaceIcon(placeTypes: List<String>): String {
            val iconMapping = mapOf(
                "restaurant" to "🍽️",
                "tourist_attraction" to "🎯", 
                "museum" to "🏛️",
                "park" to "🌳",
                "lodging" to "🏨",
                "shopping_mall" to "🛍️",
                "store" to "🏪",
                "cafe" to "☕",
                "bar" to "🍻",
                "night_club" to "🎵",
                "amusement_park" to "🎢",
                "zoo" to "🦁",
                "church" to "⛪",
                "stadium" to "🏟️",
                "movie_theater" to "🎬",
                "bowling_alley" to "🎳",
                "casino" to "🎰",
                "spa" to "💆",
                "gym" to "💪",
                "library" to "📚",
                "hospital" to "🏥",
                "airport" to "✈️",
                "subway_station" to "🚇",
                "train_station" to "🚂",
                "gas_station" to "⛽"
            )
            
            for (type in placeTypes) {
                val icon = iconMapping[type.lowercase()]
                if (icon != null) {
                    return icon
                }
            }
            
            return "📍"
        }
    }

    class NearbyPlaceDiffCallback : DiffUtil.ItemCallback<NearbyPlace>() {
        override fun areItemsTheSame(oldItem: NearbyPlace, newItem: NearbyPlace): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NearbyPlace, newItem: NearbyPlace): Boolean {
            return oldItem == newItem
        }
    }
}