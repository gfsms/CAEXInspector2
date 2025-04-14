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
        inspeccionDao.getInspeccionesConCAEXByEstado(Inspeccion.ESTADO_ABIERTA)

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

    // Cerrar una inspección
    suspend fun cerrarInspeccion(inspeccionId: Long, comentariosGenerales: String = ""): Boolean {
        // Verificar que la inspección existe
        val inspeccion = inspeccionDao.getInspeccionById(inspeccionId)
            ?: throw IllegalArgumentException("La inspección con ID $inspeccionId no existe")

        // Verificar que la inspección está abierta
        if (inspeccion.estado != Inspeccion.ESTADO_ABIERTA) {
            throw IllegalArgumentException("La inspección con ID $inspeccionId ya está cerrada")
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

        // Actualizar la inspección
        val inspeccionActualizada = inspeccion.copy(
            estado = Inspeccion.ESTADO_CERRADA,
            fechaFinalizacion = System.currentTimeMillis(),
            comentariosGenerales = comentariosGenerales
        )

        inspeccionDao.updateInspeccion(inspeccionActualizada)
        return true
    }

    // Obtener inspecciones por CAEX como Flow
    fun getInspeccionesByCAEX(caexId: Long): Flow<List<InspeccionConCAEX>> {
        return inspeccionDao.getInspeccionesConCAEXByEstado(Inspeccion.ESTADO_ABIERTA)
            .map { inspecciones -> inspecciones.filter { it.inspeccion.caexId == caexId } }
    }

    // Obtener inspecciones por modelo de CAEX, estado y tipo
    fun getInspeccionesByModeloEstadoYTipo(
        modeloCAEX: String,
        estado: String,
        tipo: String
    ): Flow<List<InspeccionConCAEX>> {
        return inspeccionDao.getInspeccionesConCAEXByModeloEstadoYTipo(modeloCAEX, estado, tipo)
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