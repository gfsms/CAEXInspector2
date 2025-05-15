package com.caextech.inspector.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.caextech.inspector.data.models.CAEXConInfo
import com.caextech.inspector.databinding.ItemCaexBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying CAEX items with enriched information in a RecyclerView.
 */
class CAEXAdapter(
    private val onItemClick: (CAEXConInfo) -> Unit,
    private val onCreateInspectionClick: (CAEXConInfo) -> Unit
) : ListAdapter<CAEXConInfo, CAEXAdapter.CAEXViewHolder>(CAEXDiffCallback()) {

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

            binding.actionButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCreateInspectionClick(getItem(position))
                }
            }
        }

        fun bind(caexInfo: CAEXConInfo) {
            val caex = caexInfo.caex

            // Basic info
            binding.caexNumberText.text = caex.getNombreCompleto()
            binding.modelText.text = caex.modelo

            // Status indicator
            val statusColor = ContextCompat.getColor(binding.root.context, caexInfo.getEstadoColor())
            binding.statusIndicator.setColorFilter(statusColor)
            binding.statusIndicator.setImageResource(caexInfo.getEstadoIcono())

            // Status text
            binding.statusText.text = caexInfo.getEstadoDescripcion()
            binding.statusText.setTextColor(statusColor)

            // Inspection statistics
            binding.totalInspectionsText.text = caexInfo.totalInspecciones.toString()

            // Last inspection info
            if (caexInfo.fechaUltimaInspeccion != null) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fecha = sdf.format(Date(caexInfo.fechaUltimaInspeccion))
                val tipo = when(caexInfo.tipoUltimaInspeccion) {
                    "RECEPCION" -> "Recepción"
                    "ENTREGA" -> "Entrega"
                    else -> ""
                }
                binding.lastInspectionText.text = "$fecha - $tipo"
            } else {
                binding.lastInspectionText.text = "Sin inspecciones"
            }

            // Action button state
            when {
                caexInfo.tieneInspeccionPendiente -> {
                    binding.actionButton.setImageResource(android.R.drawable.ic_menu_edit)
                    binding.actionButton.contentDescription = "Continuar inspección"
                }
                caexInfo.estadoUltimaInspeccion == "PENDIENTE_CIERRE" -> {
                    binding.actionButton.setImageResource(android.R.drawable.ic_menu_upload)
                    binding.actionButton.contentDescription = "Crear inspección de entrega"
                }
                else -> {
                    binding.actionButton.setImageResource(android.R.drawable.ic_menu_add)
                    binding.actionButton.contentDescription = "Crear nueva inspección"
                }
            }
        }
    }

    class CAEXDiffCallback : DiffUtil.ItemCallback<CAEXConInfo>() {
        override fun areItemsTheSame(oldItem: CAEXConInfo, newItem: CAEXConInfo): Boolean {
            return oldItem.caex.caexId == newItem.caex.caexId
        }

        override fun areContentsTheSame(oldItem: CAEXConInfo, newItem: CAEXConInfo): Boolean {
            return oldItem == newItem
        }
    }
}