package com.caextech.inspector.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caextech.inspector.R
import com.caextech.inspector.data.entities.Foto
import com.caextech.inspector.data.entities.Pregunta
import com.caextech.inspector.data.entities.Respuesta
import com.caextech.inspector.data.relations.RespuestaConDetalles
import com.caextech.inspector.databinding.ItemQuestionBinding
import com.caextech.inspector.ui.fragments.PhotoCaptureFragment
import com.caextech.inspector.utils.Logger
import com.caextech.inspector.utils.RespuestaTracker

/**
 * Adapter for displaying inspection questions in a RecyclerView.
 * Handles both displaying questions and capturing user responses.
 */
class QuestionAdapter(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val inspeccionId: Long, // Añade este parámetro
    private val onConformeSelected: (Long) -> Unit,
    private val onNoConformeSelected: (Long, String) -> Unit,
    private val onAddPhotoClicked: (Long) -> Unit,
    private val onDeletePhotoClicked: (Foto) -> Unit
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {
    private val TAG = "QuestionAdapter"

    private val preguntas = mutableListOf<Pregunta>()

    // Map to store the state of all responses
    private val respuestas = mutableMapOf<Long, RespuestaConDetalles>()

    // Map to store the photo adapters
    private val photoAdapters = mutableMapOf<Long, PhotoThumbnailAdapter>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val binding = ItemQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
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

                // Acá podríamos crear un RespuestaConDetalles temporal, pero necesitaríamos
                // más información. Por ahora, simplemente registramos esto para la visualización.
            }
        }

        notifyDataSetChanged()
    }

    inner class QuestionViewHolder(private val binding: ItemQuestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pregunta: Pregunta, respuesta: RespuestaConDetalles?) {
            // Clear existing state first - important for recycled views
            clearViewState()

            // Set question text
            binding.questionNumberText.text = "Pregunta ${pregunta.orden}"
            binding.questionText.text = pregunta.texto

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
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_pending))
            }

            // Set up the click listeners AFTER configuring the view
            setupClickListeners(pregunta)
        }

        /**
         * Clears the state of the view to prepare for binding new data
         */
        private fun clearViewState() {
            // Remove listeners first to prevent accidental triggers
            binding.radioConforme.setOnClickListener(null)
            binding.radioNoConforme.setOnClickListener(null)
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
                Respuesta.ESTADO_CONFORME -> {
                    binding.radioConforme.isChecked = true
                    binding.statusText.text = "Conforme"
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_conforme))
                }
                Respuesta.ESTADO_NO_CONFORME -> {
                    binding.radioNoConforme.isChecked = true
                    binding.commentsLayout.visibility = View.VISIBLE
                    binding.commentsEditText.setText(respuesta.respuesta.comentarios)
                    binding.statusText.text = "No Conforme"
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_no_conforme))

                    // Configure photos section
                    binding.photosContainer.visibility = View.VISIBLE
                    if (respuesta.tieneFotos()) {
                        setupPhotosRecyclerView(respuesta)
                    }
                }
            }

            // Register the state in memory for redundancy
            if (respuesta.respuesta.estado == Respuesta.ESTADO_CONFORME) {
                RespuestaTracker.registrarRespuestaConforme(inspeccionId, pregunta.preguntaId)
            } else {
                RespuestaTracker.registrarRespuestaNoConforme(inspeccionId, pregunta.preguntaId)
            }
        }

        /**
         * Configures the view based on a response that only exists in memory
         */
        private fun configureViewForMemoryResponse(pregunta: Pregunta, estado: String) {
            when (estado) {
                Respuesta.ESTADO_CONFORME -> {
                    binding.radioConforme.isChecked = true
                    binding.statusText.text = "Conforme"
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_conforme))
                }
                Respuesta.ESTADO_NO_CONFORME -> {
                    binding.radioNoConforme.isChecked = true
                    binding.commentsLayout.visibility = View.VISIBLE
                    binding.statusText.text = "No Conforme (Pendiente de guardar)"
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
            binding.radioConforme.setOnClickListener {
                // Update memory tracker first
                RespuestaTracker.registrarRespuestaConforme(inspeccionId, pregunta.preguntaId)

                // Handle "Conforme" selection in DB
                onConformeSelected(pregunta.preguntaId)

                // Update UI immediately
                binding.commentsLayout.visibility = View.GONE
                binding.photosContainer.visibility = View.GONE
                binding.statusText.text = "Conforme"
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_conforme))
            }

            binding.radioNoConforme.setOnClickListener {
                // Update memory tracker first
                RespuestaTracker.registrarRespuestaNoConforme(inspeccionId, pregunta.preguntaId)

                // Show comments UI
                binding.commentsLayout.visibility = View.VISIBLE
                binding.photosContainer.visibility = View.VISIBLE
                binding.statusText.text = "No Conforme"
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_no_conforme))

                // Handle existing comment if any
                val comentarios = binding.commentsEditText.text.toString()
                if (comentarios.isNotBlank()) {
                    onNoConformeSelected(pregunta.preguntaId, comentarios)
                }
            }

            // Set up comment field listener
            binding.commentsEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val comentarios = binding.commentsEditText.text.toString()
                    if (binding.radioNoConforme.isChecked && comentarios.isNotBlank()) {
                        onNoConformeSelected(pregunta.preguntaId, comentarios)
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

                    // Mandatory to have a comment for No Conforme
                    binding.radioNoConforme.isChecked = true
                    onNoConformeSelected(pregunta.preguntaId, comentarios)

                    Toast.makeText(context, "Guardando respuesta, intente agregar foto nuevamente", Toast.LENGTH_SHORT).show()

                    // Note: ideally we would wait for the response ID before showing the dialog
                    // We'll show a message for now and let the user try again after a moment
                    binding.statusText.text = "Guardando respuesta..."
                    // Add delay to ensure response is created before showing dialog
                    binding.root.postDelayed({
                        // Try again after response is created
                        val newRespuesta = respuestas[pregunta.preguntaId]
                        if (newRespuesta != null) {
                            showPhotoCaptureDialog(newRespuesta.respuesta.respuestaId)
                        } else {
                            Toast.makeText(context, "Intente nuevamente en un momento", Toast.LENGTH_SHORT).show()
                        }
                    }, 500) // 500ms delay
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