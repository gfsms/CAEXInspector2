package com.caextech.inspector.ui.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.caextech.inspector.data.entities.Respuesta
import com.caextech.inspector.data.relations.RespuestaConDetalles
import com.caextech.inspector.databinding.ItemNoConformeBinding

/**
 * Adapter for displaying No Conforme responses in the summary.
 */
class NoConformeAdapter(
    private val onSapIdUpdated: (Long, String, String) -> Unit
) : ListAdapter<RespuestaConDetalles, NoConformeAdapter.NoConformeViewHolder>(NoConformeDiffCallback()) {

    private val incompleteItems = mutableSetOf<Long>()
    private val photoAdapters = mutableMapOf<Long, PhotoThumbnailAdapter>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoConformeViewHolder {
        val binding = ItemNoConformeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoConformeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoConformeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Returns a list of incomplete items (missing action type or SAP ID).
     */
    fun getIncompleteItems(): List<Long> {
        return incompleteItems.toList()
    }

    inner class NoConformeViewHolder(private val binding: ItemNoConformeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentRespuestaId: Long = 0
        private var currentTipoAccion: String = Respuesta.ACCION_INMEDIATO

        init {
            // Set up action type radio group listener
            binding.actionTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    currentTipoAccion = when (checkedId) {
                        binding.radioAviso.id -> Respuesta.ACCION_INMEDIATO
                        binding.radioOT.id -> Respuesta.ACCION_PROGRAMADO
                        else -> Respuesta.ACCION_INMEDIATO
                    }

                    // Update with current SAP ID value
                    updateRespuesta()
                }
            }

            // Set up SAP ID text change listener
            binding.sapIdEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        updateRespuesta()
                    }
                }
            })
        }

        fun bind(respuestaConDetalles: RespuestaConDetalles) {
            currentRespuestaId = respuestaConDetalles.respuesta.respuestaId

            // Set category and question text
            // Using the category name from elsewhere since the extension function isn't available
            binding.categoryTitleText.text = respuestaConDetalles.pregunta.categoriaId.toString()
            binding.questionText.text = respuestaConDetalles.pregunta.texto
            // Set comments
            binding.commentsText.text = respuestaConDetalles.respuesta.comentarios

            // Set action type based on current response
            val tipoAccion = respuestaConDetalles.respuesta.tipoAccion
            if (tipoAccion != null) {
                currentTipoAccion = tipoAccion
                when (tipoAccion) {
                    Respuesta.ACCION_INMEDIATO -> binding.radioAviso.isChecked = true
                    Respuesta.ACCION_PROGRAMADO -> binding.radioOT.isChecked = true
                }
            } else {
                // Default to Aviso (immediate)
                binding.radioAviso.isChecked = true
                currentTipoAccion = Respuesta.ACCION_INMEDIATO
            }

            // Set SAP ID if available
            binding.sapIdEditText.setText(respuestaConDetalles.respuesta.idAvisoOrdenTrabajo ?: "")

            // Check if this item is incomplete
            checkCompleteness()

            // Setup photos if any
            if (respuestaConDetalles.tieneFotos()) {
                setupPhotosRecyclerView(respuestaConDetalles)
                binding.photosRecyclerView.visibility = View.VISIBLE
            } else {
                binding.photosRecyclerView.visibility = View.GONE
            }
        }

        private fun setupPhotosRecyclerView(respuestaConDetalles: RespuestaConDetalles) {
            val respuestaId = respuestaConDetalles.respuesta.respuestaId

            // Create adapter if needed
            if (!photoAdapters.containsKey(respuestaId)) {
                photoAdapters[respuestaId] = PhotoThumbnailAdapter(
                    onDeleteClicked = { /* Read-only, no delete action */ }
                )
            }

            val photoAdapter = photoAdapters[respuestaId]!!

            // Setup RecyclerView
            binding.photosRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                adapter = photoAdapter
            }

            // Update photos
            photoAdapter.updatePhotos(respuestaConDetalles.fotos)
        }

        private fun updateRespuesta() {
            // Get current SAP ID value
            val sapId = binding.sapIdEditText.text.toString().trim()

            // Update the response
            onSapIdUpdated(currentRespuestaId, currentTipoAccion, sapId)

            // Check if this item is complete
            checkCompleteness()
        }

        private fun checkCompleteness() {
            val sapId = binding.sapIdEditText.text.toString().trim()

            if (sapId.isEmpty()) {
                // This item is incomplete
                incompleteItems.add(currentRespuestaId)
                binding.validationText.visibility = View.VISIBLE
                binding.validationText.text = "El ID SAP es obligatorio"
            } else {
                // This item is complete
                incompleteItems.remove(currentRespuestaId)
                binding.validationText.visibility = View.GONE
            }
        }
    }

    class NoConformeDiffCallback : DiffUtil.ItemCallback<RespuestaConDetalles>() {
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