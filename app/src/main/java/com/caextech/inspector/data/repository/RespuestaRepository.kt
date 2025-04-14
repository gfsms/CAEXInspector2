package com.caextech.inspector.data.repository

import com.caextech.inspector.data.dao.RespuestaDao
import com.caextech.inspector.data.entities.Respuesta
import com.caextech.inspector.data.relations.RespuestaConDetalles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio para operaciones relacionadas con las respuestas de inspección.
 *
 * Esta clase proporciona métodos para acceder y manipular datos de respuestas,
 * sirviendo como una capa de abstracción entre la base de datos y la UI.
 */
class RespuestaRepository(private val respuestaDao: RespuestaDao) {

    /**
     * Obtiene todas las respuestas para una inspección específica.
     *
     * @param inspeccionId ID de la inspección
     * @return Flow de lista de respuestas
     */
    fun getRespuestasByInspeccion(inspeccionId: Long): Flow<List<Respuesta>> {
        return respuestaDao.getRespuestasByInspeccion(inspeccionId)
    }

    /**
     * Obtiene todas las respuestas con sus detalles para una inspección específica.
     *
     * @param inspeccionId ID de la inspección
     * @return Flow de lista de respuestas con detalles
     */
    fun getRespuestasConDetallesByInspeccion(inspeccionId: Long): Flow<List<RespuestaConDetalles>> {
        return respuestaDao.getRespuestasConDetallesByInspeccion(inspeccionId)
    }

    /**
     * Obtiene todas las respuestas con sus detalles para una inspección, ordenadas por categoría y orden.
     *
     * @param inspeccionId ID de la inspección
     * @return Flow de lista de respuestas con detalles ordenadas
     */
    fun getRespuestasConDetallesByInspeccionOrdenadas(inspeccionId: Long): Flow<List<RespuestaConDetalles>> {
        return respuestaDao.getRespuestasConDetallesByInspeccionOrdenadas(inspeccionId)
    }

    /**
     * Obtiene las respuestas con sus detalles filtradas por estado.
     *
     * @param inspeccionId ID de la inspección
     * @param estado Estado de las respuestas a filtrar (CONFORME o NO_CONFORME)
     * @return Flow de lista de respuestas con detalles
     */
    fun getRespuestasConDetallesByInspeccionYEstado(
        inspeccionId: Long,
        estado: String
    ): Flow<List<RespuestaConDetalles>> {
        // Use the map operator to transform the Flow results
        return respuestaDao.getRespuestasConDetallesByInspeccionOrdenadas(inspeccionId)
            .map { respuestas ->
                // This filtering happens inside the map transformation
                respuestas.filter { it.respuesta.estado == estado }
            }
    }

    /**
     * Obtiene las respuestas con sus detalles para una categoría específica de una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @param categoriaId ID de la categoría
     * @return Flow de lista de respuestas con detalles
     */
    fun getRespuestasConDetallesByInspeccionYCategoria(
        inspeccionId: Long,
        categoriaId: Long
    ): Flow<List<RespuestaConDetalles>> {
        return respuestaDao.getRespuestasConDetallesByInspeccionYCategoria(inspeccionId, categoriaId)
    }

    /**
     * Obtiene una respuesta específica para una inspección y pregunta.
     *
     * @param inspeccionId ID de la inspección
     * @param preguntaId ID de la pregunta
     * @return La respuesta o null si no existe
     */
    suspend fun getRespuestaPorInspeccionYPregunta(inspeccionId: Long, preguntaId: Long): Respuesta? {
        return respuestaDao.getRespuestaPorInspeccionYPregunta(inspeccionId, preguntaId)
    }

    /**
     * Cuenta el número de fotos para una respuesta.
     *
     * @param respuestaId ID de la respuesta
     * @return El número de fotos
     */
    suspend fun countFotosByRespuesta(respuestaId: Long): Int {
        // This would typically use the FotoDao, but for now we'll return 0
        return 0
    }

