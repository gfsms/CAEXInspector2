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
 * Adapter for displaying No Conforme responses in the summary.
 */
class NoConformeAdapter(
    private val onSapIdUpdated: (Long, String, String) -> Unit,
    private val onVerHistorialClicked: (RespuestaConDetalles, Long) -> Unit,
    private val isReadOnly: Boolean = false
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
    fun updateSapInfo(respuestaId: Long, tipoAccion: String, idSap: String) {
        val currentList = currentList.toMutableList()
        val position = currentList.indexOfFirst { it.respuesta.respuestaId == respuestaId }

        if (position >= 0) {
            notifyItemChanged(position)
        }
    }
    /**
     * Returns a list of incomplete items (missing action type or SAP ID).
     */
    fun getIncompleteItems(): List<Long> {
        return incompleteItems.toList()
    }

    inner class NoConformeViewHolder(private val binding: ItemNoConformeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentTipoAccion: String = Respuesta.ACCION_INMEDIATO
        private var isUpdating = false  // Bandera para prevenir actualizaciones recursivas
        var currentRespuestaId: Long = 0
        var currentRespuestaConDetalles: RespuestaConDetalles? = null
        var caexId: Long = 0
        init {
            // Listeners para selección de modelo
            binding.actionTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                if (isReadOnly) return@setOnCheckedChangeListener

                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    currentTipoAccion = when (checkedId) {
                        binding.radioAviso.id -> Respuesta.ACCION_INMEDIATO
                        binding.radioOT.id -> Respuesta.ACCION_PROGRAMADO
                        else -> Respuesta.ACCION_INMEDIATO
                    }

                    // No actualizamos inmediatamente, solo cuando se pierde el foco o se completa la edición
                    checkCompleteness()
                }
            }

            // Cambiar para actualizar solo cuando se pierde el foco, no en cada cambio
            binding.sapIdEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && !isReadOnly) {
                    // Solo actualizamos cuando se pierde el foco
                    updateRespuesta()
                }
            }

            // También añadir un listener para la tecla "done" o "enter" del teclado
            binding.sapIdEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE && !isReadOnly) {
                    updateRespuesta()
                    // Ocultar el teclado
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
            currentRespuestaConDetalles = respuestaConDetalles
            // Set category and question text
            binding.categoryTitleText.text = respuestaConDetalles.pregunta.getCategoriaName()
            binding.questionText.text = respuestaConDetalles.pregunta.texto

            // Set comments
            binding.commentsText.text = respuestaConDetalles.respuesta.comentarios

            // Prevenir actualizaciones durante el binding
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

            // Set read-only mode if needed
            if (isReadOnly) {
                binding.radioAviso.isEnabled = false
                binding.radioOT.isEnabled = false
                binding.sapIdEditText.isEnabled = false
                binding.validationText.visibility = View.GONE
            }

            // Fin de la actualización de UI
            isUpdating = false

            // Check if this item is incomplete (only if editable)
            if (!isReadOnly) {
                checkCompleteness()
            }

            // Setup photos if any
            if (respuestaConDetalles.tieneFotos()) {
                setupPhotosRecyclerView(respuestaConDetalles)
                binding.photosRecyclerView.visibility = View.VISIBLE
            } else {
                binding.photosRecyclerView.visibility = View.GONE
            }

            binding.verHistorialButton.setOnClickListener {
                onVerHistorialClicked(respuestaConDetalles, caexId)
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
            // Si estamos en medio de una actualización de UI, no hacer nada
            if (isUpdating || isReadOnly) return

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
            if (isReadOnly) {
                incompleteItems.remove(currentRespuestaId)
                binding.validationText.visibility = View.GONE
                return
            }

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

        fun updateSapInfo(tipoAccion: String, idSap: String) {
            isUpdating = true

            when (tipoAccion) {
                Respuesta.ACCION_INMEDIATO -> binding.radioAviso.isChecked = true
                Respuesta.ACCION_PROGRAMADO -> binding.radioOT.isChecked = true
            }

            binding.sapIdEditText.setText(idSap)
            currentTipoAccion = tipoAccion

            isUpdating = false
            checkCompleteness()
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