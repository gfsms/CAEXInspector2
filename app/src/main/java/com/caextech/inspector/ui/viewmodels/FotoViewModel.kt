package com.caextech.inspector.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.caextech.inspector.data.entities.Foto
import com.caextech.inspector.data.repository.FotoRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de fotos de evidencia.
 *
 * Esta clase expone datos observables y proporciona métodos para interactuar
 * con las fotos de manera coherente con el ciclo de vida de la UI.
 */
class FotoViewModel(private val repository: FotoRepository) : ViewModel() {

    // LiveData para notificar eventos de operaciones
    private val _operationStatus = MutableLiveData<OperationStatus>()
    val operationStatus: LiveData<OperationStatus> = _operationStatus

    // LiveData para almacenar la URI temporal para la cámara
    private val _tempPhotoUri = MutableLiveData<Uri>()
    val tempPhotoUri: LiveData<Uri> = _tempPhotoUri

    // LiveData para almacenar la ruta del archivo temporal
    private val _tempPhotoPath = MutableLiveData<String>()
    val tempPhotoPath: LiveData<String> = _tempPhotoPath

    /**
     * Obtiene todas las fotos para una respuesta específica.
     *
     * @param respuestaId ID de la respuesta
     * @return LiveData con la lista de fotos
     */
    fun getFotosByRespuesta(respuestaId: Long): LiveData<List<Foto>> {
        return repository.getFotosByRespuesta(respuestaId).asLiveData()
    }

    /**
     * Obtiene todas las fotos para una inspección específica.
     *
     * @param inspeccionId ID de la inspección
     * @return LiveData con la lista de fotos
     */
    fun getFotosByInspeccion(inspeccionId: Long): LiveData<List<Foto>> {
        return repository.getFotosByInspeccion(inspeccionId).asLiveData()
    }

    /**
     * Crea un archivo temporal para una nueva foto de la cámara.
     */
    fun prepararArchivoTemporalParaFoto() {
        try {
            val (uri, path) = repository.crearArchivoTemporalParaFoto()
            _tempPhotoUri.value = uri
            _tempPhotoPath.value = path
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(
                "Error al crear archivo temporal: ${e.message ?: "desconocido"}"
            )
        }
    }

    /**
     * Guarda una foto tomada con la cámara para una respuesta específica.
     *
     * @param respuestaId ID de la respuesta a la que pertenece esta foto
     * @param descripcion Descripción opcional de la foto
     */
    fun guardarFotoTomada(respuestaId: Long, descripcion: String = "") = viewModelScope.launch {
        try {
            // Verificar que tenemos una ruta de foto temporal
            val tempPath = _tempPhotoPath.value
                ?: throw IllegalStateException("No hay foto temporal para guardar")

            // Guardar la foto en la base de datos
            val fotoId = repository.guardarFoto(respuestaId, tempPath, descripcion)

            // Limpiar variables temporales
            _tempPhotoUri.value = null
            _tempPhotoPath.value = null

            _operationStatus.value = OperationStatus.Success(
                "Foto guardada correctamente",
                fotoId
            )
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(
                "Error al guardar foto: ${e.message ?: "desconocido"}"
            )
        }
    }

    /**
     * Guarda una foto seleccionada de la galería para una respuesta específica.
     *
     * @param respuestaId ID de la respuesta a la que pertenece esta foto
     * @param uri URI de la imagen seleccionada
     * @param descripcion Descripción opcional de la foto
     */
    fun guardarFotoSeleccionada(
        respuestaId: Long,
        uri: Uri,
        descripcion: String = ""
    ) = viewModelScope.launch {
        try {
            // Copiar la imagen desde la URI temporal a un archivo permanente
            val rutaArchivo = repository.copiarImagenDesdeTemporal(uri)
                ?: throw IllegalStateException("Error al copiar la imagen")

            // Guardar la foto en la base de datos
            val fotoId = repository.guardarFoto(respuestaId, rutaArchivo, descripcion)

            _operationStatus.value = OperationStatus.Success(
                "Foto guardada correctamente",
                fotoId
            )
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(
                "Error al guardar foto: ${e.message ?: "desconocido"}"
            )
        }
    }

    /**
     * Elimina una foto.
     *
     * @param foto La foto a eliminar
     */
    fun deleteFoto(foto: Foto) = viewModelScope.launch {
        try {
            repository.deleteFoto(foto)
            _operationStatus.value = OperationStatus.Success("Foto eliminada correctamente")
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(
                "Error al eliminar foto: ${e.message ?: "desconocido"}"
            )
        }
    }

    /**
     * Cuenta el número de fotos para una respuesta.
     *
     * @param respuestaId ID de la respuesta
     * @return El número de fotos (en un LiveData)
     */
    fun countFotosByRespuesta(respuestaId: Long): LiveData<Int> {
        val result = MutableLiveData<Int>()
        viewModelScope.launch {
            try {
                val count = repository.countFotosByRespuesta(respuestaId)
                result.value = count
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
                result.value = 0
            }
        }
        return result
    }

    /**
     * Clase sellada para representar el estado de las operaciones.
     */
    sealed class OperationStatus {
        data class Success(val message: String, val id: Long = 0) : OperationStatus()
        data class Error(val message: String) : OperationStatus()
    }

    /**
     * Factory para crear instancias de FotoViewModel con el repositorio correcto.
     */
    class FotoViewModelFactory(private val repository: FotoRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FotoViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FotoViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}