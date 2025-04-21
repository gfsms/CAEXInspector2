package com.caextech.inspector.ui.adapters

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.caextech.inspector.data.entities.Respuesta
import com.caextech.inspector.data.entities.getCategoriaName
import com.caextech.inspector.data.relations.RespuestaConDetalles
import com.caextech.inspector.databinding.ItemNoConformeBinding

/**
 * Adapter for displaying Rechazado responses in the delivery summary.
 * Similar to NoConformeAdapter but specifically for delivery inspections.
 */
class RechazadoAdapter(
    private val onSapIdUpdated: (Long, String, String) -> Unit
) : ListAdapter<RespuestaConDetalles, RechazadoAdapter.RechazadoViewHolder>(RechazadoDiffCallback()) {

    private val incompleteItems = mutableSetOf<Long>()
    private val photoAdapters = mutableMapOf<Long, PhotoThumbnailAdapter>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RechazadoViewHolder {
        val binding = ItemNoConformeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RechazadoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RechazadoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Returns a list of incomplete items (missing action type or SAP ID).
     */
    fun getIncompleteItems(): List<Long> {
        return incompleteItems.toList()
    }

    inner class RechazadoViewHolder(private val binding: ItemNoConformeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentRespuestaId: Long = 0
        private var currentTipoAccion: String = Respuesta.ACCION_INMEDIATO
        private var isUpdating = false  // Flag to prevent recursive updates

        init {
            // Listeners for action type selection
            binding.actionTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    currentTipoAccion = when (checkedId) {
                        binding.radioAviso.id -> Respuesta.ACCION_INMEDIATO
                        binding.radioOT.id -> Respuesta.ACCION_PROGRAMADO
                        else -> Respuesta.ACCION_INMEDIATO
                    }

                    // Only update when focus is lost, not on every change
                    checkCompleteness()
                }
            }

            // Update only when focus is lost
            binding.sapIdEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateRespuesta()
                }
            }

            // Also add a listener for the "done" key on the keyboard
            binding.sapIdEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    updateRespuesta()
                    // Hide keyboard
                    val imm = binding.root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.sapIdEditText.windowToken, 0)
                    binding.sapIdEditText.clearFocus()
                    return@setOnEditorActionListener true
                }
                false
            }
        }

        fun bind(respuestaConDetalles: RespuestaConDetalles) {
            currentRespuestaId = respuestaConDetalles.respuesta.respuestaId

            // Set category and question text
            binding.categoryTitleText.text = respuestaConDetalles.pregunta.getCategoriaName()
            binding.questionText.text = respuestaConDetalles.pregunta.texto

            // Set comments
            binding.commentsText.text = respuestaConDetalles.respuesta.comentarios

            // Prevent updates during binding
            isUpdating = true

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

            // End of UI update
            isUpdating = false

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
                    onDeleteClicked = { /* Read-only here, no delete action */ }
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
            // If we're in the middle of a UI update, do nothing
            if (isUpdating) return

            // Get current SAP ID value
            val sapId = binding.sapIdEditText.text.toString().trim()

            // Update the response only if we have a valid SAP ID
            if (sapId.isNotEmpty()) {
                // Update the response
                onSapIdUpdated(currentRespuestaId, currentTipoAccion, sapId)
            }

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

    class RechazadoDiffCallback : DiffUtil.ItemCallback<RespuestaConDetalles>() {
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