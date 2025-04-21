package com.caextech.inspector.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.databinding.FragmentCategoryQuestionsBinding
import com.caextech.inspector.ui.adapters.DeliveryQuestionAdapter
import com.caextech.inspector.ui.viewmodels.CategoriaPreguntaViewModel
import com.caextech.inspector.ui.viewmodels.FotoViewModel
import com.caextech.inspector.ui.viewmodels.RespuestaViewModel
import com.caextech.inspector.utils.Logger
import kotlinx.coroutines.launch

/**
 * Fragment for displaying questions of a specific category in the delivery inspection questionnaire.
 * This version highlights questions that were marked as "No Conforme" in the reception inspection
 * and uses "Aceptado"/"Rechazado" options instead of "Conforme"/"No Conforme".
 */
class DeliveryCategoryQuestionsFragment : Fragment() {
    private val TAG = "DeliveryCategoryQuestionsFragment"

    private var _binding: FragmentCategoryQuestionsBinding? = null
    private val binding get() = _binding!!

    // ViewModels
    private lateinit var categoriaPreguntaViewModel: CategoriaPreguntaViewModel
    private lateinit var respuestaViewModel: RespuestaViewModel
    private lateinit var fotoViewModel: FotoViewModel

    // Adapter
    private lateinit var questionAdapter: DeliveryQuestionAdapter

    // Fragment parameters
    private var inspeccionId: Long = 0
    private var inspeccionRecepcionId: Long = 0
    private var categoriaId: Long = 0
    private var modeloCAEX: String = ""
    private var preguntasNoConformes: Set<Long> = emptySet()

    // Current question being answered
    private var currentRespuestaId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get arguments
        arguments?.let {
            inspeccionId = it.getLong(ARG_INSPECCION_ID, 0)
            inspeccionRecepcionId = it.getLong(ARG_INSPECCION_RECEPCION_ID, 0)
            categoriaId = it.getLong(ARG_CATEGORIA_ID, 0)
            modeloCAEX = it.getString(ARG_MODELO_CAEX, "")
            preguntasNoConformes = it.getLongArray(ARG_NO_CONFORME_PREGUNTAS)?.toSet() ?: emptySet()
        }

        Logger.d(TAG, "Fragment created with inspeccionId: $inspeccionId, categoriaId: $categoriaId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryQuestionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModels
        initViewModels()

        // Setup RecyclerView
        setupRecyclerView()

        // Load category and questions
        loadCategoryData()
    }

    /**
     * Initializes ViewModels used in this fragment.
     */
    private fun initViewModels() {
        val application = requireActivity().application as CAEXInspectorApp

        categoriaPreguntaViewModel = ViewModelProvider(
            this,
            CategoriaPreguntaViewModel.CategoriaPreguntaViewModelFactory(
                application.categoriaRepository,
                application.preguntaRepository
            )
        )[CategoriaPreguntaViewModel::class.java]

        respuestaViewModel = ViewModelProvider(
            this,
            RespuestaViewModel.RespuestaViewModelFactory(
                application.respuestaRepository
            )
        )[RespuestaViewModel::class.java]

        fotoViewModel = ViewModelProvider(
            this,
            FotoViewModel.FotoViewModelFactory(
                application.fotoRepository
            )
        )[FotoViewModel::class.java]
    }

