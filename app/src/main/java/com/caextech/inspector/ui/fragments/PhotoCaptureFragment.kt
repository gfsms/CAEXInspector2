package com.caextech.inspector.ui.fragments

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.R
import com.caextech.inspector.databinding.FragmentPhotoCaptureBinding
import com.caextech.inspector.ui.viewmodels.FotoViewModel
import com.caextech.inspector.utils.FileUtils
import com.caextech.inspector.utils.Logger
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

/**
 * Fragmento de diálogo para capturar o seleccionar fotos para una respuesta No Conforme.
 */
class PhotoCaptureFragment : DialogFragment() {
    private val TAG = "PhotoCaptureFragment"

    private var _binding: FragmentPhotoCaptureBinding? = null
    private val binding get() = _binding!!

    private lateinit var fotoViewModel: FotoViewModel

    private var respuestaId: Long = 0

    // URI temporal para capturar foto
    private var tempImageUri: Uri? = null
    private var tempImagePath: String? = null

    // Weak reference to the activity context to prevent leaks
    private var activityContextRef: WeakReference<Context>? = null

    // Gestor para actividad de cámara
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    // Gestor para selección de galería
    private lateinit var selectPictureLauncher: ActivityResultLauncher<Intent>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Store a weak reference to the context
        activityContextRef = WeakReference(context)
        Logger.d(TAG, "Fragment attached to context")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener el ID de respuesta de los argumentos
        arguments?.let {
            respuestaId = it.getLong(ARG_RESPUESTA_ID, 0)
        }

        if (respuestaId == 0L) {
            Logger.e(TAG, "Error: ID de respuesta inválido")
            safeShowToast("Error: Se requiere un ID de respuesta válido")
            dismissSafely()
            return
        }

        // Initialize activity result launchers
        initActivityResultLaunchers()

        // Configurar el estilo del diálogo
        setStyle(STYLE_NORMAL, R.style.Theme_CAEXInspector_Dialog)

        Logger.d(TAG, "Fragment created with respuestaId: $respuestaId")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setWindowAnimations(R.style.DialogAnimation)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Logger.d(TAG, "Fragment view created")

