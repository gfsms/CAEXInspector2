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
 * Adaptador mejorado para mostrar preguntas de inspección en un RecyclerView.
 * Maneja tanto la visualización de preguntas como la captura de respuestas del usuario.
 */
class QuestionAdapter(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val inspeccionId: Long,
    private val onConformeSelected: (Long) -> Unit,
    private val onNoConformeSelected: (Long, String) -> Unit,
    private val onAddPhotoClicked: (Long) -> Unit,
    private val onDeletePhotoClicked: (Foto) -> Unit
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {
    private val TAG = "QuestionAdapter"

    // Listas y mapas para gestionar el estado
    private val preguntas = mutableListOf<Pregunta>()

    // Mapa para almacenar las respuestas con todos sus detalles
    private val respuestas = mutableMapOf<Long, RespuestaConDetalles>()

    // Mapa para almacenar el estado actual de cada pregunta, independientemente de si está en la BD
    // Esta es nuestra fuente de verdad unificada
    private val estadoActual = mutableMapOf<Long, String>()

    // Mapa para almacenar los comentarios que el usuario ha ingresado pero aún no se han guardado
    private val comentariosTemp = mutableMapOf<Long, String>()

    // Mapa para almacenar los adaptadores de fotos
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
        holder.bind(pregunta)
    }

    override fun getItemCount(): Int = preguntas.size

    // Crucial: Usar el ID de la pregunta como ID estable
    override fun getItemId(position: Int): Long {
        return preguntas[position].preguntaId
    }

    // Habilitar IDs estables para mantener el estado de las vistas
    init {
        setHasStableIds(true)
    }

    /**
     * Actualiza la lista de preguntas que muestra el adaptador.
     */
    fun updatePreguntas(newPreguntas: List<Pregunta>) {
        preguntas.clear()
        preguntas.addAll(newPreguntas)

        // Inicializar el estado para nuevas preguntas
        for (pregunta in newPreguntas) {
            // Solo inicializar si no existe ya en el mapa de estado
            if (!estadoActual.containsKey(pregunta.preguntaId)) {
                // Verificar si hay un estado en memoria en RespuestaTracker
                val estadoEnMemoria = RespuestaTracker.obtenerEstadoRespuesta(inspeccionId, pregunta.preguntaId)
                if (estadoEnMemoria != null) {
                    // Usar el estado de memoria como estado inicial
                    estadoActual[pregunta.preguntaId] = estadoEnMemoria
                    Logger.d(TAG, "Estado inicial desde memoria para pregunta ${pregunta.preguntaId}: $estadoEnMemoria")
                }
            }
        }

        notifyDataSetChanged()
    }

    /**
     * Actualiza el mapa de respuestas con las respuestas de la base de datos.
     */
    fun updateRespuestas(newRespuestas: List<RespuestaConDetalles>) {
        Logger.d(TAG, "Actualizando respuestas: ${newRespuestas.size} recibidas")

        // Guardar las respuestas en el mapa
        respuestas.clear()

        val preguntasEnBD = mutableSetOf<Long>()

        for (respuesta in newRespuestas) {
            val preguntaId = respuesta.pregunta.preguntaId
            respuestas[preguntaId] = respuesta
            preguntasEnBD.add(preguntaId)

            // Actualizar el estado actual desde la BD para esta pregunta
            estadoActual[preguntaId] = respuesta.respuesta.estado

            Logger.d(TAG, "Agregada respuesta DB para pregunta $preguntaId: ${respuesta.respuesta.estado}")
        }
        for (pregunta in preguntas) {
            val preguntaId = pregunta.preguntaId

            // Si la pregunta NO está en BD pero SÍ tiene estado en memoria, preservarlo
            if (!preguntasEnBD.contains(preguntaId)) {
                val estadoEnMemoria = RespuestaTracker.obtenerEstadoRespuesta(inspeccionId, preguntaId)
                if (estadoEnMemoria != null) {
                    estadoActual[preguntaId] = estadoEnMemoria
                    Logger.d(TAG, "Preservado estado en memoria para pregunta $preguntaId: $estadoEnMemoria")
                }
            }
        }
        // Actualizar RespuestaTracker con los datos más recientes
        RespuestaTracker.sincronizarConBaseDatos(inspeccionId, newRespuestas)
        // Actualizar la UI para reflejar los cambios
        notifyDataSetChanged()
    }

    inner class QuestionViewHolder(private val binding: ItemQuestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentPreguntaId: Long = 0

        fun bind(pregunta: Pregunta) {
            // Guardar el ID de la pregunta actual
            currentPreguntaId = pregunta.preguntaId

            // Limpiar estado visual primero (importante para vistas recicladas)
            clearViewState()

            // Configurar texto de la pregunta
            binding.questionNumberText.text = "Pregunta ${pregunta.orden}"
            binding.questionText.text = pregunta.texto

            // Obtener el estado actual de esta pregunta
            val estadoRespuesta = estadoActual[pregunta.preguntaId]
            val respuestaDetalles = respuestas[pregunta.preguntaId]

            // Configurar la vista según el estado actual
            if (estadoRespuesta != null) {
                // Si hay un estado, configurar la UI según ese estado
                configureViewForState(pregunta, estadoRespuesta, respuestaDetalles)
            } else {
                // Si no hay estado, mostrar como pendiente
                binding.statusText.text = "Pendiente"
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_pending))
            }

            // Configurar los listeners DESPUÉS de establecer el estado
            setupRadioButtonListeners(pregunta)
            setupCommentFieldListener(pregunta)
            setupAddPhotoButton(pregunta)
        }

        /**
         * Limpia el estado visual de la vista para prepararla para nuevos datos
         */
        private fun clearViewState() {
            // Eliminar listeners primero para evitar activaciones accidentales
            binding.radioConforme.setOnClickListener(null)
            binding.radioNoConforme.setOnClickListener(null)
            binding.commentsEditText.setOnFocusChangeListener(null)
            binding.addPhotoButton.setOnClickListener(null)

            // Restablecer estado visual
            binding.answerRadioGroup.clearCheck()
            binding.commentsEditText.setText("")
            binding.commentsLayout.visibility = View.GONE
            binding.photosContainer.visibility = View.GONE
            binding.statusText.text = ""
        }

        /**
         * Configura la vista según el estado de la respuesta.
         * PRIORIZA el estado en memoria sobre el estado de BD.
         */
        private fun configureViewForState(pregunta: Pregunta, estado: String, respuestaDetalles: RespuestaConDetalles?) {
            // CAMBIO CLAVE: Verificar primero el estado en memoria
            val estadoEnMemoria = RespuestaTracker.obtenerEstadoRespuesta(inspeccionId, pregunta.preguntaId)
            val estadoFinal = estadoEnMemoria ?: estado

            Logger.d(TAG, "Configurando vista pregunta ${pregunta.preguntaId}: estado=$estado, memoria=$estadoEnMemoria, final=$estadoFinal")

            when (estadoFinal) {
                Respuesta.ESTADO_CONFORME -> {
                    binding.radioConforme.isChecked = true
                    binding.commentsLayout.visibility = View.GONE
                    binding.photosContainer.visibility = View.GONE
                    binding.statusText.text = "Conforme"
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_conforme))
                }
                Respuesta.ESTADO_NO_CONFORME -> {
                    binding.radioNoConforme.isChecked = true
                    binding.commentsLayout.visibility = View.VISIBLE

                    // Mostrar comentarios: priorizar memoria temporal, luego BD
                    val comentarios = comentariosTemp[pregunta.preguntaId]
                        ?: respuestaDetalles?.respuesta?.comentarios
                        ?: ""
                    binding.commentsEditText.setText(comentarios)

                    binding.statusText.text = "No Conforme"
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_no_conforme))

                    // Configurar sección de fotos
                    binding.photosContainer.visibility = View.VISIBLE
                    if (respuestaDetalles != null && respuestaDetalles.tieneFotos()) {
                        setupPhotosRecyclerView(respuestaDetalles)
                    }
                }
                Respuesta.ESTADO_ACEPTADO -> {
                    binding.radioConforme.isChecked = true
                    binding.commentsLayout.visibility = View.GONE
                    binding.photosContainer.visibility = View.GONE
                    binding.statusText.text = "Aceptado"
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_conforme))
                }
                Respuesta.ESTADO_RECHAZADO -> {
                    binding.radioNoConforme.isChecked = true
                    binding.commentsLayout.visibility = View.VISIBLE

                    val comentarios = comentariosTemp[pregunta.preguntaId]
                        ?: respuestaDetalles?.respuesta?.comentarios
                        ?: ""
                    binding.commentsEditText.setText(comentarios)

                    binding.statusText.text = "Rechazado"
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_no_conforme))

                    binding.photosContainer.visibility = View.VISIBLE
                    if (respuestaDetalles != null && respuestaDetalles.tieneFotos()) {
                        setupPhotosRecyclerView(respuestaDetalles)
                    }
                }
            }
        }

        /**
         * Configura los listeners para los RadioButtons.
         * Previene activación durante la configuración de la vista.
         */
        private fun setupRadioButtonListeners(pregunta: Pregunta) {
            // Flag para prevenir activación durante bind
            var isSettingUp = false

            binding.radioConforme.setOnClickListener {
                // Solo procesar si no estamos en setup y el usuario realmente hizo click
                if (isSettingUp) return@setOnClickListener

                Logger.d(TAG, "Usuario seleccionó CONFORME para pregunta ${pregunta.preguntaId}")

                // Actualizar estado local
                estadoActual[pregunta.preguntaId] = Respuesta.ESTADO_CONFORME

                // Actualizar RespuestaTracker
                RespuestaTracker.registrarRespuestaConforme(inspeccionId, pregunta.preguntaId)

                // Actualizar UI inmediatamente sin esperar BD
                isSettingUp = true
                binding.commentsLayout.visibility = View.GONE
                binding.photosContainer.visibility = View.GONE
                binding.statusText.text = "Conforme"
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_conforme))
                isSettingUp = false

                // Guardar en la base de datos
                onConformeSelected(pregunta.preguntaId)
            }

            binding.radioNoConforme.setOnClickListener {
                if (isSettingUp) return@setOnClickListener

                Logger.d(TAG, "Usuario seleccionó NO_CONFORME para pregunta ${pregunta.preguntaId}")

                // Actualizar estado local
                estadoActual[pregunta.preguntaId] = Respuesta.ESTADO_NO_CONFORME

                // Actualizar RespuestaTracker
                RespuestaTracker.registrarRespuestaNoConforme(inspeccionId, pregunta.preguntaId)

                // Actualizar UI inmediatamente
                isSettingUp = true
                binding.commentsLayout.visibility = View.VISIBLE
                binding.photosContainer.visibility = View.VISIBLE
                binding.statusText.text = "No Conforme"
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_no_conforme))
                isSettingUp = false

                // MEJORA: Guardar inmediatamente con comentario temporal
                val comentarios = binding.commentsEditText.text.toString().ifBlank {
                    "Pendiente de completar detalles"
                }
                guardarRespuestaNoConforme(pregunta.preguntaId, comentarios)
            }
        }

        /**
         * Configura el listener para el campo de comentarios
         */
        private fun setupCommentFieldListener(pregunta: Pregunta) {
            binding.commentsEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val comentarios = binding.commentsEditText.text.toString()

                    // Guardar comentarios temporalmente
                    comentariosTemp[pregunta.preguntaId] = comentarios

                    // Si está marcado como No Conforme y hay comentarios, guardar en BD
                    if (binding.radioNoConforme.isChecked && comentarios.isNotBlank()) {
                        guardarRespuestaNoConforme(pregunta.preguntaId, comentarios)
                    }
                }
            }
        }

        /**
         * Guarda una respuesta No Conforme para una pregunta
         */
        private fun guardarRespuestaNoConforme(preguntaId: Long, comentarios: String) {
            if (comentarios.isBlank()) return

            // Actualizar RespuestaTracker para mantener estado consistente
            RespuestaTracker.registrarRespuestaNoConforme(inspeccionId, preguntaId)

            // Guardar en la base de datos
            onNoConformeSelected(preguntaId, comentarios)
        }

        /**
         * Configura el botón para agregar fotos
         */
        private fun setupAddPhotoButton(pregunta: Pregunta) {
            binding.addPhotoButton.setOnClickListener {
                val respuestaDetalles = respuestas[pregunta.preguntaId]

                if (respuestaDetalles != null) {
                    // Si ya existe una respuesta, mostrar diálogo de captura de foto
                    showPhotoCaptureDialog(respuestaDetalles.respuesta.respuestaId)
                } else {
                    // Si no hay respuesta aún, primero crear una con comentarios
                    val comentarios = binding.commentsEditText.text.toString().ifBlank {
                        "Pendiente de detallar"
                    }

                    // Asegurar que está marcado como No Conforme
                    if (!binding.radioNoConforme.isChecked) {
                        binding.radioNoConforme.isChecked = true
                        // Actualizar estado local
                        estadoActual[pregunta.preguntaId] = Respuesta.ESTADO_NO_CONFORME
                        // Actualizar tracker
                        RespuestaTracker.registrarRespuestaNoConforme(inspeccionId, pregunta.preguntaId)
                    }

                    // Guardar respuesta en BD
                    onNoConformeSelected(pregunta.preguntaId, comentarios)

                    // Mostrar indicación temporal
                    binding.statusText.text = "Guardando respuesta..."

                    // Esperar un momento para que se cree la respuesta
                    binding.root.postDelayed({
                        // Intentar de nuevo después de crear la respuesta
                        val newRespuesta = respuestas[pregunta.preguntaId]
                        if (newRespuesta != null) {
                            showPhotoCaptureDialog(newRespuesta.respuesta.respuestaId)
                        } else {
                            Toast.makeText(context, "Intente nuevamente en un momento", Toast.LENGTH_SHORT).show()
                        }
                    }, 500) // 500ms de retraso
                }
            }
        }

        /**
         * Muestra el diálogo de captura de fotos
         */
        private fun showPhotoCaptureDialog(respuestaId: Long) {
            val dialog = PhotoCaptureFragment.newInstance(respuestaId)
            dialog.show(fragmentManager, "PhotoCaptureFragment")

            // Guardar el estado actual para evitar pérdida al volver de la cámara
            RespuestaTracker.ensureNoConformeEstado(respuestaId)

            // Asegurar que el estado de la pregunta actual se mantiene como NO_CONFORME
            if (currentPreguntaId > 0) {
                estadoActual[currentPreguntaId] = Respuesta.ESTADO_NO_CONFORME
            }
        }

        /**
         * Configura el RecyclerView de fotos
         */
        private fun setupPhotosRecyclerView(respuesta: RespuestaConDetalles) {
            val respuestaId = respuesta.respuesta.respuestaId

            // Crear adaptador si es necesario
            if (!photoAdapters.containsKey(respuestaId)) {
                photoAdapters[respuestaId] = PhotoThumbnailAdapter(
                    onDeleteClicked = { foto ->
                        onDeletePhotoClicked(foto)
                    }
                )
            }

            val photoAdapter = photoAdapters[respuestaId]!!

            // Configurar RecyclerView
            binding.photosRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                adapter = photoAdapter
            }

            // Actualizar fotos
            photoAdapter.updatePhotos(respuesta.fotos)
        }
    }
}