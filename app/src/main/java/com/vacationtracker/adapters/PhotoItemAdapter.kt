package com.vacationtracker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.vacationtracker.databinding.ItemPhotoSimpleBinding
import com.vacationtracker.models.Photo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class PhotoItemAdapter(
    private val onPhotoClick: (Photo) -> Unit
) : ListAdapter<Photo, PhotoItemAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoSimpleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PhotoViewHolder(
        private val binding: ItemPhotoSimpleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: Photo) {
            val photoFile = File(photo.filePath)
            
            if (photoFile.exists()) {
                Glide.with(binding.imagePhoto.context)
                    .load(photoFile)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.imagePhoto)
            }
            
            val timeFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            binding.textPhotoTime.text = timeFormatter.format(photo.timestamp)
            
            binding.root.setOnClickListener {
                onPhotoClick(photo)
            }
        }
    }

    class PhotoDiffCallback : DiffUtil.ItemCallback<Photo>() {
        override fun areItemsTheSame(oldItem: Photo, newItem: Photo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Photo, newItem: Photo): Boolean {
            return oldItem == newItem
        }
    }
}