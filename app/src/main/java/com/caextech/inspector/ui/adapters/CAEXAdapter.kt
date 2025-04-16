package com.caextech.inspector.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.caextech.inspector.data.entities.CAEX
import com.caextech.inspector.databinding.ItemCaexBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying CAEX items in a RecyclerView.
 */
class CAEXAdapter(private val onItemClick: (CAEX) -> Unit) :
    ListAdapter<CAEX, CAEXAdapter.CAEXViewHolder>(CAEXDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CAEXViewHolder {
        val binding = ItemCaexBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CAEXViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CAEXViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CAEXViewHolder(private val binding: ItemCaexBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(caex: CAEX) {
            binding.caexNumberText.text = caex.getNombreCompleto()
            binding.modelText.text = caex.modelo

            // Format date
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.registeredDateText.text = sdf.format(Date(caex.fechaRegistro))
        }
    }

    class CAEXDiffCallback : DiffUtil.ItemCallback<CAEX>() {
        override fun areItemsTheSame(oldItem: CAEX, newItem: CAEX): Boolean {
            return oldItem.caexId == newItem.caexId
        }

        override fun areContentsTheSame(oldItem: CAEX, newItem: CAEX): Boolean {
            return oldItem == newItem
        }
    }
}