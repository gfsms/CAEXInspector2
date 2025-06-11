package com.caextech.inspector.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.caextech.inspector.R
import com.caextech.inspector.data.entities.Respuesta
import com.caextech.inspector.data.entities.getCategoriaName
import com.caextech.inspector.data.relations.RespuestaConDetalles
import com.caextech.inspector.databinding.ItemHistoryHallazgoBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryDateAdapter : ListAdapter<RespuestaConDetalles, HistoryDateAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryHallazgoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemHistoryHallazgoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RespuestaConDetalles) {
            // Fecha
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.fechaText.text = sdf.format(Date(item.respuesta.fechaCreacion))

            // Estado
            binding.estadoText.text = when(item.respuesta.estado) {
                Respuesta.ESTADO_NO_CONFORME -> "NO CONFORME"
                Respuesta.ESTADO_RECHAZADO -> "RECHAZADO"
                else -> item.respuesta.estado
            }

            val colorEstado = when(item.respuesta.estado) {
                Respuesta.ESTADO_NO_CONFORME -> R.color.status_no_conforme
                Respuesta.ESTADO_RECHAZADO -> R.color.status_no_conforme
                else -> R.color.status_pending
            }
            binding.estadoText.setTextColor(ContextCompat.getColor(binding.root.context, colorEstado))

            // Categoría y pregunta
            binding.categoriaText.text = item.pregunta.getCategoriaName()
            binding.preguntaText.text = item.pregunta.texto

            // Comentarios
            binding.comentariosText.text = item.respuesta.comentarios

            // Info SAP
            if (!item.respuesta.tipoAccion.isNullOrEmpty() && !item.respuesta.idAvisoOrdenTrabajo.isNullOrEmpty()) {
                binding.sapInfoLayout.visibility = View.VISIBLE
                val tipoTexto = if (item.respuesta.tipoAccion == Respuesta.ACCION_INMEDIATO) "Aviso" else "OT"
                binding.sapText.text = "$tipoTexto: ${item.respuesta.idAvisoOrdenTrabajo}"
            } else {
                binding.sapInfoLayout.visibility = View.GONE
            }

            // Fotos
            if (item.tieneFotos()) {
                binding.photosRecyclerView.visibility = View.VISIBLE
                val photoAdapter = PhotoThumbnailAdapter { /* readonly */ }
                binding.photosRecyclerView.apply {
                    layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                    adapter = photoAdapter
                }
                photoAdapter.updatePhotos(item.fotos)
            } else {
                binding.photosRecyclerView.visibility = View.GONE
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
