package com.caextech.inspector.ui.fragments

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.R
import com.caextech.inspector.databinding.FragmentPhotoCaptureBinding
import com.caextech.inspector.ui.viewmodels.FotoViewModel
import com.caextech.inspector.utils.FileUtils
import kotlinx.coroutines.launch

/**
 * Fragmento de diálogo para capturar o seleccionar fotos para una respuesta No Conforme.
 */
class PhotoCaptureFragment : DialogFragment() {

    private var _binding: FragmentPhotoCaptureBinding? = null
    private val binding get() = _binding!!

    private lateinit var fotoViewModel: FotoViewModel

    private var respuestaId: Long = 0

    // URI temporal para capturar foto
    private var tempImageUri: Uri? = null
    private var tempImagePath: String? = null

    // Gestor para actividad de cámara
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // La foto fue tomada con éxito
            tempImagePath?.let { path ->
                guardarFoto(path)
            } ?: run {
                Toast.makeText(
                    requireContext(),
                    "Error: No se pudo obtener la ruta de la imagen",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Gestor para selección de galería
    private val selectPictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // La foto fue seleccionada de la galería
            result.data?.data?.let { uri ->
                // Copiar la imagen a nuestra app
                val path = FileUtils.copyImageToAppStorage(requireContext(), uri)
                if (path != null) {
                    guardarFoto(path)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error al copiar la imagen seleccionada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener el ID de respuesta de los argumentos
        arguments?.let {
            respuestaId = it.getLong(ARG_RESPUESTA_ID, 0)
        }

        if (respuestaId == 0L) {
            Toast.makeText(
                requireContext(),
                "Error: Se requiere un ID de respuesta válido",
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
        }

        // Configurar el estilo del diálogo
        setStyle(STYLE_NORMAL, R.style.Theme_CAEXInspector_Dialog)
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

        // Inicializar ViewModel
        val application = requireActivity().application as CAEXInspectorApp
        fotoViewModel = ViewModelProvider(
            this,
            FotoViewModel.FotoViewModelFactory(application.fotoRepository)
        )[FotoViewModel::class.java]

        // Configurar botones
        setupButtons()

        // Observar eventos del ViewModel
        observeViewModel()
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
            dismiss()
        }
    }

    private fun observeViewModel() {
        // Observar eventos de operaciones
        fotoViewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is FotoViewModel.OperationStatus.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "Foto guardada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                }
                is FotoViewModel.OperationStatus.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "Error: ${status.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun capturarFoto() {
        try {
            // Crear archivo temporal para la foto
            val (uri, path) = FileUtils.createTempImageFile(requireContext())
            tempImageUri = uri
            tempImagePath = path

            // Iniciar la cámara con el URI del archivo
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
            }

            takePictureLauncher.launch(takePictureIntent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error al iniciar la cámara: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun seleccionarFoto() {
        // Iniciar la selección de foto de la galería
        val selectPictureIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selectPictureLauncher.launch(selectPictureIntent)
    }

    private fun guardarFoto(path: String) {
        // Usar lifecycleScope para ejecutar la función suspendida
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val descripcion = binding.descriptionEditText.text.toString()
                fotoViewModel.guardarFotoDirecta(respuestaId, path, descripcion)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al guardar la foto: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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