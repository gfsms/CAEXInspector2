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
import com.caextech.inspector.databinding.ItemQuestionBinding
import com.caextech.inspector.ui.fragments.PhotoCaptureFragment

/**
 * Adapter for displaying inspection questions in a RecyclerView.
 * Handles both displaying questions and capturing user responses.
 */
class QuestionAdapter(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val onConformeSelected: (Long) -> Unit,
    private val onNoConformeSelected: (Long, String) -> Unit,
    private val onAddPhotoClicked: (Long) -> Unit,
    private val onDeletePhotoClicked: (Foto) -> Unit
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    private val preguntas = mutableListOf<Pregunta>()
    private val respuestas = mutableMapOf<Long, RespuestaConDetalles>()
    private var photoAdapters = mutableMapOf<Long, PhotoThumbnailAdapter>()

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
        for (respuesta in newRespuestas) {
            respuestas[respuesta.pregunta.preguntaId] = respuesta
        }
        notifyDataSetChanged()
    }

    inner class QuestionViewHolder(private val binding: ItemQuestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Set up listeners
            binding.radioConforme.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val pregunta = preguntas[position]
                    onConformeSelected(pregunta.preguntaId)

                    // Hide comments and photos UI
                    binding.commentsLayout.visibility = View.GONE
                    binding.photosContainer.visibility = View.GONE
                }
            }

            binding.radioNoConforme.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Show comments UI
                    binding.commentsLayout.visibility = View.VISIBLE

                    // Check if we already have a response
                    val pregunta = preguntas[position]
                    val respuesta = respuestas[pregunta.preguntaId]

                    if (respuesta != null) {
                        // Show photos UI if we have a response
                        binding.photosContainer.visibility = View.VISIBLE

                        // Pre-fill comments if we have them
                        binding.commentsEditText.setText(respuesta.respuesta.comentarios)
                    } else {
                        binding.photosContainer.visibility = View.VISIBLE
                    }
                }
            }

            binding.commentsEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    // Save the comments when focus is lost
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION && binding.radioNoConforme.isChecked) {
                        val pregunta = preguntas[position]
                        val comentarios = binding.commentsEditText.text.toString()
                        onNoConformeSelected(pregunta.preguntaId, comentarios)
                    }
                }
            }

            binding.addPhotoButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val pregunta = preguntas[position]
                    val respuesta = respuestas[pregunta.preguntaId]

                    if (respuesta != null) {
                        // Show photo capture dialog
                        val dialog = PhotoCaptureFragment.newInstance(respuesta.respuesta.respuestaId)
                        dialog.show(fragmentManager, "PhotoCaptureFragment")
                    } else {
                        // If no response yet, we need to create one first
                        val comentarios = binding.commentsEditText.text.toString()
                        if (comentarios.isNotBlank()) {
                            // Save the No Conforme response and then handle the photo
                            onNoConformeSelected(pregunta.preguntaId, comentarios)
                            // The photo will need to be added after the response is created
                        }
                    }
                }
            }
        }

        fun bind(pregunta: Pregunta, respuesta: RespuestaConDetalles?) {
            // Set question text
            binding.questionNumberText.text = "Pregunta ${pregunta.orden}"
            binding.questionText.text = pregunta.texto

            // Reset radio buttons
            binding.answerRadioGroup.clearCheck()

            // Hide comments and photos UI by default
            binding.commentsLayout.visibility = View.GONE
            binding.photosContainer.visibility = View.GONE

            // Set status text color and value
            val context = binding.root.context

            if (respuesta == null) {
                // No response yet
                binding.statusText.text = "Pendiente"
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_pending))
            } else {
                // We have a response, set state based on it
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

                        // Show photos if we have any
                        if (respuesta.tieneFotos()) {
                            binding.photosContainer.visibility = View.VISIBLE
                            setupPhotosRecyclerView(respuesta)
                        } else {
                            binding.photosContainer.visibility = View.VISIBLE // Still show to allow adding photos
                        }
                    }
                }
            }
        }

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