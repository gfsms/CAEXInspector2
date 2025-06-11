package com.caextech.inspector.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caextech.inspector.databinding.ItemHistoryCategoryBinding
import com.caextech.inspector.data.relations.RespuestaConDetalles
import com.caextech.inspector.data.entities.getCategoriaName

class HistoryCategoryAdapter : RecyclerView.Adapter<HistoryCategoryAdapter.ViewHolder>() {

    private var categoryItems: List<Pair<String, List<RespuestaConDetalles>>> = emptyList()

    fun submitList(newItems: List<Pair<String, List<RespuestaConDetalles>>>) {
        categoryItems = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categoryItems[position])
    }

    override fun getItemCount(): Int = categoryItems.size


    class ViewHolder(private val binding: ItemHistoryCategoryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Pair<String, List<RespuestaConDetalles>>) {
            val (categoria, hallazgos) = item

            binding.categoryTitle.text = "$categoria (${hallazgos.size})"

            val historyDateAdapter = HistoryDateAdapter() // Renamed for clarity
            binding.itemsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                this.adapter = historyDateAdapter // Corrected assignment
            }
            historyDateAdapter.submitList(hallazgos) // Submit list to the correct adapter instance
        }
    }
}