package com.caextech.inspector.ui.backup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.databinding.ActivityBackupRestoreBinding
import com.caextech.inspector.ui.viewmodels.BackupRestoreViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.caextech.inspector.ui.viewmodels.BackupRestoreViewModel.OperationState.*

/**
 * Actividad para gestionar el respaldo y restauración de datos.
 *
 * Esta actividad permite al usuario:
 * - Exportar todos los datos de la aplicación (base de datos + fotos) a un archivo ZIP
 * - Importar datos desde un archivo ZIP previamente exportado
 *
 * El proceso maneja permisos de almacenamiento según la versión de Android
 * y proporciona retroalimentación visual durante las operaciones.
 */
class BackupRestoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBackupRestoreBinding
    private lateinit var viewModel: BackupRestoreViewModel

    // Launcher para seleccionar archivo de importación
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Cuando el usuario selecciona un archivo, iniciamos la importación
            showImportConfirmationDialog(it)
        }
    }

    // Launcher para manejar permisos en Android 11+
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Verificar si el permiso fue otorgado después de volver de configuración
        checkAndRequestPermissions()
    }

    // Identificador de operación pendiente después de obtener permisos
    private var pendingOperation: PendingOperation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflar el layout
        binding = ActivityBackupRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Respaldo y Restauración"
        }

        // Inicializar ViewModel con todos los repositorios necesarios
        val app = application as CAEXInspectorApp
        viewModel = ViewModelProvider(
            this,
            BackupRestoreViewModel.BackupRestoreViewModelFactory(
                app.database,
                app.fotoRepository,
                app
            )
        )[BackupRestoreViewModel::class.java]

        // Configurar listeners de botones
        setupButtonListeners()

        // Observar el estado de las operaciones
        observeViewModel()
    }

    /**
     * Configura los listeners para los botones de exportar e importar
     */
    private fun setupButtonListeners() {
        // Botón de exportar
        binding.exportButton.setOnClickListener {
            pendingOperation = PendingOperation.EXPORT
            if (checkAndRequestPermissions()) {
                startExport()
            }
        }

        // Botón de importar
        binding.importButton.setOnClickListener {
            pendingOperation = PendingOperation.IMPORT
            if (checkAndRequestPermissions()) {
                startImport()
            }
        }
    }

    /**
     * Observa los cambios en el ViewModel para actualizar la UI
     */
    private fun observeViewModel() {
        viewModel.operationState.observe(this) { state ->
            when (state) {
                is Idle -> {
                    setUIEnabled(true)
                    binding.progressBar.visibility = View.GONE
                    binding.progressText.text = ""
                }
                is InProgress -> {
                    setUIEnabled(false)
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressText.text = state.message
                }
                is Success -> {
                    setUIEnabled(true)
                    binding.progressBar.visibility = View.GONE
                    if (state.filePath != null) {
                        showSuccessDialog(state.message, state.filePath)
                    } else {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                }
                is Error -> {
                    setUIEnabled(true)
                    binding.progressBar.visibility = View.GONE
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Error")
                        .setMessage(state.message)
                        .setPositiveButton("Aceptar", null)
                        .show()
                }
            }
        }

        // Observar el progreso detallado
        viewModel.progressMessage.observe(this) { message ->
            binding.progressText.text = message
        }
    }

    /**
     * Verifica y solicita los permisos necesarios según la versión de Android
     *
     * @return true si los permisos están otorgados, false si se necesita solicitarlos
     */
    private fun checkAndRequestPermissions(): Boolean {
        return when {
            // Android 11 (API 30) y superior - usar MANAGE_EXTERNAL_STORAGE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) {
                    true
                } else {
                    requestManageStoragePermission()
                    false
                }
            }
            // Android 10 (API 29) - no necesita permisos especiales para app-specific directory
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                true
            }
            // Android 9 y anterior - usar WRITE_EXTERNAL_STORAGE
            else -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    true
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSION_REQUEST_CODE
                    )
                    false
                }
            }
        }
    }

    /**
     * Solicita el permiso MANAGE_EXTERNAL_STORAGE para Android 11+
     */
    private fun requestManageStoragePermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permiso Requerido")
            .setMessage("Para exportar e importar datos, la aplicación necesita acceso al almacenamiento. Por favor, habilite el permiso en la siguiente pantalla.")
            .setPositiveButton("Continuar") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    addCategory("android.intent.category.DEFAULT")
                    data = Uri.parse("package:${applicationContext.packageName}")
                }
                storagePermissionLauncher.launch(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Maneja el resultado de la solicitud de permisos
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso otorgado, ejecutar la operación pendiente
                when (pendingOperation) {
                    PendingOperation.EXPORT -> startExport()
                    PendingOperation.IMPORT -> startImport()
                    null -> {} // No hay operación pendiente
                }
            } else {
                // Permiso denegado
                Toast.makeText(
                    this,
                    "Se requiere permiso de almacenamiento para esta operación",
                    Toast.LENGTH_LONG
                ).show()
            }
            pendingOperation = null
        }
    }

    /**
     * Inicia el proceso de exportación
     */
    private fun startExport() {
        viewModel.exportData()
    }

    /**
     * Inicia el proceso de importación abriendo el selector de archivos
     */
    private fun startImport() {
        // Abrir selector de archivos para archivos ZIP
        filePickerLauncher.launch("application/zip")
    }

    /**
     * Muestra un diálogo de confirmación antes de importar
     */
    private fun showImportConfirmationDialog(fileUri: Uri) {
        val options = arrayOf("Reemplazar todos los datos", "Combinar con datos existentes")

        MaterialAlertDialogBuilder(this)
            .setTitle("Modo de Importación")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Reemplazar todos los datos
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Confirmar Reemplazo")
                            .setMessage("¿Está seguro de reemplazar TODOS los datos actuales? Esta acción no se puede deshacer.")
                            .setPositiveButton("Reemplazar") { _, _ ->
                                viewModel.importData(fileUri, replaceAll = true)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                    1 -> {
                        // Combinar datos
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Confirmar Combinación")
                            .setMessage("Se agregarán los datos del respaldo a los datos existentes. Las inspecciones y respuestas se duplicarán si ya existen.")
                            .setPositiveButton("Combinar") { _, _ ->
                                viewModel.importData(fileUri, replaceAll = false)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Muestra un diálogo de éxito con la ruta del archivo exportado
     */
    private fun showSuccessDialog(message: String, filePath: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Exportación Exitosa")
            .setMessage("$message\n\nArchivo guardado en:\n$filePath\n\nPuede subir este archivo a SharePoint para compartirlo.")
            .setPositiveButton("Aceptar", null)
            .show()
    }

    /**
     * Habilita o deshabilita la UI durante las operaciones
     */
    private fun setUIEnabled(enabled: Boolean) {
        binding.exportButton.isEnabled = enabled
        binding.importButton.isEnabled = enabled
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Enum para identificar la operación pendiente después de obtener permisos
     */
    private enum class PendingOperation {
        EXPORT, IMPORT
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}