package com.caextech.inspector.ui.inspection

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.MainActivity
import com.caextech.inspector.R
import com.caextech.inspector.data.entities.Respuesta
import com.caextech.inspector.databinding.ActivityNoConformeSummaryBinding
import com.caextech.inspector.ui.adapters.NoConformeAdapter
import com.caextech.inspector.ui.viewmodels.InspeccionViewModel
import com.caextech.inspector.ui.viewmodels.RespuestaViewModel
import com.caextech.inspector.utils.PdfGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log

/**
 * Activity for displaying and finalizing the summary of No Conforme responses.
 * Allows adding SAP IDs, selecting action types, and generating a PDF report.
 */
class NoConformeSummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoConformeSummaryBinding

    // ViewModels
    private lateinit var inspeccionViewModel: InspeccionViewModel
    private lateinit var respuestaViewModel: RespuestaViewModel

    // Adapter
    private lateinit var noConformeAdapter: NoConformeAdapter

    // Inspection data
    private var inspeccionId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityNoConformeSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configure Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get inspection ID from intent
        inspeccionId = intent.getLongExtra(EXTRA_INSPECCION_ID, 0)
        if (inspeccionId == 0L) {
            Toast.makeText(this, "Error: No se especificó una inspección", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize ViewModels
        initViewModels()

        // Set up RecyclerView
        setupRecyclerView()

        // Load inspection data
        loadInspectionData()

        // Set up button listeners
        setupButtonListeners()
    }

    /**
     * Initializes the ViewModels used in this activity.
     */
    private fun initViewModels() {
        val application = application as CAEXInspectorApp

        inspeccionViewModel = ViewModelProvider(
            this,
            InspeccionViewModel.InspeccionViewModelFactory(
                application.inspeccionRepository,
                application.caexRepository
            )
        )[InspeccionViewModel::class.java]

        respuestaViewModel = ViewModelProvider(
            this,
            RespuestaViewModel.RespuestaViewModelFactory(
                application.respuestaRepository
            )
        )[RespuestaViewModel::class.java]
    }

    /**
     * Sets up the RecyclerView with the NoConformeAdapter.
     */
    private fun setupRecyclerView() {
        noConformeAdapter = NoConformeAdapter(
            onSapIdUpdated = { respuestaId, tipoAccion, sapId ->
                // Update the action type and SAP ID for this response
                updateRespuestaAction(respuestaId, tipoAccion, sapId)
            }
        )

        binding.noConformeRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NoConformeSummaryActivity)
            adapter = noConformeAdapter
        }
    }

    /**
     * Loads the inspection data and No Conforme responses.
     */
    private fun loadInspectionData() {
        // Load inspection details
        inspeccionViewModel.getInspeccionConCAEXById(inspeccionId).observe(this) { inspeccionConCAEX ->
            if (inspeccionConCAEX == null) {
                Toast.makeText(this, "Error: Inspección no encontrada", Toast.LENGTH_SHORT).show()
                finish()
                return@observe
            }

            // Set title with inspection info
            binding.inspectionTitleText.text = inspeccionConCAEX.getTituloDescriptivo()
        }

        // Load No Conforme responses
        respuestaViewModel.getRespuestasConDetallesByInspeccionYEstado(
            inspeccionId,
            Respuesta.ESTADO_NO_CONFORME
        ).observe(this) { noConformes ->
            // Update adapter with No Conforme responses
            noConformeAdapter.submitList(noConformes)

            // Update summary text
            binding.summaryText.text = "Total de ítems No Conformes: ${noConformes.size}"
        }
    }

    /**
     * Sets up listeners for the buttons.
     */
    private fun setupButtonListeners() {
        // Generate PDF button
        binding.generatePdfButton.setOnClickListener {
            generatePdf()
        }

        // Complete inspection button
        binding.completeInspectionButton.setOnClickListener {
            completeInspection()
        }
    }

    /**
     * Updates a response with action type and SAP ID.
     */
    private fun updateRespuestaAction(respuestaId: Long, tipoAccion: String, sapId: String) {
        // Use lifecycleScope to launch a coroutine
        lifecycleScope.launch {
            try {
                // Validar entrada
                if (sapId.isBlank()) {
                    // No mostrar Toast aquí, la validación visual ya se muestra en la UI
                    return@launch
                }

                // Log para depuración
                Log.d("NoConformeSummary", "Actualizando respuesta $respuestaId: tipoAccion=$tipoAccion, sapId=$sapId")

                // Actualizar la respuesta
                val result = respuestaViewModel.actualizarRespuestaNoConforme(
                    respuestaId,
                    "", // Mantenemos los comentarios existentes
                    tipoAccion,
                    sapId
                )

                // Verificar el resultado
                if (!result) {
                    Log.e("NoConformeSummary", "Error al actualizar la respuesta $respuestaId")
                    Toast.makeText(
                        this@NoConformeSummaryActivity,
                        "Error al actualizar la respuesta",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.d("NoConformeSummary", "Respuesta $respuestaId actualizada correctamente")
                }
            } catch (e: Exception) {
                Log.e("NoConformeSummary", "Excepción al actualizar respuesta: ${e.message}", e)
                Toast.makeText(
                    this@NoConformeSummaryActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    /**
     * Generates a PDF report with the No Conforme responses.
     */
    private fun generatePdf() {
        // Create a timestamp for the filename
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Inspeccion_${inspeccionId}_${timestamp}.pdf"

        // Create directory in external storage
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val pdfFile = File(storageDir, fileName)

        // Get No Conforme responses
        respuestaViewModel.getRespuestasConDetallesByInspeccionYEstado(
            inspeccionId,
            Respuesta.ESTADO_NO_CONFORME
        ).observe(this) { noConformes ->
            // Generate PDF
            try {
                PdfGenerator.generatePdf(
                    this,
                    inspeccionId,
                    noConformes,
                    pdfFile
                )

                // Share the PDF
                sharePdf(pdfFile)
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Error al generar PDF: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Shares the generated PDF file.
     */
    private fun sharePdf(pdfFile: File) {
        // Get the file URI using FileProvider
        val fileUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            pdfFile
        )

        // Create intent to view the PDF
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Check if there's an app that can handle the intent
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // If no PDF viewer is installed, show a share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Compartir PDF"))
        }
    }

    /**
     * Completes the inspection and navigates back to the main screen.
     */
    private fun completeInspection() {
        // Check if all required fields are filled
        if (!validateInputs()) {
            return
        }

        // Get general comments
        val comments = binding.generalCommentsEditText.text.toString()

        // Show confirmation dialog
        AlertDialog.Builder(this)
            .setTitle("Completar Inspección")
            .setMessage("¿Está seguro de que desea completar la inspección? Esta acción no se puede deshacer.")
            .setPositiveButton("Sí, Completar") { _, _ ->
                // Complete the inspection
                inspeccionViewModel.cerrarInspeccion(inspeccionId, comments).observe(this) { success ->
                    if (success) {
                        Toast.makeText(
                            this,
                            "Inspección completada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Go back to main activity
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Error al completar la inspección. Asegúrese de que todos los campos requeridos estén completos.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Validates that all required inputs are filled.
     */
    private fun validateInputs(): Boolean {
        // Check if all No Conforme items have action type and SAP ID
        val incompleteItems = noConformeAdapter.getIncompleteItems()

        if (incompleteItems.isNotEmpty()) {
            // Show error message
            Toast.makeText(
                this,
                "Hay ${incompleteItems.size} ítems No Conforme sin tipo de acción o ID SAP",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        return true
    }

    /**
     * Handles the Up/Back navigation in the ActionBar.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_INSPECCION_ID = "extra_inspeccion_id"
    }
}