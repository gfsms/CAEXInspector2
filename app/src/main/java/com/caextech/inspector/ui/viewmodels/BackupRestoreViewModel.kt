package com.caextech.inspector.ui.viewmodels

import android.app.Application
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.caextech.inspector.data.AppDatabase
import com.caextech.inspector.data.repository.FotoRepository
import com.caextech.inspector.utils.BackupUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipException

/**
 * ViewModel para gestionar las operaciones de respaldo y restauración.
 *
 * Este ViewModel coordina:
 * - La exportación de toda la base de datos a JSON
 * - La copia de todas las fotografías
 * - La creación del archivo ZIP final
 * - La importación y validación de datos
 * - La restauración de la base de datos y archivos
 *
 * Utiliza corrutinas para operaciones asíncronas y LiveData para comunicar
 * el estado a la UI de manera reactiva.
 */
class BackupRestoreViewModel(
    private val database: AppDatabase,
    private val fotoRepository: FotoRepository,
    application: Application
) : AndroidViewModel(application) {

    // Estado de la operación actual
    private val _operationState = MutableLiveData<OperationState>(OperationState.Idle)
    val operationState: LiveData<OperationState> = _operationState

    // Mensaje de progreso detallado
    private val _progressMessage = MutableLiveData<String>()
    val progressMessage: LiveData<String> = _progressMessage

    /**
     * Exporta todos los datos de la aplicación a un archivo ZIP.
     *
     * El proceso incluye:
     * 1. Crear estructura de directorios temporales
     * 2. Exportar toda la base de datos a JSON
     * 3. Copiar todas las fotografías
     * 4. Crear metadatos del respaldo
     * 5. Comprimir todo en un archivo ZIP
     * 6. Guardar en la carpeta Downloads del usuario
     */
    fun exportData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Cambiar estado a "en progreso"
                updateState(OperationState.InProgress("Preparando exportación..."))

                // Crear nombre de archivo con timestamp
                val timestamp = SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.getDefault())
                    .format(Date())
                val backupFileName = "CAEX_Backup_$timestamp.zip"

                // Determinar directorio de destino (Downloads)
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val backupFile = File(downloadsDir, backupFileName)

                // Actualizar progreso
                withContext(Dispatchers.Main) {
                    _progressMessage.value = "Exportando base de datos..."
                }

                // Realizar la exportación usando nuestra utilidad
                val success = BackupUtils.exportToZip(
                    context = getApplication(),
                    database = database,
                    outputFile = backupFile
                ) { progress ->
                    // Callback de progreso - necesitamos lanzar una corrutina para actualizar
                    viewModelScope.launch(Dispatchers.Main) {
                        _progressMessage.value = progress
                    }
                }

                if (success) {
                    // Exportación exitosa
                    updateState(
                        OperationState.Success(
                            "Respaldo creado exitosamente",
                            backupFile.absolutePath
                        )
                    )
                } else {
                    // Error en la exportación
                    updateState(
                        OperationState.Error("Error al crear el respaldo")
                    )
                }

            } catch (e: Exception) {
                // Manejar cualquier excepción no capturada
                e.printStackTrace()
                Log.e("BackupRestore", "Error en importación", e)

                val errorMessage = when (e) {
                    is ZipException -> "Error en archivo ZIP: ${e.message}"
                    is IOException -> "Error de archivo: ${e.message}"
                    is SecurityException -> "Error de permisos: ${e.message}"
                    is IllegalStateException -> "Error de estado: ${e.message}"
                    else -> "Error: ${e.javaClass.simpleName} - ${e.message ?: "Sin detalles"}"
                }

                updateState(OperationState.Error(errorMessage))
            }
        }
    }


    /**
     * Importa datos desde un archivo ZIP de respaldo.
     *
     * El proceso incluye:
     * 1. Validar el archivo ZIP
     * 2. Extraer y validar metadatos
     * 3. Verificar compatibilidad de versión
     * 4. Limpiar datos existentes (si replaceAll es true)
     * 5. Importar nuevos datos a la base de datos
     * 6. Restaurar fotografías con rutas correctas
     *
     * @param fileUri URI del archivo ZIP seleccionado por el usuario
     * @param replaceAll Si es true, reemplaza todos los datos. Si es false, combina con existentes
     */
    fun importData(fileUri: Uri, replaceAll: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Cambiar estado a "en progreso"
                updateState(OperationState.InProgress("Preparando importación..."))

                // Actualizar progreso
                withContext(Dispatchers.Main) {
                    _progressMessage.value = "Validando archivo de respaldo..."
                }

                // Realizar la importación usando nuestra utilidad
                val success = BackupUtils.importFromZip(
                    context = getApplication(),
                    database = database,
                    zipUri = fileUri,
                    replaceAll = replaceAll
                ) { progress ->
                    // Callback de progreso - necesitamos lanzar una corrutina para actualizar
                    viewModelScope.launch(Dispatchers.Main) {
                        _progressMessage.value = progress
                    }
                }

                if (success) {
                    // Importación exitosa
                    updateState(
                        OperationState.Success("Datos restaurados exitosamente")
                    )
                } else {
                    // Error en la importación
                    updateState(
                        OperationState.Error("Error al restaurar los datos")
                    )
                }

            } catch (e: Exception) {
                // Manejar cualquier excepción no capturada
                e.printStackTrace()
                Log.e("BackupRestore", "Error en exportación", e)

                val errorMessage = when (e) {
                    is IOException -> "Error de archivo: ${e.message}"
                    is SecurityException -> "Error de permisos: ${e.message}"
                    else -> "Error: ${e.javaClass.simpleName} - ${e.message ?: "Sin detalles"}"
                }

                updateState(OperationState.Error(errorMessage))
            }
        }
    }


    /**
     * Actualiza el estado de la operación en el thread principal
     */
    private suspend fun updateState(state: OperationState) {
        withContext(Dispatchers.Main) {
            _operationState.value = state
        }
    }

    /**
     * Estados posibles de una operación de respaldo/restauración
     */
    sealed class OperationState {
        object Idle : OperationState()
        data class InProgress(val message: String) : OperationState()
        data class Success(val message: String, val filePath: String? = null) : OperationState()
        data class Error(val message: String) : OperationState()
    }

    /**
     * Factory para crear instancias del ViewModel con las dependencias necesarias
     */
    class BackupRestoreViewModelFactory(
        private val database: AppDatabase,
        private val fotoRepository: FotoRepository,
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BackupRestoreViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BackupRestoreViewModel(database, fotoRepository, application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}