        try {
            // Inicializar ViewModel - use the application context which is always available
            val application = requireActivity().application as CAEXInspectorApp
            fotoViewModel = ViewModelProvider(
                this,
                FotoViewModel.FotoViewModelFactory(application.fotoRepository)
            )[FotoViewModel::class.java]

            // Configurar botones
            setupButtons()

            // Observar eventos del ViewModel
            observeViewModel()
        } catch (e: Exception) {
            Logger.e(TAG, "Error in onViewCreated: ${e.message}", e)
            safeShowToast("Error al inicializar: ${e.message}")
            dismissSafely()
        }
    }

    /**
     * Initializes activity result launchers
     */
    private fun initActivityResultLaunchers() {
        Logger.d(TAG, "Initializing activity result launchers")

        // Launcher for taking a picture with the camera
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Logger.d(TAG, "Camera result received: ${result.resultCode}")

            if (!isAdded) {
                Logger.w(TAG, "Fragment not attached when camera result received")
                return@registerForActivityResult
            }

            if (result.resultCode == android.app.Activity.RESULT_OK) {
                try {
                    tempImagePath?.let { path ->
                        Logger.d(TAG, "Photo captured successfully, path: $path")
                        // Verify the file exists
                        val file = File(path)
                        if (file.exists() && file.length() > 0) {
                            guardarFoto(path)
                        } else {
                            Logger.e(TAG, "Photo file doesn't exist or is empty: $path")
                            safeShowToast("Error: La foto no se guardó correctamente")
                        }
                    } ?: run {
                        Logger.e(TAG, "No temporary image path available")
                        safeShowToast("Error: No se guardó la ruta de la imagen")
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Error processing camera result: ${e.message}", e)
                    safeShowToast("Error al procesar la foto: ${e.message}")
                }
            } else {
                Logger.d(TAG, "Camera capture cancelled or failed")
            }
        }

        // Launcher for selecting a picture from the gallery
        selectPictureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Logger.d(TAG, "Gallery result received: ${result.resultCode}")

            if (!isAdded) {
                Logger.w(TAG, "Fragment not attached when gallery result received")
                return@registerForActivityResult
            }

            if (result.resultCode == android.app.Activity.RESULT_OK) {
                try {
                    result.data?.data?.let { uri ->
                        Logger.d(TAG, "Photo selected from gallery: $uri")
                        processGalleryImage(uri)
                    } ?: run {
                        Logger.e(TAG, "No URI returned from gallery")
                        safeShowToast("Error: No se recibió la imagen seleccionada")
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Error processing gallery result: ${e.message}", e)
                    safeShowToast("Error al procesar la imagen seleccionada: ${e.message}")
                }
            }
        }
    }

    /**
     * Process an image selected from the gallery
     */
    private fun processGalleryImage(uri: Uri) {
        try {
            // Get the activity context safely
            val context = activityContextRef?.get() ?: requireContext()

            // Copy the image to our app's storage
            val path = FileUtils.copyImageToAppStorage(context, uri)
            if (path != null) {
                guardarFoto(path)
            } else {
                safeShowToast("Error al copiar la imagen seleccionada")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error processing gallery image: ${e.message}", e)
            safeShowToast("Error al procesar la imagen: ${e.message}")
        }
    }

    private fun setupButtons() {
        // Botón de tomar foto
        binding.cameraButton.setOnClickListener {
            capturarFoto()
        }

        // Botón de seleccionar de galería
        binding.galleryButton.setOnClickListener {
            seleccionarFoto()
        }

        // Botón de cancelar
        binding.cancelButton.setOnClickListener {
            dismissSafely()
        }
    }

    private fun observeViewModel() {
        // Observar eventos de operaciones
        fotoViewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            if (!isAdded) {
                Logger.w(TAG, "Fragment not attached when operation status received")
                return@observe
            }

            when (status) {
                is FotoViewModel.OperationStatus.Success -> {
                    Logger.d(TAG, "Photo operation successful: ${status.message}")
                    safeShowToast("Foto guardada correctamente")
                    dismissSafely()
                }
                is FotoViewModel.OperationStatus.Error -> {
                    Logger.e(TAG, "Photo operation error: ${status.message}")
                    safeShowToast("Error: ${status.message}")
                }
            }
        }
    }

    private fun capturarFoto() {
        try {
            if (!isAdded) {
                Logger.w(TAG, "Fragment not attached when trying to capture photo")
                return
            }

            Logger.d(TAG, "Starting photo capture")

            // Get the activity context safely
            val context = activityContextRef?.get() ?: requireContext()

            // Create a temporary file for the photo
            val (uri, path) = FileUtils.createTempImageFile(context)
            tempImageUri = uri
            tempImagePath = path
            Logger.d(TAG, "Temporary file created: $path")

            // Create camera intent
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
            }

            // Check if there's a camera app available
            val packageManager = context.packageManager
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                takePictureLauncher.launch(takePictureIntent)
                Logger.d(TAG, "Camera intent launched")
            } else {
                Logger.e(TAG, "No camera app available")
                safeShowToast("No se encontró una aplicación de cámara")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error starting camera: ${e.message}", e)
            safeShowToast("Error al iniciar la cámara: ${e.message}")
        }
    }

    private fun seleccionarFoto() {
        try {
            if (!isAdded) {
                Logger.w(TAG, "Fragment not attached when trying to select photo")
                return
            }

            Logger.d(TAG, "Starting gallery selection")

            // Create gallery intent
            val selectPictureIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            selectPictureLauncher.launch(selectPictureIntent)
            Logger.d(TAG, "Gallery intent launched")
        } catch (e: Exception) {
            Logger.e(TAG, "Error starting gallery: ${e.message}", e)
            safeShowToast("Error al abrir la galería: ${e.message}")
        }
    }

    private fun guardarFoto(path: String) {
        if (!isAdded) {
            Logger.w(TAG, "Fragment not attached when trying to save photo")
            return
        }

        Logger.d(TAG, "Saving photo at path: $path for respuestaId: $respuestaId")

        // Use viewLifecycleOwner.lifecycleScope to launch a coroutine
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val descripcion = binding.descriptionEditText.text.toString()
                fotoViewModel.guardarFotoDirecta(respuestaId, path, descripcion)
                Logger.d(TAG, "Photo save request sent to ViewModel")
            } catch (e: Exception) {
                Logger.e(TAG, "Error saving photo: ${e.message}", e)
                safeShowToast("Error al guardar la foto: ${e.message}")
            }
        }
    }

    /**
     * Shows a toast message safely, checking if the fragment is attached
     */
    private fun safeShowToast(message: String) {
        if (isAdded) {
            try {
                // Try to use the activity context first
                val context = activityContextRef?.get() ?: requireContext()
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Logger.e(TAG, "Error showing toast: ${e.message}", e)
                // If all else fails, try using the application context as a last resort
                try {
                    Toast.makeText(requireActivity().applicationContext, message, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Logger.e(TAG, "Could not show toast with application context either: ${e.message}")
                }
            }
        } else {
            Logger.w(TAG, "Cannot show toast - fragment not attached. Message: $message")
        }
    }

    /**
     * Dismisses the dialog safely, checking if it's attached and not already dismissed
     */
    private fun dismissSafely() {
        if (isAdded && !requireActivity().isFinishing) {
            try {
                dismiss()
                Logger.d(TAG, "Dialog dismissed safely")
            } catch (e: Exception) {
                Logger.e(TAG, "Error dismissing dialog: ${e.message}", e)
            }
        } else {
            Logger.w(TAG, "Cannot dismiss - fragment not attached or activity finishing")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Logger.d(TAG, "Fragment view destroyed")
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        Logger.d(TAG, "Fragment detached from context")
        activityContextRef = null
    }

    companion object {
        private const val ARG_RESPUESTA_ID = "respuesta_id"

        fun newInstance(respuestaId: Long): PhotoCaptureFragment {
            return PhotoCaptureFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_RESPUESTA_ID, respuestaId)
                }
            }
        }
    }
}