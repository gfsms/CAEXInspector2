package com.caextech.inspector.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.databinding.FragmentCategoryQuestionsBinding
import com.caextech.inspector.ui.adapters.QuestionAdapter
import com.caextech.inspector.ui.viewmodels.CategoriaPreguntaViewModel
import com.caextech.inspector.ui.viewmodels.FotoViewModel
import com.caextech.inspector.ui.viewmodels.RespuestaViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * Fragment for displaying questions of a specific category in the inspection questionnaire.
 */
class CategoryQuestionsFragment : Fragment() {

    private var _binding: FragmentCategoryQuestionsBinding? = null
    private val binding get() = _binding!!

    // ViewModels
    private lateinit var categoriaPreguntaViewModel: CategoriaPreguntaViewModel
    private lateinit var respuestaViewModel: RespuestaViewModel
    private lateinit var fotoViewModel: FotoViewModel

    // Adapter
    private lateinit var questionAdapter: QuestionAdapter

    // Fragment parameters
    private var inspeccionId: Long = 0
    private var categoriaId: Long = 0
    private var modeloCAEX: String = ""

    // Activity result launchers for camera and gallery
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectPictureLauncher: ActivityResultLauncher<Intent>

    // Current question being answered
    private var currentRespuestaId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get arguments
        arguments?.let {
            inspeccionId = it.getLong(ARG_INSPECCION_ID, 0)
            categoriaId = it.getLong(ARG_CATEGORIA_ID, 0)
            modeloCAEX = it.getString(ARG_MODELO_CAEX, "")
        }

        // Initialize activity result launchers
        initActivityResultLaunchers()
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
     * Initializes activity result launchers for camera and gallery.
     */
    private fun initActivityResultLaunchers() {
        // Launcher for taking a picture with the camera
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                // Photo was taken successfully
                if (currentRespuestaId > 0) {
                    fotoViewModel.guardarFotoTomada(currentRespuestaId)
                }
            }
        }

        // Launcher for selecting a picture from the gallery
        selectPictureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                // Photo was selected from gallery
                result.data?.data?.let { uri ->
                    if (currentRespuestaId > 0) {
                        fotoViewModel.guardarFotoSeleccionada(currentRespuestaId, uri)
                    }
                }
            }
        }
    }

    /**
     * Sets up the RecyclerView with the QuestionAdapter.
     */
    private fun setupRecyclerView() {
        questionAdapter = QuestionAdapter(
            requireContext(),
            childFragmentManager,
            onConformeSelected = { preguntaId ->
                guardarRespuestaConforme(preguntaId)
            },
            onNoConformeSelected = { preguntaId, comentarios ->
                guardarRespuestaNoConforme(preguntaId, comentarios)
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
            android.util.Log.d("CategoryQuestionsFragment", "Loaded ${preguntas.size} questions for category ${categoriaConPreguntas.categoria.nombre}")

            // Update adapter with questions
            questionAdapter.updatePreguntas(preguntas)
        }

        // Load answers for this category
        respuestaViewModel.getRespuestasConDetallesByInspeccionYCategoria(
            inspeccionId,
            categoriaId
        ).observe(viewLifecycleOwner) { respuestas ->
            // Debug log for responses
            android.util.Log.d("CategoryQuestionsFragment", "Loaded ${respuestas.size} responses for this category")

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
     * Loads the responses for the specified questions.
     *
     * This is a placeholder method - in a real implementation, we would load
     * responses for specific questions.
     */
    private fun loadRespuestasForPreguntas(preguntaIds: List<Long>) {
        // Now handled directly by the observation of categoriaActual
    }

    /**
     * Saves a "Conforme" response for the given question.
     */
    private fun guardarRespuestaConforme(preguntaId: Long) {
        // Use viewLifecycleOwner.lifecycleScope to launch a coroutine
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Save the response using the ViewModel
                respuestaViewModel.guardarRespuestaConforme(inspeccionId, preguntaId)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al guardar respuesta: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Saves a "No Conforme" response for the given question.
     */
    private fun guardarRespuestaNoConforme(preguntaId: Long, comentarios: String) {
        if (comentarios.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Debe ingresar comentarios para una respuesta No Conforme",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Use viewLifecycleOwner.lifecycleScope to launch a coroutine
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Save the response using the ViewModel
                respuestaViewModel.guardarRespuestaNoConformeSimplificada(inspeccionId, preguntaId, comentarios)
            } catch (e: Exception) {
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

    /**
     * Launches the camera to take a photo.
     */
    private fun takePhoto() {
        // Prepare temporary file for the photo
        fotoViewModel.prepararArchivoTemporalParaFoto()

        // Observe the temporary URI
        fotoViewModel.tempPhotoUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                // Launch camera with the URI
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, uri)
                }

                // Verify that there's a camera activity to handle the intent
                if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
                    takePictureLauncher.launch(takePictureIntent)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No se encontró una aplicación de cámara",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Launches the gallery to select a photo.
     */
    private fun selectPhotoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selectPictureLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_INSPECCION_ID = "arg_inspeccion_id"
        private const val ARG_CATEGORIA_ID = "arg_categoria_id"
        private const val ARG_MODELO_CAEX = "arg_modelo_caex"

        /**
         * Creates a new instance of CategoryQuestionsFragment with the necessary arguments.
         */
        fun newInstance(
            inspeccionId: Long,
            categoriaId: Long,
            modeloCAEX: String
        ): CategoryQuestionsFragment {
            return CategoryQuestionsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_INSPECCION_ID, inspeccionId)
                    putLong(ARG_CATEGORIA_ID, categoriaId)
                    putString(ARG_MODELO_CAEX, modeloCAEX)
                }
            }
        }
    }
}