    /**
     * Guarda una respuesta "Conforme" para una pregunta en una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @param preguntaId ID de la pregunta
     * @return ID de la respuesta guardada
     */
    suspend fun guardarRespuestaConforme(inspeccionId: Long, preguntaId: Long): Long {
        // Verificar si ya existe una respuesta para esta pregunta en esta inspección
        val respuestaExistente = respuestaDao.getRespuestaPorInspeccionYPregunta(inspeccionId, preguntaId)

        if (respuestaExistente != null) {
            // Actualizar la respuesta existente
            val respuestaActualizada = respuestaExistente.copy(
                estado = Respuesta.ESTADO_CONFORME,
                comentarios = "",
                tipoAccion = null,
                idAvisoOrdenTrabajo = null,
                fechaModificacion = System.currentTimeMillis()
            )
            respuestaDao.updateRespuesta(respuestaActualizada)
            return respuestaExistente.respuestaId
        } else {
            // Crear una nueva respuesta
            val nuevaRespuesta = Respuesta(
                inspeccionId = inspeccionId,
                preguntaId = preguntaId,
                estado = Respuesta.ESTADO_CONFORME
            )
            return respuestaDao.insertRespuesta(nuevaRespuesta)
        }
    }

    /**
     * Guarda una respuesta "No Conforme" simplificada para una pregunta en una inspección.
     * Solo requiere los comentarios, el resto de detalles se completarán en la pantalla de resumen.
     *
     * @param inspeccionId ID de la inspección
     * @param preguntaId ID de la pregunta
     * @param comentarios Comentarios explicativos sobre el problema
     * @return ID de la respuesta creada
     */
    suspend fun guardarRespuestaNoConformeSimplificada(
        inspeccionId: Long,
        preguntaId: Long,
        comentarios: String
    ): Long {
        // Verificar que los comentarios no estén vacíos
        if (comentarios.isBlank()) {
            throw IllegalArgumentException("Los comentarios son obligatorios para una respuesta No Conforme")
        }

        // Verificar si ya existe una respuesta para esta pregunta en esta inspección
        val respuestaExistente = respuestaDao.getRespuestaPorInspeccionYPregunta(inspeccionId, preguntaId)

        if (respuestaExistente != null) {
            // Actualizar la respuesta existente
            val respuestaActualizada = respuestaExistente.copy(
                estado = Respuesta.ESTADO_NO_CONFORME,
                comentarios = comentarios,  // Guardar los comentarios
                tipoAccion = respuestaExistente.tipoAccion,  // Mantener el valor existente
                idAvisoOrdenTrabajo = respuestaExistente.idAvisoOrdenTrabajo,  // Mantener el valor existente
                fechaModificacion = System.currentTimeMillis()
            )
            respuestaDao.updateRespuesta(respuestaActualizada)
            return respuestaExistente.respuestaId
        } else {
            // Crear una nueva respuesta
            val nuevaRespuesta = Respuesta(
                inspeccionId = inspeccionId,
                preguntaId = preguntaId,
                estado = Respuesta.ESTADO_NO_CONFORME,
                comentarios = comentarios  // Guardar los comentarios
            )
            return respuestaDao.insertRespuesta(nuevaRespuesta)
        }
    }

    /**
     * Guarda una respuesta "No Conforme" para una pregunta en una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @param preguntaId ID de la pregunta
     * @param comentarios Comentarios explicativos sobre el problema
     * @param tipoAccion Tipo de acción (INMEDIATO o PROGRAMADO)
     * @param idAvisoOrdenTrabajo ID del aviso o la orden de trabajo asociada
     * @return ID de la respuesta creada
     */
    suspend fun guardarRespuestaNoConforme(
        inspeccionId: Long,
        preguntaId: Long,
        comentarios: String,
        tipoAccion: String,
        idAvisoOrdenTrabajo: String
    ): Long {
        // Validar campos obligatorios
        if (comentarios.isBlank()) {
            throw IllegalArgumentException("Los comentarios son obligatorios para una respuesta No Conforme")
        }

        if (tipoAccion != Respuesta.ACCION_INMEDIATO && tipoAccion != Respuesta.ACCION_PROGRAMADO) {
            throw IllegalArgumentException("El tipo de acción debe ser INMEDIATO o PROGRAMADO")
        }

        if (idAvisoOrdenTrabajo.isBlank()) {
            throw IllegalArgumentException("El ID de aviso u orden de trabajo es obligatorio")
        }

        // Verificar si ya existe una respuesta para esta pregunta en esta inspección
        val respuestaExistente = respuestaDao.getRespuestaPorInspeccionYPregunta(inspeccionId, preguntaId)

        if (respuestaExistente != null) {
            // Actualizar la respuesta existente
            val respuestaActualizada = respuestaExistente.copy(
                estado = Respuesta.ESTADO_NO_CONFORME,
                comentarios = comentarios,
                tipoAccion = tipoAccion,
                idAvisoOrdenTrabajo = idAvisoOrdenTrabajo,
                fechaModificacion = System.currentTimeMillis()
            )
            respuestaDao.updateRespuesta(respuestaActualizada)
            return respuestaExistente.respuestaId
        } else {
            // Crear una nueva respuesta
            val nuevaRespuesta = Respuesta(
                inspeccionId = inspeccionId,
                preguntaId = preguntaId,
                estado = Respuesta.ESTADO_NO_CONFORME,
                comentarios = comentarios,
                tipoAccion = tipoAccion,
                idAvisoOrdenTrabajo = idAvisoOrdenTrabajo
            )
            return respuestaDao.insertRespuesta(nuevaRespuesta)
        }
    }

