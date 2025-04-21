package com.caextech.inspector.data.repository

import com.caextech.inspector.data.dao.CAEXDao
import com.caextech.inspector.data.dao.InspeccionDao
import com.caextech.inspector.data.dao.PreguntaDao
import com.caextech.inspector.data.dao.RespuestaDao
import com.caextech.inspector.data.entities.Inspeccion
import com.caextech.inspector.data.relations.InspeccionCompleta
import com.caextech.inspector.data.relations.InspeccionConCAEX
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

/**
 * Repositorio para operaciones relacionadas con las inspecciones.
 *
 * Esta clase proporciona métodos para acceder y manipular datos de inspecciones,
 * sirviendo como una capa de abstracción entre la base de datos y la UI.
 */
class InspeccionRepository(
    private val inspeccionDao: InspeccionDao,
    private val respuestaDao: RespuestaDao,
    private val preguntaDao: PreguntaDao,
    private val caexDao: CAEXDao
) {

    // Obtener todas las inspecciones con CAEX como Flow
    val allInspeccionesConCAEX: Flow<List<InspeccionConCAEX>> = inspeccionDao.getAllInspeccionesConCAEX()

    // Obtener inspecciones abiertas con CAEX como Flow
    val inspeccionesAbiertasConCAEX: Flow<List<InspeccionConCAEX>> =
        inspeccionDao.getInspeccionesConCAEXByEstados(
            listOf(Inspeccion.ESTADO_ABIERTA, Inspeccion.ESTADO_PENDIENTE_CIERRE)
        )
    // Nueva propiedad específicamente para inspecciones pendientes de cierre
    val inspeccionesPendienteCierreConCAEX: Flow<List<InspeccionConCAEX>> =
        inspeccionDao.getInspeccionesConCAEXByEstado(Inspeccion.ESTADO_PENDIENTE_CIERRE)

    // Obtener inspecciones cerradas con CAEX como Flow
    val inspeccionesCerradasConCAEX: Flow<List<InspeccionConCAEX>> =
        inspeccionDao.getInspeccionesConCAEXByEstado(Inspeccion.ESTADO_CERRADA)

    // Obtener inspecciones de recepción abiertas con CAEX como Flow
    val inspeccionesRecepcionAbiertasConCAEX: Flow<List<InspeccionConCAEX>> =
        inspeccionDao.getInspeccionesConCAEXByTipoYEstado(
            Inspeccion.TIPO_RECEPCION,
            Inspeccion.ESTADO_ABIERTA
        )

    // Crear una nueva inspección de recepción
    suspend fun crearInspeccionRecepcion(
        caexId: Long,
        nombreInspector: String,
        nombreSupervisor: String
    ): Long {
        // Verificar que el CAEX existe
        val caex = caexDao.getCAEXById(caexId)
            ?: throw IllegalArgumentException("El CAEX con ID $caexId no existe")

        // Crear la inspección
        val inspeccion = Inspeccion(
            caexId = caexId,
            tipo = Inspeccion.TIPO_RECEPCION,
            estado = Inspeccion.ESTADO_ABIERTA,
            nombreInspector = nombreInspector,
            nombreSupervisor = nombreSupervisor
        )

        return inspeccionDao.insertInspeccion(inspeccion)
    }
    /**
     * Crea una inspección de entrega basada en una inspección de recepción existente.
     *
     * @param inspeccionRecepcionId ID de la inspección de recepción
     * @param nombreInspector Nombre del inspector que realiza la entrega
     * @param nombreSupervisor Nombre del supervisor de taller
     * @return ID de la inspección de entrega creada
     * @throws IllegalArgumentException si la inspección de recepción no existe o no está en estado PENDIENTE_CIERRE
     */
    suspend fun crearInspeccionEntregaDesdeRecepcion(
        inspeccionRecepcionId: Long,
        nombreInspector: String,
        nombreSupervisor: String
    ): Long {
        // Verificar que la inspección de recepción existe
        val inspeccionRecepcion = inspeccionDao.getInspeccionById(inspeccionRecepcionId)
            ?: throw IllegalArgumentException("La inspección de recepción con ID $inspeccionRecepcionId no existe")

        // Verificar que la inspección de recepción es de tipo RECEPCION
        if (inspeccionRecepcion.tipo != Inspeccion.TIPO_RECEPCION) {
            throw IllegalArgumentException("La inspección con ID $inspeccionRecepcionId no es de tipo RECEPCION")
        }

        // Verificar que la inspección de recepción está en estado PENDIENTE_CIERRE
        if (inspeccionRecepcion.estado != Inspeccion.ESTADO_PENDIENTE_CIERRE) {
            throw IllegalArgumentException("La inspección de recepción debe estar en estado PENDIENTE_CIERRE")
        }

        // Crear la inspección de entrega
        val inspeccion = Inspeccion(
            caexId = inspeccionRecepcion.caexId,
            tipo = Inspeccion.TIPO_ENTREGA,
            estado = Inspeccion.ESTADO_ABIERTA,
            nombreInspector = nombreInspector,
            nombreSupervisor = nombreSupervisor,
            inspeccionRecepcionId = inspeccionRecepcionId,
            comentariosGenerales = ""
        )

        return inspeccionDao.insertInspeccion(inspeccion)
    }
    // Crear una nueva inspección de entrega basada en una inspección de recepción
    suspend fun crearInspeccionEntrega(
        inspeccionRecepcionId: Long,
        nombreInspector: String,
        nombreSupervisor: String
    ): Long {
        // Verificar que la inspección de recepción existe
        val inspeccionRecepcion = inspeccionDao.getInspeccionById(inspeccionRecepcionId)
            ?: throw IllegalArgumentException("La inspección de recepción con ID $inspeccionRecepcionId no existe")

        // Verificar que la inspección de recepción es de tipo RECEPCION
        if (inspeccionRecepcion.tipo != Inspeccion.TIPO_RECEPCION) {
            throw IllegalArgumentException("La inspección con ID $inspeccionRecepcionId no es de tipo RECEPCION")
        }

        // Crear la inspección de entrega
        val inspeccion = Inspeccion(
            caexId = inspeccionRecepcion.caexId,
            tipo = Inspeccion.TIPO_ENTREGA,
            estado = Inspeccion.ESTADO_ABIERTA,
            nombreInspector = nombreInspector,
            nombreSupervisor = nombreSupervisor,
            inspeccionRecepcionId = inspeccionRecepcionId
        )

        return inspeccionDao.insertInspeccion(inspeccion)
    }
    /**
     * Obtiene inspecciones con CAEX que coincidan con cualquiera de los estados especificados.
     *
     * @param estados Lista de estados de inspección
     * @return Flow con la lista de inspecciones con CAEX
     */
    fun getInspeccionesConCAEXByEstados(estados: List<String>): Flow<List<InspeccionConCAEX>> {
        return inspeccionDao.getInspeccionesConCAEXByEstados(estados)
    }

    /**
     * Cierra una inspección.
     * Para inspecciones de tipo ENTREGA, también cierra la inspección de recepción asociada.
     *
     * @param inspeccionId ID de la inspección
     * @param comentariosGenerales Comentarios generales sobre la inspección
     * @return true si la operación fue exitosa, false en caso contrario
     */
    suspend fun cerrarInspeccion(inspeccionId: Long, comentariosGenerales: String = ""): Boolean {
        // Verificar que la inspección existe
        val inspeccion = inspeccionDao.getInspeccionById(inspeccionId)
            ?: throw IllegalArgumentException("La inspección con ID $inspeccionId no existe")

        // Verificar que la inspección está abierta
        if (inspeccion.estado != Inspeccion.ESTADO_ABIERTA) {
            throw IllegalArgumentException("La inspección con ID $inspeccionId ya está cerrada o pendiente de cierre")
        }

        // Obtener el modelo del CAEX para saber cuántas preguntas debe tener
        val caex = caexDao.getCAEXById(inspeccion.caexId)
            ?: throw IllegalArgumentException("No se encontró el CAEX asociado a la inspección")

        // Contar cuántas preguntas hay para este modelo
        val totalPreguntas = preguntaDao.countPreguntasByModelo(caex.modelo)

        // Contar cuántas respuestas tiene la inspección
        val totalRespuestas = respuestaDao.countRespuestasByInspeccion(inspeccionId)

        // Verificar que todas las preguntas tienen respuesta
        if (totalRespuestas < totalPreguntas) {
            return false
        }

        // Determinar el nuevo estado según el tipo de inspección
        val nuevoEstado = when (inspeccion.tipo) {
            Inspeccion.TIPO_RECEPCION -> Inspeccion.ESTADO_PENDIENTE_CIERRE
            Inspeccion.TIPO_ENTREGA -> Inspeccion.ESTADO_CERRADA
            else -> Inspeccion.ESTADO_CERRADA // Por defecto, cerrar
        }

        // Actualizar la inspección
        val inspeccionActualizada = inspeccion.copy(
            estado = nuevoEstado,
            fechaFinalizacion = System.currentTimeMillis(),
            comentariosGenerales = comentariosGenerales
        )

        inspeccionDao.updateInspeccion(inspeccionActualizada)

        // Si es una inspección de entrega, cerrar también la inspección de recepción asociada
        if (inspeccion.tipo == Inspeccion.TIPO_ENTREGA && inspeccion.inspeccionRecepcionId != null) {
            val inspeccionRecepcion = inspeccionDao.getInspeccionById(inspeccion.inspeccionRecepcionId)
            if (inspeccionRecepcion != null && inspeccionRecepcion.estado == Inspeccion.ESTADO_PENDIENTE_CIERRE) {
                val inspeccionRecepcionActualizada = inspeccionRecepcion.copy(
                    estado = Inspeccion.ESTADO_CERRADA,
                    fechaFinalizacion = System.currentTimeMillis()
                )
                inspeccionDao.updateInspeccion(inspeccionRecepcionActualizada)
            }
        }

        return true
    }

    /**
     * Verifica si una inspección de recepción tiene una inspección de entrega asociada
     *
     * @param recepcionId ID de la inspección de recepción
     * @return true si tiene una inspección de entrega, false en caso contrario
     */
    suspend fun tieneInspeccionEntregaAsociada(recepcionId: Long): Boolean {
        val count = inspeccionDao.countInspeccionesByInspeccionRecepcionId(recepcionId)
        return count > 0
    }

    /**
     * Obtiene la inspección de entrega asociada a una inspección de recepción
     *
     * @param recepcionId ID de la inspección de recepción
     * @return La inspección de entrega o null si no existe
     */
    suspend fun getInspeccionEntregaByRecepcion(recepcionId: Long): Inspeccion? {
        return inspeccionDao.getInspeccionByInspeccionRecepcionId(recepcionId)
    }
    // Obtener inspecciones por CAEX como Flow
    fun getInspeccionesByCAEX(caexId: Long): Flow<List<InspeccionConCAEX>> {
        return inspeccionDao.getInspeccionesConCAEXByEstado(Inspeccion.ESTADO_ABIERTA)
            .map { inspecciones -> inspecciones.filter { it.inspeccion.caexId == caexId } }
    }
    /**
     * Obtiene inspecciones con CAEX para un estado específico.
     *
     * @param estado Estado de las inspecciones a buscar
     * @return Flow con la lista de inspecciones con CAEX
     */
    fun getInspeccionesConCAEXByEstado(estado: String): Flow<List<InspeccionConCAEX>> {
        return inspeccionDao.getInspeccionesConCAEXByEstado(estado)
    }
    // Obtener inspecciones por modelo de CAEX, estado y tipo
    fun getInspeccionesByModeloEstadoYTipo(
        modeloCAEX: String,
        estado: String,
        tipo: String
    ): Flow<List<InspeccionConCAEX>> {
        return inspeccionDao.getInspeccionesConCAEXByModeloEstadoYTipo(modeloCAEX, estado, tipo)
    }
    /**
     * Gets inspections with CAEX by reception inspection ID.
     */
    fun getInspeccionesConCAEXByInspeccionRecepcionId(inspeccionRecepcionId: Long): Flow<List<InspeccionConCAEX>> {
        return inspeccionDao.getInspeccionesConCAEXByInspeccionRecepcionId(inspeccionRecepcionId)
    }
    // Obtener una inspección completa por ID
    suspend fun getInspeccionCompletaById(inspeccionId: Long): InspeccionCompleta? {
        return inspeccionDao.getInspeccionCompletaById(inspeccionId)
    }

    // Obtener una inspección con CAEX por ID
    suspend fun getInspeccionConCAEXById(inspeccionId: Long): InspeccionConCAEX? {
        return inspeccionDao.getInspeccionConCAEXById(inspeccionId)
    }

    // Eliminar una inspección
    suspend fun delete(inspeccion: Inspeccion) {
        inspeccionDao.deleteInspeccion(inspeccion)
    }
}