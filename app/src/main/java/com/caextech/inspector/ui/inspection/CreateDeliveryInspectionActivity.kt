package com.caextech.inspector.ui.inspection

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.R
import com.caextech.inspector.databinding.ActivityCreateDeliveryInspectionBinding
import com.caextech.inspector.ui.viewmodels.InspeccionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity for creating a new delivery inspection linked to a reception inspection.
 * Collects inspector and supervisor names and creates the delivery inspection.
 */
class CreateDeliveryInspectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateDeliveryInspectionBinding
    private lateinit var inspeccionViewModel: InspeccionViewModel

    // Data from intent
    private var inspeccionRecepcionId: Long = 0
    private var caexId: Long = 0
    private var caexNombre: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityCreateDeliveryInspectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configure toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get data from intent
        inspeccionRecepcionId = intent.getLongExtra(EXTRA_INSPECCION_RECEPCION_ID, 0)
        caexId = intent.getLongExtra(EXTRA_CAEX_ID, 0)
        caexNombre = intent.getStringExtra(EXTRA_CAEX_NOMBRE) ?: ""

        if (inspeccionRecepcionId == 0L || caexId == 0L) {
            Toast.makeText(this, "Error: Datos de inspección incorrectos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize ViewModel
        val application = application as CAEXInspectorApp
        inspeccionViewModel = ViewModelProvider(
            this,
            InspeccionViewModel.InspeccionViewModelFactory(
                application.inspeccionRepository,
                application.caexRepository
            )
        )[InspeccionViewModel::class.java]

        // Set up UI with CAEX information
        setupUI()

        // Observe state of operations
        observeOperationStatus()

        // Set up button listener
        binding.createDeliveryButton.setOnClickListener {
            createDeliveryInspection()
        }
    }

    /**
     * Sets up the UI with CAEX information.
     */
    private fun setupUI() {
        // Set CAEX info
        binding.caexInfoText.text = caexNombre

        // Set current date/time
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val dateTimeStr = sdf.format(Date())
        binding.dateTimeEditText.setText(dateTimeStr)
    }

    /**
     * Observes the operation status from the ViewModel.
     */
    private fun observeOperationStatus() {
        inspeccionViewModel.operationStatus.observe(this) { status ->
            Log.d("DeliveryDebug", "Received operation status: $status")

            when (status) {
                is InspeccionViewModel.OperationStatus.Success -> {
                    Log.d("DeliveryDebug", "Success creating delivery inspection with ID: ${status.id}")
                    Toast.makeText(this, status.message, Toast.LENGTH_SHORT).show()

                    // Start questionnaire activity with the new inspection ID
                    val intent = Intent(this, InspectionQuestionnaireActivity::class.java).apply {
                        putExtra(InspectionQuestionnaireActivity.EXTRA_INSPECCION_ID, status.id)
                    }
                    startActivity(intent)
                    finish()
                }
                is InspeccionViewModel.OperationStatus.Error -> {
                    Log.e("DeliveryDebug", "Error creating delivery inspection: ${status.message}")
                    binding.createDeliveryButton.isEnabled = true
                    binding.createDeliveryButton.text = getString(R.string.create_delivery_inspection)
                    Toast.makeText(this, "Error: ${status.message}", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    /**
     * Creates a delivery inspection if all fields are valid.
     */
    private fun createDeliveryInspection() {
        // Validate fields
        if (!validateFields()) {
            return
        }

        // Show loading state
        binding.createDeliveryButton.isEnabled = false
        binding.createDeliveryButton.text = "Creando inspección..."



        // Get names from UI
        val inspectorName = binding.inspectorNameEditText.text.toString().trim()
        val supervisorName = binding.supervisorNameEditText.text.toString().trim()

        Log.d("DeliveryDebug", "Creating delivery inspection for reception ID: $inspeccionRecepcionId")


        // Create delivery inspection
        inspeccionViewModel.crearInspeccionEntrega(
            inspeccionRecepcionId,
            inspectorName,
            supervisorName
        )
        Log.d("DeliveryDebug", "Called inspeccionViewModel.crearInspeccionEntrega")
    }

    /**
     * Validates that all required fields are filled.
     */
    private fun validateFields(): Boolean {
        // Validate inspector name
        if (binding.inspectorNameEditText.text.toString().trim().isEmpty()) {
            binding.inspectorNameEditText.error = "El nombre del inspector es requerido"
            binding.inspectorNameEditText.requestFocus()
            return false
        }

        // Validate supervisor name
        if (binding.supervisorNameEditText.text.toString().trim().isEmpty()) {
            binding.supervisorNameEditText.error = "El nombre del supervisor es requerido"
            binding.supervisorNameEditText.requestFocus()
            return false
        }

        return true
    }

    /**
     * Handles the Up/Back button in the toolbar.
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_INSPECCION_RECEPCION_ID = "extra_inspeccion_recepcion_id"
        const val EXTRA_CAEX_ID = "extra_caex_id"
        const val EXTRA_CAEX_NOMBRE = "extra_caex_nombre"
    }
}