    /**
     * Actualiza una respuesta No Conforme con los detalles completos.
     *
     * @param respuestaId ID de la respuesta
     * @param comentarios Comentarios sobre el problema
     * @param tipoAccion Tipo de acción (INMEDIATO o PROGRAMADO)
     * @param idAvisoOrdenTrabajo ID del aviso o la orden de trabajo asociada
     * @return true si la actualización fue exitosa, false en caso contrario
     */
    suspend fun actualizarRespuestaNoConforme(
        respuestaId: Long,
        comentarios: String,
        tipoAccion: String,
        idAvisoOrdenTrabajo: String
    ): Boolean {
        // Validar campos obligatorios
        if (comentarios.isBlank()) {
            throw IllegalArgumentException("Los comentarios son obligatorios para una respuesta No Conforme")
        }

        if (tipoAccion != Respuesta.ACCION_INMEDIATO && tipoAccion != Respuesta.ACCION_PROGRAMADO) {
            throw IllegalArgumentException("El tipo de acción debe ser INMEDIATO o PROGRAMADO")
        }

        if (idAvisoOrdenTrabajo.isBlank()) {
            throw IllegalArgumentException("El ID de aviso u orden de trabajo es obligatorio")
        }

        // Obtener la respuesta actual
        val respuestaActual = respuestaDao.getRespuestaById(respuestaId)
            ?: throw IllegalArgumentException("La respuesta con ID $respuestaId no existe")

        // Verificar que sea una respuesta No Conforme
        if (respuestaActual.estado != Respuesta.ESTADO_NO_CONFORME) {
            throw IllegalArgumentException("Solo se pueden actualizar respuestas No Conformes")
        }

        // Actualizar la respuesta
        val respuestaActualizada = respuestaActual.copy(
            comentarios = comentarios,
            tipoAccion = tipoAccion,
            idAvisoOrdenTrabajo = idAvisoOrdenTrabajo,
            fechaModificacion = System.currentTimeMillis()
        )

        respuestaDao.updateRespuesta(respuestaActualizada)
        return true
    }

    /**
     * Obtiene una respuesta específica por su ID.
     *
     * @param respuestaId ID de la respuesta
     * @return La respuesta o null si no existe
     */
    suspend fun getRespuestaById(respuestaId: Long): Respuesta? {
        return respuestaDao.getRespuestaById(respuestaId)
    }

    /**
     * Obtiene una respuesta con todos sus detalles por su ID.
     *
     * @param respuestaId ID de la respuesta
     * @return La respuesta con detalles o null si no existe
     */
    suspend fun getRespuestaConDetallesById(respuestaId: Long): RespuestaConDetalles? {
        return respuestaDao.getRespuestaConDetallesById(respuestaId)
    }

    /**
     * Elimina todas las respuestas asociadas a una inspección.
     *
     * @param inspeccionId ID de la inspección
     */
    suspend fun deleteRespuestasByInspeccion(inspeccionId: Long) {
        respuestaDao.deleteRespuestasByInspeccion(inspeccionId)
    }

    /**
     * Cuenta el número de respuestas para una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @return Número de respuestas
     */
    suspend fun countRespuestasByInspeccion(inspeccionId: Long): Int {
        return respuestaDao.countRespuestasByInspeccion(inspeccionId)
    }

    /**
     * Cuenta el número de respuestas con un estado específico para una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @param estado Estado de las respuestas (CONFORME o NO_CONFORME)
     * @return Número de respuestas con el estado especificado
     */
    suspend fun countRespuestasByInspeccionYEstado(inspeccionId: Long, estado: String): Int {
        return respuestaDao.countRespuestasByInspeccionYEstado(inspeccionId, estado)
    }
}