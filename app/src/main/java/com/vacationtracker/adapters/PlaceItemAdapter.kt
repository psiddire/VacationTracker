package com.vacationtracker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vacationtracker.databinding.ItemPlaceSimpleBinding
import com.vacationtracker.models.Place

class PlaceItemAdapter(
    private val onPlaceClick: (Place) -> Unit
) : ListAdapter<Place, PlaceItemAdapter.PlaceViewHolder>(PlaceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemPlaceSimpleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlaceViewHolder(
        private val binding: ItemPlaceSimpleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(place: Place) {
            binding.textPlaceName.text = place.name
            
            if (!place.address.isNullOrBlank()) {
                binding.textPlaceAddress.text = place.address
                binding.textPlaceAddress.visibility = android.view.View.VISIBLE
            } else {
                binding.textPlaceAddress.visibility = android.view.View.GONE
            }
            
            
            binding.root.setOnClickListener {
                onPlaceClick(place)
            }
        }
    }

    class PlaceDiffCallback : DiffUtil.ItemCallback<Place>() {
        override fun areItemsTheSame(oldItem: Place, newItem: Place): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Place, newItem: Place): Boolean {
            return oldItem == newItem
        }
    }
}