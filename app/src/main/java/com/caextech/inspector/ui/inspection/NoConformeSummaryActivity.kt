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
import com.caextech.inspector.data.entities.CAEX
import com.caextech.inspector.data.entities.Inspeccion
import com.caextech.inspector.data.entities.Respuesta
import com.caextech.inspector.data.relations.RespuestaConDetalles
import com.caextech.inspector.databinding.ActivityNoConformeSummaryBinding
import com.caextech.inspector.ui.adapters.NoConformeAdapter
import com.caextech.inspector.ui.adapters.RechazadoAdapter
import com.caextech.inspector.ui.fragments.HistorialRespuestasFragment
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
 * Activity for displaying and finalizing the summary of responses.
 * Handles both reception (No Conforme) and delivery (Rechazado) inspections.
 */
class NoConformeSummaryActivity : AppCompatActivity(), HistorialRespuestasFragment.OnIdSapSelectedListener {

    private var respuestaSeleccionadaId: Long = 0
    private lateinit var caexActual: CAEX

    private lateinit var binding: ActivityNoConformeSummaryBinding

    // ViewModels
    private lateinit var inspeccionViewModel: InspeccionViewModel
    private lateinit var respuestaViewModel: RespuestaViewModel

    // Adapters
    private lateinit var noConformeAdapter: NoConformeAdapter
    private lateinit var rechazadoAdapter: RechazadoAdapter

    // Inspection data
    private var inspeccionId: Long = 0
    private var inspeccionRecepcionId: Long? = null
    private var tipoInspeccion: String = Inspeccion.TIPO_RECEPCION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityNoConformeSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configure Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get inspection data from intent
        inspeccionId = intent.getLongExtra(EXTRA_INSPECCION_ID, 0)
        tipoInspeccion = intent.getStringExtra(EXTRA_INSPECCION_TIPO) ?: Inspeccion.TIPO_RECEPCION
        inspeccionRecepcionId = intent.getLongExtra(EXTRA_INSPECCION_RECEPCION_ID, 0).takeIf { it > 0 }

