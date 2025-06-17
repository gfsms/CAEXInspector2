package com.caextech.inspector.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.caextech.inspector.R
import com.caextech.inspector.data.entities.getCategoriaName
import com.caextech.inspector.data.relations.RespuestaConDetalles
import com.caextech.inspector.databinding.ItemHallazgoSimpleBinding

class HallazgosAdapter : ListAdapter<RespuestaConDetalles, HallazgosAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHallazgoSimpleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemHallazgoSimpleBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RespuestaConDetalles) {
            // Categoría
            binding.categoriaText.text = item.pregunta.getCategoriaName()

            // Pregunta
            binding.preguntaText.text = item.pregunta.texto

            // Estado con color
            binding.estadoText.text = when(item.respuesta.estado) {
                "NO_CONFORME" -> "NO CONFORME"
                "RECHAZADO" -> "RECHAZADO"
                else -> item.respuesta.estado
            }

            val colorEstado = when(item.respuesta.estado) {
                "NO_CONFORME", "RECHAZADO" -> R.color.status_no_conforme
                else -> R.color.status_pending
            }
            binding.estadoText.setTextColor(ContextCompat.getColor(binding.root.context, colorEstado))

            // Comentarios
            binding.comentariosText.text = item.respuesta.comentarios.ifEmpty { "Sin comentarios" }

            // Indicador de fotos
            if (item.tieneFotos()) {
                binding.fotoIndicator.text = "${item.fotos.size} foto${if (item.fotos.size != 1) "s" else ""}"
                binding.fotoIndicator.setTextColor(ContextCompat.getColor(binding.root.context, R.color.colorPrimary))
            } else {
                binding.fotoIndicator.text = ""
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RespuestaConDetalles>() {
        override fun areItemsTheSame(oldItem: RespuestaConDetalles, newItem: RespuestaConDetalles): Boolean {
            return oldItem.respuesta.respuestaId == newItem.respuesta.respuestaId
        }

        override fun areContentsTheSame(oldItem: RespuestaConDetalles, newItem: RespuestaConDetalles): Boolean {
            return oldItem == newItem
        }
    }
}