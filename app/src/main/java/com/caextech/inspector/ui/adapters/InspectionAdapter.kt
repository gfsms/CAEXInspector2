package com.caextech.inspector.ui.adapters

import android.content.Intent
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
import com.caextech.inspector.ui.inspection.InspectionDetailActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying Inspection items in a RecyclerView.
 */
class InspectionAdapter(
    private val onItemClick: (InspeccionConCAEX) -> Unit,
    private val onDeleteClick: ((InspeccionConCAEX) -> Unit)? = null // Callback para eliminar
) : ListAdapter<InspeccionConCAEX, InspectionAdapter.InspectionViewHolder>(InspectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InspectionViewHolder {
        val binding = ItemInspectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InspectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InspectionViewHolder, position: Int) {
        val inspection = getItem(position)
        holder.bind(inspection)
    }

    inner class InspectionViewHolder(
        private val binding: ItemInspectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(inspection: InspeccionConCAEX) {
            val context = binding.root.context

            // Título de la inspección
            val tipoTexto = when (inspection.inspeccion.tipo) {
                Inspeccion.TIPO_RECEPCION -> "Recepción"
                Inspeccion.TIPO_ENTREGA -> "Entrega"
                else -> "Inspección"
            }

            binding.inspectionTitleText.text =
                "$tipoTexto - ${inspection.caex.getNombreCompleto()}"

            // Estado con color
            binding.statusText.text = when (inspection.inspeccion.estado) {
                Inspeccion.ESTADO_ABIERTA -> "Abierta"
                Inspeccion.ESTADO_PENDIENTE_CIERRE -> "Pendiente de Cierre"
                Inspeccion.ESTADO_CERRADA -> "Cerrada"
                else -> inspection.inspeccion.estado
            }

            val statusColor = when (inspection.inspeccion.estado) {
                Inspeccion.ESTADO_ABIERTA -> ContextCompat.getColor(context, R.color.status_pending)
                Inspeccion.ESTADO_PENDIENTE_CIERRE -> ContextCompat.getColor(context, R.color.status_pendiente_cierre)
                Inspeccion.ESTADO_CERRADA -> ContextCompat.getColor(context, R.color.status_conforme)
                else -> ContextCompat.getColor(context, android.R.color.darker_gray)
            }
            binding.statusText.setTextColor(statusColor)

            // Inspector y fecha
            binding.inspectorText.text = inspection.inspeccion.nombreInspector

            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.dateText.text = sdf.format(Date(inspection.inspeccion.fechaCreacion))

            // Mostrar botón eliminar solo para inspecciones ABIERTA
            val puedeEliminar = inspection.inspeccion.estado == Inspeccion.ESTADO_ABIERTA && onDeleteClick != null
            binding.deleteButton.visibility = if (puedeEliminar) View.VISIBLE else View.GONE

            // Click listeners
            binding.root.setOnClickListener { onItemClick(inspection) }
            binding.continueButton.setOnClickListener { onItemClick(inspection) }

            if (puedeEliminar) {
                binding.deleteButton.setOnClickListener {
                    onDeleteClick?.invoke(inspection)
                }
            }
        }
    }

    class InspectionDiffCallback : DiffUtil.ItemCallback<InspeccionConCAEX>() {
        override fun areItemsTheSame(oldItem: InspeccionConCAEX, newItem: InspeccionConCAEX): Boolean {
            return oldItem.inspeccion.inspeccionId == newItem.inspeccion.inspeccionId
        }

        override fun areContentsTheSame(oldItem: InspeccionConCAEX, newItem: InspeccionConCAEX): Boolean {
            return oldItem == newItem
        }
    }
}