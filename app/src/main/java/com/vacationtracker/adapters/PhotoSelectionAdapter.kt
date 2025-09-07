package com.vacationtracker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.vacationtracker.databinding.ItemPhotoSelectableBinding
import com.vacationtracker.models.Photo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class PhotoSelectionAdapter(
    private val onSelectionChanged: (Int) -> Unit
) : ListAdapter<PhotoSelectionAdapter.SelectablePhoto, PhotoSelectionAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    data class SelectablePhoto(
        val photo: Photo,
        var isSelected: Boolean = false
    )

    private val selectedPhotos = mutableSetOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoSelectableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getSelectedPhotos(): List<Photo> {
        return currentList.filter { it.isSelected }.map { it.photo }
    }

    fun selectAll() {
        val updatedList = currentList.map { it.copy(isSelected = true) }
        selectedPhotos.addAll(currentList.map { it.photo.id })
        submitList(updatedList)
        onSelectionChanged(selectedPhotos.size)
    }

    fun clearAll() {
        val updatedList = currentList.map { it.copy(isSelected = false) }
        selectedPhotos.clear()
        submitList(updatedList)
        onSelectionChanged(0)
    }

    inner class PhotoViewHolder(
        private val binding: ItemPhotoSelectableBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(selectablePhoto: SelectablePhoto) {
            val photo = selectablePhoto.photo
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
            
            binding.checkboxSelect.isChecked = selectablePhoto.isSelected
            
            binding.root.setOnClickListener {
                toggleSelection(selectablePhoto)
            }
            
            binding.checkboxSelect.setOnClickListener {
                toggleSelection(selectablePhoto)
            }
        }
        
        private fun toggleSelection(selectablePhoto: SelectablePhoto) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val updatedList = currentList.toMutableList()
                val updatedPhoto = selectablePhoto.copy(isSelected = !selectablePhoto.isSelected)
                updatedList[position] = updatedPhoto
                
                if (updatedPhoto.isSelected) {
                    selectedPhotos.add(updatedPhoto.photo.id)
                } else {
                    selectedPhotos.remove(updatedPhoto.photo.id)
                }
                
                submitList(updatedList)
                onSelectionChanged(selectedPhotos.size)
            }
        }
    }

    class PhotoDiffCallback : DiffUtil.ItemCallback<SelectablePhoto>() {
        override fun areItemsTheSame(oldItem: SelectablePhoto, newItem: SelectablePhoto): Boolean {
            return oldItem.photo.id == newItem.photo.id
        }

        override fun areContentsTheSame(oldItem: SelectablePhoto, newItem: SelectablePhoto): Boolean {
            return oldItem == newItem
        }
    }
}