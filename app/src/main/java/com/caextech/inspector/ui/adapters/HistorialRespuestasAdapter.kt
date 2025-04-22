package com.caextech.inspector.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.caextech.inspector.R
import com.caextech.inspector.data.entities.Inspeccion
import com.caextech.inspector.data.entities.Respuesta
import com.caextech.inspector.data.relations.RespuestaConDetalles
import com.caextech.inspector.databinding.ItemHistorialRespuestaBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistorialRespuestasAdapter(
    private val onUsarIdSapClicked: (RespuestaConDetalles) -> Unit
) : ListAdapter<RespuestaConDetalles, HistorialRespuestasAdapter.HistorialViewHolder>(HistorialDiffCallback()) {

    private val photoAdapters = mutableMapOf<Long, PhotoThumbnailAdapter>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val binding = ItemHistorialRespuestaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistorialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistorialViewHolder(private val binding: ItemHistorialRespuestaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(respuestaConDetalles: RespuestaConDetalles) {
            val respuesta = respuestaConDetalles.respuesta

            // Configurar fecha
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.fechaInspeccionText.text = sdf.format(Date(respuesta.fechaCreacion))

            // Configurar tipo de inspección basado en el estado
            binding.tipoInspeccionText.text = when (respuesta.estado) {
                Respuesta.ESTADO_NO_CONFORME -> "Recepción"
                Respuesta.ESTADO_RECHAZADO -> "Entrega"
                else -> "Inspección"
            }
            binding.tipoInspeccionText.setBackgroundResource(
                if (respuesta.estado == Respuesta.ESTADO_NO_CONFORME)
                    R.color.status_no_conforme
                else
                    R.color.status_pendiente_cierre
            )

            // Configurar comentarios
            binding.comentariosText.text = respuesta.comentarios

            // Configurar información de SAP
            if (respuesta.tipoAccion != null && respuesta.idAvisoOrdenTrabajo != null) {
                val tipoAccionTexto = if (respuesta.tipoAccion == Respuesta.ACCION_INMEDIATO) {
                    "Aviso"
                } else {
                    "OT"
                }
                binding.sapInfoText.text = "$tipoAccionTexto: ${respuesta.idAvisoOrdenTrabajo}"
                binding.usarIdSapButton.visibility = View.VISIBLE
            } else {
                binding.sapInfoText.text = "No disponible"
                binding.usarIdSapButton.visibility = View.GONE
            }

            // Configurar botón
            binding.usarIdSapButton.setOnClickListener {
                onUsarIdSapClicked(respuestaConDetalles)
            }

            // Configurar sección de fotos
            if (respuestaConDetalles.fotos.isNotEmpty()) {
                setupPhotosRecyclerView(respuestaConDetalles)
                binding.fotosRecyclerView.visibility = View.VISIBLE
            } else {
                binding.fotosRecyclerView.visibility = View.GONE
            }
        }

        private fun setupPhotosRecyclerView(respuesta: RespuestaConDetalles) {
            val respuestaId = respuesta.respuesta.respuestaId

            if (!photoAdapters.containsKey(respuestaId)) {
                photoAdapters[respuestaId] = PhotoThumbnailAdapter(
                    onDeleteClicked = { /* Solo lectura, no se puede eliminar */ }
                )
            }

            val photoAdapter = photoAdapters[respuestaId]!!

            binding.fotosRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                adapter = photoAdapter
            }

            photoAdapter.updatePhotos(respuesta.fotos)
        }
    }

    class HistorialDiffCallback : DiffUtil.ItemCallback<RespuestaConDetalles>() {
        override fun areItemsTheSame(oldItem: RespuestaConDetalles, newItem: RespuestaConDetalles): Boolean {
            return oldItem.respuesta.respuestaId == newItem.respuesta.respuestaId
        }

        override fun areContentsTheSame(oldItem: RespuestaConDetalles, newItem: RespuestaConDetalles): Boolean {
            return oldItem.respuesta == newItem.respuesta &&
                    oldItem.pregunta == newItem.pregunta &&
                    oldItem.fotos == newItem.fotos
        }
    }
}