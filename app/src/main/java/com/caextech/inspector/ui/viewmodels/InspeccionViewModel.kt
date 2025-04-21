package com.caextech.inspector.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.caextech.inspector.data.entities.CAEX
import com.caextech.inspector.data.entities.Inspeccion
import com.caextech.inspector.data.relations.InspeccionCompleta
import com.caextech.inspector.data.relations.InspeccionConCAEX
import com.caextech.inspector.data.repository.CAEXRepository
import com.caextech.inspector.data.repository.InspeccionRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de inspecciones.
 *
 * Esta clase expone datos observables y proporciona métodos para interactuar
 * con los datos de inspecciones de manera coherente con el ciclo de vida de la UI.
 */
class InspeccionViewModel(
    private val inspeccionRepository: InspeccionRepository,
    private val caexRepository: CAEXRepository? = null
) : ViewModel() {

    // LiveData para notificar eventos de operaciones
    private val _operationStatus = MutableLiveData<OperationStatus>()
    val operationStatus: LiveData<OperationStatus> = _operationStatus

    // LiveData con todas las inspecciones
    val allInspeccionesConCAEX: LiveData<List<InspeccionConCAEX>> =
        inspeccionRepository.allInspeccionesConCAEX.asLiveData()

    // LiveData con inspecciones abiertas
    val inspeccionesAbiertasConCAEX: LiveData<List<InspeccionConCAEX>>
        get() = inspeccionRepository.getInspeccionesConCAEXByEstados(
            listOf(Inspeccion.ESTADO_ABIERTA, Inspeccion.ESTADO_PENDIENTE_CIERRE)
        ).asLiveData()

    // LiveData con inspecciones cerradas
    val inspeccionesCerradasConCAEX: LiveData<List<InspeccionConCAEX>> =
        inspeccionRepository.inspeccionesCerradasConCAEX.asLiveData()

    // LiveData con inspecciones de recepción abiertas
    val inspeccionesRecepcionAbiertasConCAEX: LiveData<List<InspeccionConCAEX>> =
        inspeccionRepository.inspeccionesRecepcionAbiertasConCAEX.asLiveData()

    val inspeccionesPendienteCierreConCAEX: LiveData<List<InspeccionConCAEX>>
        get() = inspeccionRepository.getInspeccionesConCAEXByEstado(Inspeccion.ESTADO_PENDIENTE_CIERRE).asLiveData()

    /**
     * Busca un CAEX por su número identificador y modelo, lo crea si no existe,
     * y luego crea una inspección de recepción para ese CAEX.
     */
    fun buscarCAEXPorNumeroYCrearInspeccion(
        numeroIdentificador: Int,
        modelo: String,
        nombreInspector: String,
        nombreSupervisor: String
    ) = viewModelScope.launch {
        try {
            if (caexRepository == null) {
                _operationStatus.value = OperationStatus.Error("No se puede realizar esta operación sin el CAEXRepository")
                return@launch
            }

            // Buscar el CAEX por número identificador
            var caex = caexRepository.getCAEXByNumeroIdentificador(numeroIdentificador)

            // Si no existe, crearlo
            if (caex == null) {
                val nuevoCAEX = CAEX(
                    numeroIdentificador = numeroIdentificador,
                    modelo = modelo
                )

                // Validar que el identificador sea válido para el modelo
                if (!nuevoCAEX.esIdentificadorValido()) {
                    throw IllegalArgumentException("El número identificador $numeroIdentificador no es válido para el modelo $modelo")
                }

                // Insertar el nuevo CAEX y obtener su ID
                val caexId = caexRepository.insert(nuevoCAEX)
                caex = caexRepository.getCAEXById(caexId)
                    ?: throw IllegalStateException("Error al crear el CAEX")
            } else {
                // Verificar que el modelo coincida
                if (caex.modelo != modelo) {
                    throw IllegalArgumentException("El CAEX #$numeroIdentificador existe pero es de modelo ${caex.modelo}, no $modelo")
                }
            }

            // Crear la inspección de recepción
            val inspeccionId = inspeccionRepository.crearInspeccionRecepcion(
                caex.caexId,
                nombreInspector,
                nombreSupervisor
            )

            _operationStatus.value = OperationStatus.Success(
                "Inspección de recepción creada correctamente",
                inspeccionId
            )
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Crea una nueva inspección de recepción.
     *
     * @param caexId ID del CAEX a inspeccionar
     * @param nombreInspector Nombre del inspector
     * @param nombreSupervisor Nombre del supervisor de taller
     */
    fun crearInspeccionRecepcion(
        caexId: Long,
        nombreInspector: String,
        nombreSupervisor: String
    ) = viewModelScope.launch {
        try {
            val inspeccionId = inspeccionRepository.crearInspeccionRecepcion(
                caexId,
                nombreInspector,
                nombreSupervisor
            )
            _operationStatus.value = OperationStatus.Success(
                "Inspección de recepción creada correctamente",
                inspeccionId
            )
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Creates a delivery inspection linked to a reception inspection.
     */
    fun crearInspeccionEntrega(
        inspeccionRecepcionId: Long,
        nombreInspector: String,
        nombreSupervisor: String
    ) = viewModelScope.launch {
        try {
            // Log for debugging
            Log.d("DeliveryDebug", "Starting creation in ViewModel")

            // Create the inspection and get its ID
            val inspeccionId = inspeccionRepository.crearInspeccionEntregaDesdeRecepcion(
                inspeccionRecepcionId,
                nombreInspector,
                nombreSupervisor
            )

            // Important: Update the operation status with the new ID
            _operationStatus.postValue(OperationStatus.Success(
                "Inspección de entrega creada correctamente",
                inspeccionId
            ))

            Log.d("DeliveryDebug", "Delivery inspection created with ID: $inspeccionId")
        } catch (e: Exception) {
            Log.e("DeliveryDebug", "Error creating delivery inspection: ${e.message}", e)
            _operationStatus.postValue(OperationStatus.Error(e.message ?: "Error desconocido"))
        }
    }
    /**
     * Verifica si una inspección de recepción ya tiene una inspección de entrega asociada.
     *
     * @param recepcionId ID de la inspección de recepción
     * @return LiveData<Boolean> true si ya tiene una inspección de entrega, false en caso contrario
     */
    fun tieneInspeccionEntregaAsociada(recepcionId: Long): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            try {
                val tieneEntrega = inspeccionRepository.tieneInspeccionEntregaAsociada(recepcionId)
                result.value = tieneEntrega
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
                result.value = false
            }
        }
        return result
    }

    /**
     * Obtiene la inspección de entrega asociada a una inspección de recepción.
     *
     * @param recepcionId ID de la inspección de recepción
     * @return LiveData<Inspeccion?> La inspección de entrega o null si no existe
     */
    fun getInspeccionEntregaByRecepcion(recepcionId: Long): LiveData<Inspeccion?> {
        val result = MutableLiveData<Inspeccion?>()
        viewModelScope.launch {
            try {
                val inspeccionEntrega = inspeccionRepository.getInspeccionEntregaByRecepcion(recepcionId)
                result.value = inspeccionEntrega
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
                result.value = null
            }
        }
        return result
    }


    /**
     * Gets inspections with CAEX by reception inspection ID.
     */
    fun getInspeccionesConCAEXByInspeccionRecepcionId(inspeccionRecepcionId: Long): LiveData<List<InspeccionConCAEX>> {
        return inspeccionRepository.getInspeccionesConCAEXByInspeccionRecepcionId(inspeccionRecepcionId).asLiveData()
    }
    /**
     * Cierra una inspección.
     *
     * @param inspeccionId ID de la inspección a cerrar
     * @param comentariosGenerales Comentarios generales sobre la inspección
     */
    fun cerrarInspeccion(inspeccionId: Long, comentariosGenerales: String = ""): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            try {
                val success = inspeccionRepository.cerrarInspeccion(inspeccionId, comentariosGenerales)
                result.value = success
                if (success) {
                    _operationStatus.value = OperationStatus.Success(
                        "Inspección cerrada correctamente",
                        inspeccionId
                    )
                } else {
                    _operationStatus.value = OperationStatus.Error(
                        "No se puede cerrar la inspección. Asegúrate de responder todas las preguntas."
                    )
                }
            } catch (e: Exception) {
                result.value = false
                _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
            }
        }
        return result
    }

    /**
     * Obtiene las inspecciones para un modelo de CAEX, estado y tipo específicos.
     *
     * @param modeloCAEX Modelo del CAEX (797F o 798AC)
     * @param estado Estado de la inspección (ABIERTA o CERRADA)
     * @param tipo Tipo de inspección (RECEPCION o ENTREGA)
     * @return LiveData con la lista de inspecciones que cumplen los criterios
     */
    fun getInspeccionesByModeloEstadoYTipo(
        modeloCAEX: String,
        estado: String,
        tipo: String
    ): LiveData<List<InspeccionConCAEX>> {
        return inspeccionRepository.getInspeccionesByModeloEstadoYTipo(modeloCAEX, estado, tipo).asLiveData()
    }

    /**
     * Obtiene los detalles completos de una inspección.
     *
     * @param inspeccionId ID de la inspección
     */
    fun getInspeccionCompleta(inspeccionId: Long) = viewModelScope.launch {
        try {
            val inspeccion = inspeccionRepository.getInspeccionCompletaById(inspeccionId)
            if (inspeccion != null) {
                _inspeccionCompleta.value = inspeccion
            } else {
                _operationStatus.value = OperationStatus.Error("Inspección no encontrada")
            }
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
        }
    }

    // LiveData para almacenar la inspección completa actual
    private val _inspeccionCompleta = MutableLiveData<InspeccionCompleta>()
    val inspeccionCompleta: LiveData<InspeccionCompleta> = _inspeccionCompleta

    /**
     * Obtiene una inspección con CAEX por su ID.
     *
     * @param inspeccionId ID de la inspección
     * @return LiveData con la inspección o null si no existe
     */
    fun getInspeccionConCAEXById(inspeccionId: Long): LiveData<InspeccionConCAEX?> {
        val result = MutableLiveData<InspeccionConCAEX?>()
        viewModelScope.launch {
            try {
                val inspeccion = inspeccionRepository.getInspeccionConCAEXById(inspeccionId)
                result.value = inspeccion
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
                result.value = null
            }
        }
        return result
    }

    /**
     * Elimina una inspección.
     *
     * @param inspeccion La inspección a eliminar
     */
    fun deleteInspeccion(inspeccion: Inspeccion) = viewModelScope.launch {
        try {
            inspeccionRepository.delete(inspeccion)
            _operationStatus.value = OperationStatus.Success("Inspección eliminada correctamente")
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Clase sellada para representar el estado de las operaciones.
     */
    sealed class OperationStatus {
        data class Success(val message: String, val id: Long = 0) : OperationStatus()
        data class Error(val message: String) : OperationStatus()
    }

    /**
     * Factory para crear instancias de InspeccionViewModel con el repositorio correcto.
     */
    class InspeccionViewModelFactory(
        private val inspeccionRepository: InspeccionRepository,
        private val caexRepository: CAEXRepository? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InspeccionViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return InspeccionViewModel(inspeccionRepository, caexRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}