    /**
     * Sets up the RecyclerView with the DeliveryQuestionAdapter.
     */
    private fun setupRecyclerView() {
        questionAdapter = DeliveryQuestionAdapter(
            requireContext(),
            childFragmentManager,
            inspeccionId,
            inspeccionRecepcionId,
            preguntasNoConformes,
            onAceptadoSelected = { preguntaId ->
                guardarRespuestaAceptada(preguntaId)
            },
            onRechazadoSelected = { preguntaId, comentarios ->
                guardarRespuestaRechazada(preguntaId, comentarios)
            },
            onAddPhotoClicked = { respuestaId ->
                showPhotoOptions(respuestaId)
            },
            onDeletePhotoClicked = { foto ->
                fotoViewModel.deleteFoto(foto)
            }
        )

        binding.questionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = questionAdapter
        }
    }

    /**
     * Loads the category data and its questions.
     */
    private fun loadCategoryData() {
        // Load category with its questions
        categoriaPreguntaViewModel.getCategoriaConPreguntas(categoriaId)

        // Observe the category
        categoriaPreguntaViewModel.categoriaActual.observe(viewLifecycleOwner) { categoriaConPreguntas ->
            // Set category title
            binding.categoryTitleText.text = categoriaConPreguntas.categoria.nombre

            // Filter questions for the specified model
            val preguntas = categoriaConPreguntas.getPreguntasParaModelo(modeloCAEX)

            // Debug log for questions
            Logger.d(TAG, "Loaded ${preguntas.size} questions for category ${categoriaConPreguntas.categoria.nombre}")

            // Update adapter with questions
            questionAdapter.updatePreguntas(preguntas)
        }

        // Load answers for this category
        respuestaViewModel.getRespuestasConDetallesByInspeccionYCategoria(
            inspeccionId,
            categoriaId
        ).observe(viewLifecycleOwner) { respuestas ->
            // Debug log for responses
            Logger.d(TAG, "Loaded ${respuestas.size} responses for this category")

            // Update adapter with the questions and their answers
            questionAdapter.updateRespuestas(respuestas)

            // Update progress text
            val totalPreguntas = questionAdapter.itemCount
            val preguntasRespondidas = respuestas.size
            binding.progressText.text = "Completado: $preguntasRespondidas/$totalPreguntas preguntas"

            // Show/hide complete button if all questions answered
            binding.completeCategoryButton.visibility = if (preguntasRespondidas == totalPreguntas && totalPreguntas > 0) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    /**
     * Saves an "Aceptado" response for the given question.
     */
    private fun guardarRespuestaAceptada(preguntaId: Long) {
        // Use viewLifecycleOwner.lifecycleScope to launch a coroutine
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Save the response using the ViewModel
                respuestaViewModel.guardarRespuestaAceptada(inspeccionId, preguntaId)
                Logger.d(TAG, "Guardada respuesta ACEPTADO para pregunta $preguntaId")
            } catch (e: Exception) {
                Logger.e(TAG, "Error al guardar respuesta: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Error al guardar respuesta: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Saves a "Rechazado" response for the given question.
     */
    private fun guardarRespuestaRechazada(preguntaId: Long, comentarios: String) {
        if (comentarios.isBlank()) {
            Logger.w(TAG, "Intento de guardar respuesta RECHAZADO sin comentarios")
            Toast.makeText(
                requireContext(),
                "Debe ingresar comentarios para una respuesta Rechazado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Use viewLifecycleOwner.lifecycleScope to launch a coroutine
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Save the response using the ViewModel
                respuestaViewModel.guardarRespuestaRechazadaSimplificada(inspeccionId, preguntaId, comentarios)
                Logger.d(TAG, "Guardada respuesta RECHAZADO para pregunta $preguntaId")
            } catch (e: Exception) {
                Logger.e(TAG, "Error al guardar respuesta: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Error al guardar respuesta: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Shows options for taking or selecting a photo.
     */
    private fun showPhotoOptions(respuestaId: Long) {
        currentRespuestaId = respuestaId

        // Show the PhotoCaptureFragment dialog
        val dialog = PhotoCaptureFragment.newInstance(respuestaId)
        dialog.show(childFragmentManager, "PhotoCaptureFragment")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_INSPECCION_ID = "arg_inspeccion_id"
        private const val ARG_INSPECCION_RECEPCION_ID = "arg_inspeccion_recepcion_id"
        private const val ARG_CATEGORIA_ID = "arg_categoria_id"
        private const val ARG_MODELO_CAEX = "arg_modelo_caex"
        private const val ARG_NO_CONFORME_PREGUNTAS = "arg_no_conforme_preguntas"

        /**
         * Creates a new instance of DeliveryCategoryQuestionsFragment with the necessary arguments.
         */
        fun newInstance(
            inspeccionId: Long,
            inspeccionRecepcionId: Long,
            categoriaId: Long,
            modeloCAEX: String,
            preguntasNoConformes: ArrayList<Long>
        ): DeliveryCategoryQuestionsFragment {
            return DeliveryCategoryQuestionsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_INSPECCION_ID, inspeccionId)
                    putLong(ARG_INSPECCION_RECEPCION_ID, inspeccionRecepcionId)
                    putLong(ARG_CATEGORIA_ID, categoriaId)
                    putString(ARG_MODELO_CAEX, modeloCAEX)
                    putLongArray(ARG_NO_CONFORME_PREGUNTAS, preguntasNoConformes.toLongArray())
                }
            }
        }
    }
}