        if (inspeccionId == 0L) {
            Toast.makeText(this, "Error: No se especificó una inspección", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize ViewModels
        initViewModels()

        // Set up RecyclerViews
        setupRecyclerViews()

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
     * Sets up the RecyclerViews for NoConforme and Rechazado items.
     */
    private fun setupRecyclerViews() {
        // Setup adapter for "No Conforme" responses
        noConformeAdapter = NoConformeAdapter(
            onSapIdUpdated = { respuestaId, tipoAccion, sapId ->
                // For reception inspections, update with SAP ID
                // For delivery inspections, this is read-only
                if (tipoInspeccion == Inspeccion.TIPO_RECEPCION) {
                    updateRespuestaAction(respuestaId, tipoAccion, sapId)
                }
            },
            onVerHistorialClicked = { respuestaConDetalles, _ ->
                showHistorialDialog(respuestaConDetalles)
            },
            isReadOnly = tipoInspeccion == Inspeccion.TIPO_ENTREGA // Read-only in delivery inspection
        )

        binding.noConformeRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NoConformeSummaryActivity)
            adapter = noConformeAdapter
        }

        // Setup adapter for "Rechazado" responses (only for delivery inspections)
        if (tipoInspeccion == Inspeccion.TIPO_ENTREGA) {
            rechazadoAdapter = RechazadoAdapter(
                onSapIdUpdated = { respuestaId, tipoAccion, sapId ->
                    updateRespuestaAction(respuestaId, tipoAccion, sapId)
                },
                onVerHistorialClicked = { respuestaConDetalles, _ ->
                    showHistorialDialog(respuestaConDetalles)
                }
            )

            binding.rechazadoRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@NoConformeSummaryActivity)
                adapter = rechazadoAdapter
            }
        }
    }

    /**
     * Loads the inspection data and responses.
     */
    private fun loadInspectionData() {
        // Load inspection details
        inspeccionViewModel.getInspeccionConCAEXById(inspeccionId).observe(this) { inspeccionConCAEX ->
            if (inspeccionConCAEX == null) {
                Toast.makeText(this, "Error: Inspección no encontrada", Toast.LENGTH_SHORT).show()
                finish()
                return@observe
            }

            // Store the CAEX for later use
            caexActual = inspeccionConCAEX.caex

            // Pass the CAEX ID to the adapters' ViewHolders
            val viewHolders = binding.noConformeRecyclerView.findViewHolderForAdapterPosition(0)
            if (viewHolders is NoConformeAdapter.NoConformeViewHolder) {
                viewHolders.caexId = caexActual.caexId
            }

            if (tipoInspeccion == Inspeccion.TIPO_ENTREGA) {
                val rechazadoViewHolders = binding.rechazadoRecyclerView.findViewHolderForAdapterPosition(0)
                if (rechazadoViewHolders is RechazadoAdapter.RechazadoViewHolder) {
                    rechazadoViewHolders.caexId = caexActual.caexId
                }
            }

            // Set title and adjust UI based on inspection type
            if (tipoInspeccion == Inspeccion.TIPO_RECEPCION) {
                // For reception inspections, show only NoConforme section
                binding.inspectionTitleText.text = "Inspección de Recepción - ${inspeccionConCAEX.caex.getNombreCompleto()}"
                title = getString(R.string.no_conforme_summary)

                binding.noConformeSectionLabel.text = getString(R.string.no_conforme_items_reception)
                binding.rechazadoSection.visibility = android.view.View.GONE
            } else {
                // For delivery inspections, show both sections
                binding.inspectionTitleText.text = "Inspección de Entrega - ${inspeccionConCAEX.caex.getNombreCompleto()}"
                title = getString(R.string.delivery_inspection_summary)

                binding.noConformeSectionLabel.text = getString(R.string.no_conforme_items_reception)
                binding.rechazadoSectionLabel.text = getString(R.string.rejected_items_delivery)
                binding.rechazadoSection.visibility = android.view.View.VISIBLE

                // Load no conforme items from reception inspection
                if (inspeccionRecepcionId != null) {
                    loadRecepcionItems(inspeccionRecepcionId!!)
                }
            }
        }

        // Load responses based on inspection type
        if (tipoInspeccion == Inspeccion.TIPO_RECEPCION) {
            // For reception inspections, load "No Conforme" responses
            respuestaViewModel.getRespuestasConDetallesByInspeccionYEstado(
                inspeccionId,
                Respuesta.ESTADO_NO_CONFORME
            ).observe(this) { noConformes ->
                // Update adapter with No Conforme responses
                noConformeAdapter.submitList(noConformes)

                // Update summary text
                binding.summaryText.text = "Total de ítems No Conformes: ${noConformes.size}"

                // Set CAEX ID for all ViewHolders
                for (i in 0 until binding.noConformeRecyclerView.childCount) {
                    val viewHolder = binding.noConformeRecyclerView.getChildViewHolder(binding.noConformeRecyclerView.getChildAt(i))
                    if (viewHolder is NoConformeAdapter.NoConformeViewHolder) {
                        viewHolder.caexId = caexActual.caexId
                    }
                }
            }
        } else {
            // For delivery inspections, load "Rechazado" responses
            respuestaViewModel.getRespuestasConDetallesByInspeccionYEstado(
                inspeccionId,
                Respuesta.ESTADO_RECHAZADO
            ).observe(this) { rechazados ->
                // Update adapter with Rechazado responses
                rechazadoAdapter.submitList(rechazados)

                // Update summary text
                binding.summaryText.text = "Total de ítems Rechazados: ${rechazados.size}"

                // Set CAEX ID for all ViewHolders
                for (i in 0 until binding.rechazadoRecyclerView.childCount) {
                    val viewHolder = binding.rechazadoRecyclerView.getChildViewHolder(binding.rechazadoRecyclerView.getChildAt(i))
                    if (viewHolder is RechazadoAdapter.RechazadoViewHolder) {
                        viewHolder.caexId = caexActual.caexId
                    }
                }
            }
        }
    }

    private fun showHistorialDialog(respuestaConDetalles: RespuestaConDetalles) {
        // Guardar referencia
        respuestaSeleccionadaId = respuestaConDetalles.respuesta.respuestaId

        // Mostrar diálogo
        val dialog = HistorialRespuestasFragment.newInstance(
            caexActual.caexId,
            respuestaConDetalles.pregunta.preguntaId,
            inspeccionId,
            respuestaConDetalles.pregunta.texto
        )
        dialog.show(supportFragmentManager, "HistorialDialog")
    }

    override fun onIdSapSelected(tipoAccion: String, idSap: String) {
        if (respuestaSeleccionadaId > 0 && idSap.isNotEmpty()) {
            // Actualizar UI
            noConformeAdapter.updateSapInfo(respuestaSeleccionadaId, tipoAccion, idSap)

            // Guardar en base de datos
            updateRespuestaAction(respuestaSeleccionadaId, tipoAccion, idSap)

            // Mensaje de éxito
            Toast.makeText(this, "ID SAP aplicado: $idSap", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Loads "No Conforme" items from the reception inspection.
     */
    private fun loadRecepcionItems(recepcionId: Long) {
        respuestaViewModel.getRespuestasConDetallesByInspeccionYEstado(
            recepcionId,
            Respuesta.ESTADO_NO_CONFORME
        ).observe(this) { noConformes ->
            // Update adapter with No Conforme responses from reception
            noConformeAdapter.submitList(noConformes)

            // Set CAEX ID for all ViewHolders
            for (i in 0 until binding.noConformeRecyclerView.childCount) {
                val viewHolder = binding.noConformeRecyclerView.getChildViewHolder(binding.noConformeRecyclerView.getChildAt(i))
                if (viewHolder is NoConformeAdapter.NoConformeViewHolder) {
                    viewHolder.caexId = caexActual.caexId
                }
            }
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

                // Actualizar la respuesta según el tipo de inspección
                val result = if (tipoInspeccion == Inspeccion.TIPO_RECEPCION) {
                    respuestaViewModel.actualizarRespuestaNoConforme(respuestaId, "", tipoAccion, sapId)
                } else {
                    respuestaViewModel.actualizarRespuestaRechazada(respuestaId, "", tipoAccion, sapId)
                }

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
     * Generates a PDF report with the responses.
     */
    private fun generatePdf() {
        // Create a timestamp for the filename
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val prefix = if (tipoInspeccion == Inspeccion.TIPO_RECEPCION) "Recepcion" else "Entrega"
        val fileName = "Inspeccion_${prefix}_${inspeccionId}_${timestamp}.pdf"

        // Create directory in external storage
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val pdfFile = File(storageDir, fileName)

        if (tipoInspeccion == Inspeccion.TIPO_RECEPCION) {
            // For reception inspections, generate PDF with No Conforme items
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
        } else {
            // For delivery inspections, generate PDF with both No Conforme and Rechazado items
            if (inspeccionRecepcionId != null) {
                generateDeliveryPdf(pdfFile, inspeccionId, inspeccionRecepcionId!!)
            } else {
                // If no reception inspection ID is available, only include Rechazado items
                respuestaViewModel.getRespuestasConDetallesByInspeccionYEstado(
                    inspeccionId,
                    Respuesta.ESTADO_RECHAZADO
                ).observe(this) { rechazados ->
                    // Generate PDF
                    try {
                        PdfGenerator.generatePdf(
                            this,
                            inspeccionId,
                            rechazados,
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
        }
    }

    /**
     * Generates a PDF for delivery inspections that includes both reception and delivery items.
     */
    private fun generateDeliveryPdf(pdfFile: File, entregaId: Long, recepcionId: Long) {
        // Get No Conforme responses from reception inspection
        respuestaViewModel.getRespuestasConDetallesByInspeccionYEstado(
            recepcionId,
            Respuesta.ESTADO_NO_CONFORME
        ).observe(this) { noConformes ->
            // Get Rechazado responses from delivery inspection
            respuestaViewModel.getRespuestasConDetallesByInspeccionYEstado(
                entregaId,
                Respuesta.ESTADO_RECHAZADO
            ).observe(this) { rechazados ->
                // Generate PDF with both sets of responses
                try {
                    PdfGenerator.generateDeliveryPdf(
                        this,
                        entregaId,
                        recepcionId,
                        noConformes,
                        rechazados,
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
        // For reception inspections, check if all No Conforme items have action type and SAP ID
        if (tipoInspeccion == Inspeccion.TIPO_RECEPCION) {
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
        } else {
            // For delivery inspections, check if all Rechazado items have action type and SAP ID
            val incompleteItems = rechazadoAdapter.getIncompleteItems()

            if (incompleteItems.isNotEmpty()) {
                // Show error message
                Toast.makeText(
                    this,
                    "Hay ${incompleteItems.size} ítems Rechazado sin tipo de acción o ID SAP",
                    Toast.LENGTH_LONG
                ).show()
                return false
            }
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
        const val EXTRA_INSPECCION_TIPO = "extra_inspeccion_tipo"
        const val EXTRA_INSPECCION_RECEPCION_ID = "extra_inspeccion_recepcion_id"
    }
}