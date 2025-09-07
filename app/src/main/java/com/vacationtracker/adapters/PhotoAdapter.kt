package com.vacationtracker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vacationtracker.databinding.ItemPhotoBinding
import com.vacationtracker.models.Photo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class PhotoAdapter(
    private val onPhotoClick: (Photo) -> Unit
) : ListAdapter<Photo, PhotoAdapter.PhotoViewHolder>(PhotoDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding, onPhotoClick)
    }
    
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class PhotoViewHolder(
        private val binding: ItemPhotoBinding,
        private val onPhotoClick: (Photo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val timeFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        
        fun bind(photo: Photo) {
            binding.apply {
                // Load image using Glide
                val photoFile = File(photo.filePath)
                if (photoFile.exists()) {
                    Glide.with(itemView.context)
                        .load(photoFile)
                        .centerCrop()
                        .into(imagePhoto)
                } else {
                    // Fallback if file doesn't exist
                    imagePhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                }
                
                // Set timestamp
                textTimestamp.text = timeFormatter.format(photo.timestamp)
                
                // Set caption if available
                if (photo.caption.isNullOrBlank()) {
                    textCaption.visibility = android.view.View.GONE
                } else {
                    textCaption.text = photo.caption
                    textCaption.visibility = android.view.View.VISIBLE
                }
                
                root.setOnClickListener { onPhotoClick(photo) }
            }
        }
    }
    
    private class PhotoDiffCallback : DiffUtil.ItemCallback<Photo>() {
        override fun areItemsTheSame(oldItem: Photo, newItem: Photo): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Photo, newItem: Photo): Boolean {
            return oldItem == newItem
        }
    }
}