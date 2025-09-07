package com.vacationtracker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vacationtracker.R
import com.vacationtracker.databinding.ItemTripBinding
import com.vacationtracker.models.Trip
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class TripAdapter(
    private val onTripClick: (Trip) -> Unit,
    private val onShareClick: (Trip) -> Unit,
    private val onRenameClick: (Trip) -> Unit,
    private val onDeleteClick: (Trip) -> Unit
) : ListAdapter<Trip, TripAdapter.TripViewHolder>(TripDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding, onTripClick, onShareClick, onRenameClick, onDeleteClick)
    }
    
    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class TripViewHolder(
        private val binding: ItemTripBinding,
        private val onTripClick: (Trip) -> Unit,
        private val onShareClick: (Trip) -> Unit,
        private val onRenameClick: (Trip) -> Unit,
        private val onDeleteClick: (Trip) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        fun bind(trip: Trip) {
            binding.apply {
                textTripName.text = trip.name
                textStartDate.text = "Started: ${dateFormatter.format(trip.startDate)} at ${timeFormatter.format(trip.startDate)}"
                
                if (trip.endDate != null) {
                    textEndDate.text = "Ended: ${dateFormatter.format(trip.endDate)} at ${timeFormatter.format(trip.endDate)}"
                    textEndDate.visibility = android.view.View.VISIBLE
                    
                    // Calculate actual duration
                    val durationMillis = trip.endDate.time - trip.startDate.time
                    val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
                    textDuration.text = "Duration: ${hours}h ${minutes}m"
                } else {
                    textEndDate.visibility = android.view.View.GONE
                    textDuration.text = "Duration: Active trip"
                }
                
                textDistance.text = "Distance: ${String.format("%.1f", trip.totalDistance / 1000)} km"
                
                if (trip.description.isNullOrBlank()) {
                    textDescription.visibility = android.view.View.GONE
                } else {
                    textDescription.visibility = android.view.View.VISIBLE
                    textDescription.text = trip.description
                }
                
                // Set status indicator
                if (trip.isActive) {
                    indicatorStatus.setBackgroundResource(R.drawable.status_active)
                    textStatus.text = "Active"
                    textStatus.setTextColor(binding.root.context.getColor(R.color.success))
                    buttonDelete.visibility = android.view.View.GONE // Don't allow deleting active trips
                } else {
                    indicatorStatus.setBackgroundResource(R.drawable.status_completed)
                    textStatus.text = "Completed"
                    textStatus.setTextColor(binding.root.context.getColor(R.color.adventure_purple))
                    buttonDelete.visibility = android.view.View.VISIBLE // Allow deleting completed trips
                }
                
                root.setOnClickListener { onTripClick(trip) }
                buttonShare.setOnClickListener { onShareClick(trip) }
                buttonRename.setOnClickListener { onRenameClick(trip) }
                buttonDelete.setOnClickListener { onDeleteClick(trip) }
            }
        }
    }
    
    private class TripDiffCallback : DiffUtil.ItemCallback<Trip>() {
        override fun areItemsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem == newItem
        }
    }
}