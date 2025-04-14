package com.caextech.inspector.ui.inspection

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.R
import com.caextech.inspector.databinding.ActivityCreateInspectionBinding
import com.caextech.inspector.ui.viewmodels.InspeccionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Actividad para crear una nueva inspección de "Control Inicio de Intervención".
 * Permite seleccionar el modelo CAEX, ingresar su ID, nombre del inspector y del supervisor.
 */
class CreateInspectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateInspectionBinding
    private lateinit var inspeccionViewModel: InspeccionViewModel

    // Variables para validación
    private var modeloSeleccionado: String = ""
    private var idEsValido: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar ViewBinding
        binding = ActivityCreateInspectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Inicializar ViewModel
        val application = application as CAEXInspectorApp
        inspeccionViewModel = ViewModelProvider(
            this,
            InspeccionViewModel.InspeccionViewModelFactory(
                application.inspeccionRepository,
                application.caexRepository  // Aseguramos pasar el CAEXRepository
            )
        )[InspeccionViewModel::class.java]

        // Configurar fecha y hora actual (no editable)
        actualizarFechaHora()

        // Configurar listeners
        setupListeners()

        // Observar estado de operaciones
        observarEstadoOperaciones()
    }

    /**
     * Configura los listeners para los elementos de la UI.
     */
    private fun setupListeners() {
        // Listeners para selección de modelo
        binding.modelRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            modeloSeleccionado = when (checkedId) {
                R.id.radio797F -> "797F"
                R.id.radio798AC -> "798AC"
                else -> ""
            }
            validarID()
        }

        // Listener para cambios en el ID
        binding.caexIdEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validarID()
            }
        })

        // Listener para botón de iniciar inspección
        binding.startInspectionButton.setOnClickListener {
            iniciarInspeccion()
        }
    }

    /**
     * Actualiza el campo de fecha y hora con la fecha y hora actual.
     */
    private fun actualizarFechaHora() {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val fechaHoraActual = sdf.format(Date())
        binding.dateTimeEditText.setText(fechaHoraActual)
    }

    /**
     * Valida si el ID ingresado es válido para el modelo seleccionado.
     */
    private fun validarID() {
        // Verificar si se ha seleccionado un modelo
        if (modeloSeleccionado.isEmpty()) {
            binding.idValidationTextView.text = "Seleccione un modelo primero"
            binding.idValidationTextView.setTextColor(getColor(android.R.color.darker_gray))
            idEsValido = false
            return
        }

        // Obtener el ID ingresado
        val idTexto = binding.caexIdEditText.text.toString()
        if (idTexto.isEmpty()) {
            binding.idValidationTextView.text = "Ingrese el ID del CAEX"
            binding.idValidationTextView.setTextColor(getColor(android.R.color.darker_gray))
            idEsValido = false
            return
        }

        // Convertir a entero
        val id = idTexto.toIntOrNull()
        if (id == null) {
            binding.idValidationTextView.text = "ID debe ser un número"
            binding.idValidationTextView.setTextColor(getColor(android.R.color.holo_red_dark))
            idEsValido = false
            return
        }

        // Validar según el modelo
        idEsValido = when (modeloSeleccionado) {
            "797F" -> (id in 301..339) || id == 365 || id == 366
            "798AC" -> id in 340..352
            else -> false
        }

        // Actualizar mensaje de validación
        if (idEsValido) {
            binding.idValidationTextView.text = "ID válido para $modeloSeleccionado"
            binding.idValidationTextView.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            binding.idValidationTextView.text = "ID no válido para $modeloSeleccionado"
            binding.idValidationTextView.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    /**
     * Inicia el proceso de inspección si todos los campos son válidos.
     */
    private fun iniciarInspeccion() {
        // Validar que todos los campos estén completos
        if (!validarCampos()) {
            return
        }

        // Mostrar indicador de carga
        binding.startInspectionButton.isEnabled = false
        binding.startInspectionButton.text = "Creando inspección..."

        // Obtener valores de los campos
        val caexId = binding.caexIdEditText.text.toString().toInt()
        val nombreInspector = binding.inspectorNameEditText.text.toString()
        val nombreSupervisor = binding.supervisorNameEditText.text.toString()

        // Buscar o crear el CAEX
        inspeccionViewModel.buscarCAEXPorNumeroYCrearInspeccion(
            caexId,
            modeloSeleccionado,
            nombreInspector,
            nombreSupervisor
        )
    }

    /**
     * Observa el estado de las operaciones del ViewModel.
     */
    private fun observarEstadoOperaciones() {
        inspeccionViewModel.operationStatus.observe(this) { status ->
            // Restaurar el botón
            binding.startInspectionButton.isEnabled = true
            binding.startInspectionButton.text = "Iniciar Inspección"

            when (status) {
                is InspeccionViewModel.OperationStatus.Success -> {
                    // Inspección creada exitosamente, navegar a la pantalla de inspección
                    Toast.makeText(this, status.message, Toast.LENGTH_SHORT).show()

                    // Iniciar la actividad de inspección pasando el ID de la inspección
                    val intent = Intent(this, InspectionQuestionnaireActivity::class.java).apply {
                        putExtra(EXTRA_INSPECCION_ID, status.id)
                    }
                    startActivity(intent)
                    finish()
                }
                is InspeccionViewModel.OperationStatus.Error -> {
                    // Mostrar mensaje de error
                    Toast.makeText(this, "Error: ${status.message}", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    /**
     * Valida que todos los campos obligatorios estén completos y sean válidos.
     *
     * @return true si todos los campos son válidos, false en caso contrario
     */
    private fun validarCampos(): Boolean {
        // Validar modelo seleccionado
        if (modeloSeleccionado.isEmpty()) {
            Toast.makeText(this, "Seleccione un modelo de CAEX", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar ID
        if (!idEsValido) {
            Toast.makeText(this, "ID de CAEX no válido", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar nombre del inspector
        if (binding.inspectorNameEditText.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Ingrese el nombre del inspector", Toast.LENGTH_SHORT).show()
            binding.inspectorNameEditText.requestFocus()
            return false
        }

        // Validar nombre del supervisor
        if (binding.supervisorNameEditText.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Ingrese el nombre del supervisor", Toast.LENGTH_SHORT).show()
            binding.supervisorNameEditText.requestFocus()
            return false
        }

        return true
    }

    /**
     * Maneja el comportamiento del botón Back en la toolbar.
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_INSPECCION_ID = "extra_inspeccion_id"
    }
}