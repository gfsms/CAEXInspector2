package com.caextech.inspector.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.caextech.inspector.data.entities.Foto
import com.caextech.inspector.databinding.ItemPhotoThumbnailBinding
import java.io.File

/**
 * Adapter for displaying photo thumbnails in a RecyclerView.
 */
class PhotoThumbnailAdapter(
    private val onDeleteClicked: (Foto) -> Unit
) : RecyclerView.Adapter<PhotoThumbnailAdapter.PhotoViewHolder>() {

    private val photos = mutableListOf<Foto>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoThumbnailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount(): Int = photos.size

    /**
     * Updates the list of photos displayed by the adapter.
     */
    fun updatePhotos(newPhotos: List<Foto>) {
        photos.clear()
        photos.addAll(newPhotos)
        notifyDataSetChanged()
    }

    inner class PhotoViewHolder(private val binding: ItemPhotoThumbnailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Set up delete button listener
            binding.deletePhotoButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClicked(photos[position])
                }
            }
        }

        fun bind(foto: Foto) {
            // Load photo into ImageView
            Glide.with(binding.root.context)
                .load(File(foto.rutaArchivo))
                .centerCrop()
                .into(binding.photoImageView)
        }
    }
}