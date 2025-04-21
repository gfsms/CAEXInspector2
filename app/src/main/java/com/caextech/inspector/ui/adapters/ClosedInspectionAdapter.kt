package com.caextech.inspector.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.caextech.inspector.R
import com.caextech.inspector.data.entities.Inspeccion
import com.caextech.inspector.data.relations.InspeccionConCAEX
import com.caextech.inspector.databinding.ItemClosedInspectionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying closed inspections in a RecyclerView.
 * Groups related reception and delivery inspections together.
 */
class ClosedInspectionAdapter(
    private val onItemClick: (InspeccionConCAEX) -> Unit
) : ListAdapter<ClosedInspectionAdapter.InspectionItem, RecyclerView.ViewHolder>(InspectionDiffCallback()) {

    // ViewType constants
    companion object {
        private const val VIEWTYPE_SINGLE = 0
        private const val VIEWTYPE_GROUP = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEWTYPE_SINGLE -> {
                // Single inspection view holder
                val binding = ItemClosedInspectionBinding.inflate(inflater, parent, false)
                SingleInspectionViewHolder(binding)
            }
            VIEWTYPE_GROUP -> {
                // Grouped inspection view holder
                val binding = ItemClosedInspectionBinding.inflate(inflater, parent, false)
                GroupedInspectionViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is SingleInspectionViewHolder -> holder.bind(item as InspectionItem.Single)
            is GroupedInspectionViewHolder -> holder.bind(item as InspectionItem.Group)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is InspectionItem.Single -> VIEWTYPE_SINGLE
            is InspectionItem.Group -> VIEWTYPE_GROUP
        }
    }

    /**
     * Updates the list by grouping related reception and delivery inspections.
     * This method processes the raw inspection list to group related inspections.
     */
    fun submitInspectionList(inspections: List<InspeccionConCAEX>) {
        // Map to group delivery inspections by their reception ID
        val deliveryByReception = mutableMapOf<Long, InspeccionConCAEX>()

        // First pass: find all delivery inspections and map them by reception ID
        inspections.forEach { inspeccion ->
            inspeccion.inspeccion.inspeccionRecepcionId?.let { recepcionId ->
                deliveryByReception[recepcionId] = inspeccion
            }
        }

        // Second pass: build the grouped items list
        val groupedList = mutableListOf<InspectionItem>()

        inspections.forEach { inspeccion ->
            if (inspeccion.inspeccion.tipo == Inspeccion.TIPO_RECEPCION) {
                // Check if this reception has a matching delivery
                val delivery = deliveryByReception[inspeccion.inspeccion.inspeccionId]

                if (delivery != null) {
                    // Add as a group
                    groupedList.add(InspectionItem.Group(inspeccion, delivery))
                } else {
                    // Add as a single item
                    groupedList.add(InspectionItem.Single(inspeccion))
                }
            } else if (inspeccion.inspeccion.inspeccionRecepcionId == null) {
                // This is a delivery with no reception reference, add as single
                groupedList.add(InspectionItem.Single(inspeccion))
            }
            // Skip delivery inspections that were already included in a group
        }

        // Submit the processed list
        submitList(groupedList)
    }

    /**
     * ViewHolder for single inspections.
     */
    inner class SingleInspectionViewHolder(private val binding: ItemClosedInspectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position) as InspectionItem.Single
                    onItemClick(item.inspection)
                }
            }
        }

        fun bind(item: InspectionItem.Single) {
            val inspeccion = item.inspection.inspeccion

            // Hide group container and group elements
            binding.groupLayout.visibility = View.GONE

            // Set primary inspection details
            binding.inspectionTitleText.text = item.inspection.getTituloDescriptivo()
            binding.inspectorText.text = inspeccion.nombreInspector

            // Set status
            binding.statusText.text = "Estado: Cerrada"
            binding.statusText.setTextColor(ContextCompat.getColor(binding.root.context, R.color.colorPrimary))

            // Format date
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.dateText.text = sdf.format(Date(inspeccion.fechaCreacion))
        }
    }

    /**
     * ViewHolder for grouped inspections (reception + delivery).
     */
    inner class GroupedInspectionViewHolder(private val binding: ItemClosedInspectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position) as InspectionItem.Group
                    // Can customize which one is clicked, or handle both
                    onItemClick(item.reception)
                }
            }

            binding.recepcionCard.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position) as InspectionItem.Group
                    onItemClick(item.reception)
                }
            }

            binding.entregaCard.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position) as InspectionItem.Group
                    onItemClick(item.delivery)
                }
            }
        }

        fun bind(item: InspectionItem.Group) {
            // Hide single inspection details
            binding.inspectionTitleText.visibility = View.GONE
            binding.statusText.visibility = View.GONE
            binding.inspectorText.visibility = View.GONE
            binding.dateText.visibility = View.GONE

            // Show group layout
            binding.groupLayout.visibility = View.VISIBLE

            // Format date
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            // Set reception details
            binding.recepcionTitleText.text = "Recepción: ${item.reception.caex.getNombreCompleto()}"
            binding.recepcionDateText.text = sdf.format(Date(item.reception.inspeccion.fechaCreacion))

            // Set delivery details
            binding.entregaTitleText.text = "Entrega: ${item.delivery.caex.getNombreCompleto()}"
            binding.entregaDateText.text = sdf.format(Date(item.delivery.inspeccion.fechaCreacion))
        }
    }

    /**
     * Sealed class to represent different types of inspection items.
     */
    sealed class InspectionItem {
        /**
         * Represents a single inspection.
         */
        data class Single(val inspection: InspeccionConCAEX) : InspectionItem()

        /**
         * Represents a group of related reception and delivery inspections.
         */
        data class Group(val reception: InspeccionConCAEX, val delivery: InspeccionConCAEX) : InspectionItem()
    }

    /**
     * DiffUtil callback for InspectionItem.
     */
    class InspectionDiffCallback : DiffUtil.ItemCallback<InspectionItem>() {
        override fun areItemsTheSame(oldItem: InspectionItem, newItem: InspectionItem): Boolean {
            return when {
                oldItem is InspectionItem.Single && newItem is InspectionItem.Single ->
                    oldItem.inspection.inspeccion.inspeccionId == newItem.inspection.inspeccion.inspeccionId
                oldItem is InspectionItem.Group && newItem is InspectionItem.Group ->
                    oldItem.reception.inspeccion.inspeccionId == newItem.reception.inspeccion.inspeccionId
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: InspectionItem, newItem: InspectionItem): Boolean {
            return when {
                oldItem is InspectionItem.Single && newItem is InspectionItem.Single ->
                    oldItem.inspection.inspeccion == newItem.inspection.inspeccion &&
                            oldItem.inspection.caex == newItem.inspection.caex
                oldItem is InspectionItem.Group && newItem is InspectionItem.Group ->
                    oldItem.reception.inspeccion == newItem.reception.inspeccion &&
                            oldItem.reception.caex == newItem.reception.caex &&
                            oldItem.delivery.inspeccion == newItem.delivery.inspeccion &&
                            oldItem.delivery.caex == newItem.delivery.caex
                else -> false
            }
        }
    }
}