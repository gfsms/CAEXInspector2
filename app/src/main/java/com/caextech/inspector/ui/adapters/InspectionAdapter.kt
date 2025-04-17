package com.caextech.inspector.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.caextech.inspector.R
import com.caextech.inspector.data.entities.Inspeccion
import com.caextech.inspector.data.relations.InspeccionConCAEX
import com.caextech.inspector.databinding.ItemInspectionBinding
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying Inspection items in a RecyclerView.
 */
class InspectionAdapter(private val onItemClick: (InspeccionConCAEX) -> Unit) :
    ListAdapter<InspeccionConCAEX, InspectionAdapter.InspectionViewHolder>(InspectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InspectionViewHolder {
        val binding = ItemInspectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InspectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InspectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InspectionViewHolder(private val binding: ItemInspectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.continueButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(inspeccionConCAEX: InspeccionConCAEX) {
            val inspeccion = inspeccionConCAEX.inspeccion

            // Set title with inspection type and CAEX info
            binding.inspectionTitleText.text = inspeccionConCAEX.getTituloDescriptivo()

            // Set status
            when (inspeccion.estado) {
                Inspeccion.ESTADO_ABIERTA -> {
                    binding.statusText.text = "Estado: Abierta"
                    binding.statusText.setTextColor(ContextCompat.getColor(binding.root.context, R.color.status_pending))
                }
                Inspeccion.ESTADO_PENDIENTE_CIERRE -> {
                    binding.statusText.text = "Estado: Pendiente de cierre"
                    binding.statusText.setTextColor(ContextCompat.getColor(binding.root.context, R.color.status_pendiente_cierre))
                }
                Inspeccion.ESTADO_CERRADA -> {
                    binding.statusText.text = "Estado: Cerrada"
                    binding.statusText.setTextColor(ContextCompat.getColor(binding.root.context, R.color.colorPrimary))
                }
                else -> {
                    binding.statusText.text = inspeccionConCAEX.getEstadoDescriptivo()
                }
            }

            // Set inspector name
            binding.inspectorText.text = inspeccion.nombreInspector

            // Format date
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.dateText.text = sdf.format(Date(inspeccion.fechaCreacion))

            // Show or hide continue button depending on inspection state
            binding.continueButton.visibility = if (inspeccion.estado != Inspeccion.ESTADO_CERRADA) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    class InspectionDiffCallback : DiffUtil.ItemCallback<InspeccionConCAEX>() {
        override fun areItemsTheSame(oldItem: InspeccionConCAEX, newItem: InspeccionConCAEX): Boolean {
            return oldItem.inspeccion.inspeccionId == newItem.inspeccion.inspeccionId
        }

        override fun areContentsTheSame(oldItem: InspeccionConCAEX, newItem: InspeccionConCAEX): Boolean {
            return oldItem.inspeccion == newItem.inspeccion && oldItem.caex == newItem.caex
        }
    }
}