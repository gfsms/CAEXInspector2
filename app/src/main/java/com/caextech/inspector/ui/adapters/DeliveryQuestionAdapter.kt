package com.caextech.inspector.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caextech.inspector.R
import com.caextech.inspector.data.entities.Foto
import com.caextech.inspector.data.entities.Pregunta
import com.caextech.inspector.data.entities.Respuesta
import com.caextech.inspector.data.relations.RespuestaConDetalles
import com.caextech.inspector.databinding.ItemDeliveryQuestionBinding
import com.caextech.inspector.ui.fragments.PhotoCaptureFragment
import com.caextech.inspector.utils.Logger
import com.caextech.inspector.utils.RespuestaTracker

/**
 * Adapter for displaying delivery inspection questions in a RecyclerView.
 * Handles questions for delivery inspections showing "Aceptado"/"Rechazado" options,
 * and highlights questions that were "No Conforme" in the reception inspection.
 */
class DeliveryQuestionAdapter(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val inspeccionId: Long,
    private val inspeccionRecepcionId: Long,
    private val preguntasNoConformes: Set<Long>,
    private val onAceptadoSelected: (Long) -> Unit,
    private val onRechazadoSelected: (Long, String) -> Unit,
    private val onAddPhotoClicked: (Long) -> Unit,
    private val onDeletePhotoClicked: (Foto) -> Unit
) : RecyclerView.Adapter<DeliveryQuestionAdapter.DeliveryQuestionViewHolder>() {
    private val TAG = "DeliveryQuestionAdapter"

    private val preguntas = mutableListOf<Pregunta>()

    // Map to store the state of all responses
    private val respuestas = mutableMapOf<Long, RespuestaConDetalles>()

    // Map to store the photo adapters
    private val photoAdapters = mutableMapOf<Long, PhotoThumbnailAdapter>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeliveryQuestionViewHolder {
        val binding = ItemDeliveryQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeliveryQuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeliveryQuestionViewHolder, position: Int) {
        val pregunta = preguntas[position]
        val respuesta = respuestas[pregunta.preguntaId]
        holder.bind(pregunta, respuesta)
    }

    override fun getItemCount(): Int = preguntas.size

    // Crucial: Use the pregunta ID as the stable ID
    override fun getItemId(position: Int): Long {
        return preguntas[position].preguntaId
    }

    // Enable stable IDs to maintain view state
    init {
        setHasStableIds(true)
    }

    /**
     * Updates the list of questions displayed by the adapter.
     */
    fun updatePreguntas(newPreguntas: List<Pregunta>) {
        preguntas.clear()
        preguntas.addAll(newPreguntas)
        notifyDataSetChanged()
    }

    /**
     * Updates the map of responses for the questions.
     */
    fun updateRespuestas(newRespuestas: List<RespuestaConDetalles>) {
        Logger.d(TAG, "Actualizando respuestas: ${newRespuestas.size} recibidas")

        // Limpiar el mapa de respuestas actual
        respuestas.clear()

        // Update the responses map with the new responses
        for (respuesta in newRespuestas) {
            respuestas[respuesta.pregunta.preguntaId] = respuesta
            Logger.d(TAG, "Agregada respuesta DB para pregunta ${respuesta.pregunta.preguntaId}: ${respuesta.respuesta.estado}")
        }

        // Verificar respuestas en memoria que no están en la DB
        for (pregunta in preguntas) {
            // Si ya tenemos esta respuesta en el mapa, continuar
            if (respuestas.containsKey(pregunta.preguntaId)) continue

            // Verificar si tenemos una respuesta en memoria
            val estadoEnMemoria = RespuestaTracker.obtenerEstadoRespuesta(inspeccionId, pregunta.preguntaId)
            if (estadoEnMemoria != null) {
                Logger.d(TAG, "Encontrada respuesta en memoria para pregunta ${pregunta.preguntaId}: $estadoEnMemoria")
            }
        }

        notifyDataSetChanged()
    }

    /**
     * ViewHolder for displaying delivery questions with unique UI for delivery inspections.
     */
    inner class DeliveryQuestionViewHolder(private val binding: ItemDeliveryQuestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pregunta: Pregunta, respuesta: RespuestaConDetalles?) {
            // Clear existing state first - important for recycled views
            clearViewState()

            // Set question text
            binding.questionNumberText.text = "Pregunta ${pregunta.orden}"
            binding.questionText.text = pregunta.texto

            // Check if this question was marked as No Conforme in the reception inspection
            val wasNoConforme = preguntasNoConformes.contains(pregunta.preguntaId)

            // Highlight the background if the question was No Conforme in reception
            if (wasNoConforme) {
                binding.questionCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.highlight_no_conforme_background)
                )
                binding.recepcionStatusText.visibility = View.VISIBLE
                binding.recepcionStatusText.text = "No Conforme en Recepción"
            } else {
                binding.questionCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.white)
                )
                binding.recepcionStatusText.visibility = View.GONE
            }

            // Check if we have a response in memory that's not in the DB
            val estadoEnMemoria = RespuestaTracker.obtenerEstadoRespuesta(inspeccionId, pregunta.preguntaId)

            // Configure the view based on the response or memory state
            if (respuesta != null) {
                // We have a DB response, use that
                configureViewForDbResponse(pregunta, respuesta)
            } else if (estadoEnMemoria != null) {
                // We have a memory-only response, use that
                configureViewForMemoryResponse(pregunta, estadoEnMemoria)
            } else {
                // No response yet
                binding.statusText.text = "Pendiente"
                binding.statusText.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            }

            // Set up the click listeners AFTER configuring the view
            setupClickListeners(pregunta)
        }

        /**
         * Clears the state of the view to prepare for binding new data
         */
        private fun clearViewState() {
            // Remove listeners first to prevent accidental triggers
            binding.radioAceptado.setOnClickListener(null)
            binding.radioRechazado.setOnClickListener(null)
            binding.answerRadioGroup.clearCheck()
            binding.commentsEditText.setText("")
            binding.commentsLayout.visibility = View.GONE
            binding.photosContainer.visibility = View.GONE
        }

        /**
         * Configures the view based on a response from the database
         */
        private fun configureViewForDbResponse(pregunta: Pregunta, respuesta: RespuestaConDetalles) {
            // Configure based on response state
            when (respuesta.respuesta.estado) {
                Respuesta.ESTADO_ACEPTADO -> {
                    binding.radioAceptado.isChecked = true
                    binding.statusText.text = "Aceptado"
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_conforme))
                }
                Respuesta.ESTADO_RECHAZADO -> {
                    binding.radioRechazado.isChecked = true
                    binding.commentsLayout.visibility = View.VISIBLE
                    binding.commentsEditText.setText(respuesta.respuesta.comentarios)
                    binding.statusText.text = "Rechazado"
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_no_conforme))

                    // Configure photos section
                    binding.photosContainer.visibility = View.VISIBLE
                    if (respuesta.tieneFotos()) {
                        setupPhotosRecyclerView(respuesta)
                    }
                }
            }

            // Register the state in memory for redundancy
            if (respuesta.respuesta.estado == Respuesta.ESTADO_ACEPTADO) {
                RespuestaTracker.registrarRespuestaAceptada(inspeccionId, pregunta.preguntaId)
            } else {
                RespuestaTracker.registrarRespuestaRechazada(inspeccionId, pregunta.preguntaId)
            }
        }

        /**
         * Configures the view based on a response that only exists in memory
         */
        private fun configureViewForMemoryResponse(pregunta: Pregunta, estado: String) {
            when (estado) {
                Respuesta.ESTADO_ACEPTADO -> {
                    binding.radioAceptado.isChecked = true
                    binding.statusText.text = "Aceptado"
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_conforme))
                }
                Respuesta.ESTADO_RECHAZADO -> {
                    binding.radioRechazado.isChecked = true
                    binding.commentsLayout.visibility = View.VISIBLE
                    binding.statusText.text = "Rechazado (Pendiente de guardar)"
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_no_conforme))
                    binding.photosContainer.visibility = View.VISIBLE
                }
            }
        }

        /**
         * Sets up click listeners for the radio buttons and other UI elements
         */
        private fun setupClickListeners(pregunta: Pregunta) {
            // Set up click listeners for radio buttons - do this individually
            // instead of using RadioGroup.OnCheckedChangeListener to avoid issues
            binding.radioAceptado.setOnClickListener {
                // Update memory tracker first
                RespuestaTracker.registrarRespuestaAceptada(inspeccionId, pregunta.preguntaId)

                // Handle "Aceptado" selection in DB
                onAceptadoSelected(pregunta.preguntaId)

                // Update UI immediately
                binding.commentsLayout.visibility = View.GONE
                binding.photosContainer.visibility = View.GONE
                binding.statusText.text = "Aceptado"
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_conforme))
            }

            binding.radioRechazado.setOnClickListener {
                // Update memory tracker first
                RespuestaTracker.registrarRespuestaRechazada(inspeccionId, pregunta.preguntaId)

                // Show comments UI
                binding.commentsLayout.visibility = View.VISIBLE
                binding.photosContainer.visibility = View.VISIBLE
                binding.statusText.text = "Rechazado"
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_no_conforme))

                // Handle existing comment if any
                val comentarios = binding.commentsEditText.text.toString()
                if (comentarios.isNotBlank()) {
                    onRechazadoSelected(pregunta.preguntaId, comentarios)
                }
            }

            // Set up comment field listener
            binding.commentsEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val comentarios = binding.commentsEditText.text.toString()
                    if (binding.radioRechazado.isChecked && comentarios.isNotBlank()) {
                        onRechazadoSelected(pregunta.preguntaId, comentarios)
                    }
                }
            }

            // Set up add photo button
            binding.addPhotoButton.setOnClickListener {
                val respuesta = respuestas[pregunta.preguntaId]

                if (respuesta != null) {
                    // If we have a response, show the photo capture dialog
                    showPhotoCaptureDialog(respuesta.respuesta.respuestaId)
                } else {
                    // If no response yet, first create one with comments
                    val comentarios = binding.commentsEditText.text.toString().ifBlank {
                        // Si no hay comentarios, usar un comentario temporal
                        "Pendiente de detallar"
                    }

                    // Mandatory to have a comment for Rechazado
                    onRechazadoSelected(pregunta.preguntaId, comentarios)

                    // Note: ideally we would wait for the response ID before showing the dialog
                    // We'll show a message for now and let the user try again after a moment
                    binding.statusText.text = "Guardando respuesta..."
                }
            }
        }

        /**
         * Shows the photo capture dialog
         */
        private fun showPhotoCaptureDialog(respuestaId: Long) {
            val dialog = PhotoCaptureFragment.newInstance(respuestaId)
            dialog.show(fragmentManager, "PhotoCaptureFragment")
        }

        /**
         * Sets up the photos RecyclerView
         */
        private fun setupPhotosRecyclerView(respuesta: RespuestaConDetalles) {
            val respuestaId = respuesta.respuesta.respuestaId

            // Create adapter if needed
            if (!photoAdapters.containsKey(respuestaId)) {
                photoAdapters[respuestaId] = PhotoThumbnailAdapter(
                    onDeleteClicked = { foto ->
                        onDeletePhotoClicked(foto)
                    }
                )
            }

            val photoAdapter = photoAdapters[respuestaId]!!

            // Setup RecyclerView
            binding.photosRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                adapter = photoAdapter
            }

            // Update photos
            photoAdapter.updatePhotos(respuesta.fotos)
        }
    }
}