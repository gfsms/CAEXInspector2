package com.caextech.inspector.utils

import com.caextech.inspector.data.entities.Respuesta
import com.caextech.inspector.data.relations.RespuestaConDetalles

/**
 * Clase para rastrear todas las respuestas en memoria, independientemente de si están
 * guardadas en la base de datos o no.
 *
 * Esta clase resuelve el problema de respuestas que se pierden al navegar entre fragmentos
 * o al volver de actividades como la cámara.
 */
object RespuestaTracker {
    private const val TAG = "RespuestaTracker"

    // Mapa para rastrear el estado de todas las respuestas por inspecciónId y preguntaId
    private val respuestasEnMemoria = mutableMapOf<Pair<Long, Long>, String>()

    // Mapa para asociar respuestaId con el par inspeccionId-preguntaId
    private val respuestaIdMap = mutableMapOf<Long, Pair<Long, Long>>()

    /**
     * Asegura que el estado de la pregunta sea el deseado
     */
    fun ensureQuestionState(inspeccionId: Long, preguntaId: Long, deseadoEstado: String) {
        val key = Pair(inspeccionId, preguntaId)
        val estadoActual = respuestasEnMemoria[key]

        // Si el estado actual no coincide con el deseado, actualizarlo
        if (estadoActual != deseadoEstado) {
            respuestasEnMemoria[key] = deseadoEstado
            Logger.d(TAG, "Estado restaurado para inspección $inspeccionId, pregunta $preguntaId: $deseadoEstado")
        }
    }

    /**
     * Registra el mapeo de respuestaId a inspeccionId-preguntaId
     */
    fun registrarRespuestaId(respuestaId: Long, inspeccionId: Long, preguntaId: Long) {
        respuestaIdMap[respuestaId] = Pair(inspeccionId, preguntaId)
        Logger.d(TAG, "Mapeo registrado: respuestaId $respuestaId -> inspección $inspeccionId, pregunta $preguntaId")
    }

    /**
     * Asegura que una pregunta esté marcada como No Conforme o Rechazado después de capturar una foto
     */
    fun ensureNoConformeEstado(respuestaId: Long) {
        // Buscar el par inspeccionId-preguntaId para este respuestaId
        val idPair = respuestaIdMap[respuestaId]
        if (idPair != null) {
            val (inspeccionId, preguntaId) = idPair
            val estadoActual = respuestasEnMemoria[idPair]

            // Si el estado es No Conforme o Rechazado, mantenerlo
            if (estadoActual == Respuesta.ESTADO_NO_CONFORME || estadoActual == Respuesta.ESTADO_RECHAZADO) {
                // No hacer nada, mantener el estado
                Logger.d(TAG, "Preservando estado $estadoActual para respuestaId: $respuestaId")
            } else {
                // Si no tiene estado o es otro estado, determinar el apropiado basado en el respuestaId
                // Asumir que es No Conforme si no sabemos exactamente
                respuestasEnMemoria[idPair] = Respuesta.ESTADO_NO_CONFORME
                Logger.d(TAG, "Estableciendo estado NO_CONFORME para respuestaId: $respuestaId")
            }
        } else {
            Logger.w(TAG, "No se encontró mapeo para respuestaId: $respuestaId")
        }
    }

    /**
     * Registra una respuesta "Conforme" para una pregunta específica en una inspección
     */
    fun registrarRespuestaConforme(inspeccionId: Long, preguntaId: Long) {
        val key = Pair(inspeccionId, preguntaId)
        respuestasEnMemoria[key] = Respuesta.ESTADO_CONFORME
        Logger.d(TAG, "Registrada respuesta CONFORME para inspección $inspeccionId, pregunta $preguntaId")
    }

    /**
     * Registra una respuesta "No Conforme" para una pregunta específica en una inspección
     */
    fun registrarRespuestaNoConforme(inspeccionId: Long, preguntaId: Long) {
        val key = Pair(inspeccionId, preguntaId)
        respuestasEnMemoria[key] = Respuesta.ESTADO_NO_CONFORME
        Logger.d(TAG, "Registrada respuesta NO_CONFORME para inspección $inspeccionId, pregunta $preguntaId")
    }

    /**
     * Registra una respuesta "Aceptado" para una pregunta específica en una inspección
     */
    fun registrarRespuestaAceptada(inspeccionId: Long, preguntaId: Long) {
        val key = Pair(inspeccionId, preguntaId)
        respuestasEnMemoria[key] = Respuesta.ESTADO_ACEPTADO
        Logger.d(TAG, "Registrada respuesta ACEPTADO para inspección $inspeccionId, pregunta $preguntaId")
    }

    /**
     * Registra una respuesta "Rechazado" para una pregunta específica en una inspección
     */
    fun registrarRespuestaRechazada(inspeccionId: Long, preguntaId: Long) {
        val key = Pair(inspeccionId, preguntaId)
        respuestasEnMemoria[key] = Respuesta.ESTADO_RECHAZADO
        Logger.d(TAG, "Registrada respuesta RECHAZADO para inspección $inspeccionId, pregunta $preguntaId")
    }

    /**
     * Obtiene el estado actual de una respuesta en memoria
     * @return el estado (CONFORME, NO_CONFORME, ACEPTADO, RECHAZADO) o null si no hay respuesta registrada
     */
    fun obtenerEstadoRespuesta(inspeccionId: Long, preguntaId: Long): String? {
        val key = Pair(inspeccionId, preguntaId)
        return respuestasEnMemoria[key]
    }

    /**
     * Obtiene si una respuesta estaba marcada como "No Conforme" en una inspección previa
     * utilizando el ID de la inspección previa.
     *
     * @param inspeccionRecepcionId ID de la inspección de recepción
     * @param preguntaId ID de la pregunta
     * @return true si la pregunta estaba marcada como "No Conforme", false en caso contrario o si no hay datos
     */
    fun preguntaFueNoConforme(inspeccionRecepcionId: Long, preguntaId: Long): Boolean {
        val key = Pair(inspeccionRecepcionId, preguntaId)
        val estado = respuestasEnMemoria[key]
        return estado == Respuesta.ESTADO_NO_CONFORME
    }

    /**
     * Actualiza el mapa de memoria con datos de la base de datos
     */
    fun actualizarDesdeBaseDeDatos(inspeccionId: Long, respuestasDB: List<RespuestaConDetalles>) {
        for (respuesta in respuestasDB) {
            val preguntaId = respuesta.pregunta.preguntaId
            val key = Pair(inspeccionId, preguntaId)
            respuestasEnMemoria[key] = respuesta.respuesta.estado

            // También registrar el mapeo de respuestaId
            registrarRespuestaId(respuesta.respuesta.respuestaId, inspeccionId, preguntaId)

            Logger.d(TAG, "Actualizado estado desde DB para inspección $inspeccionId, pregunta $preguntaId: ${respuesta.respuesta.estado}")
        }
    }

    /**
     * Limpia todas las respuestas en memoria para una inspección específica
     */
    fun limpiarRespuestas(inspeccionId: Long) {
        val keysToRemove = respuestasEnMemoria.keys
            .filter { it.first == inspeccionId }

        keysToRemove.forEach { key ->
            respuestasEnMemoria.remove(key)
        }

        // También limpiar mapeos de respuestaId
        val respuestaIdsToRemove = respuestaIdMap.entries
            .filter { it.value.first == inspeccionId }
            .map { it.key }

        respuestaIdsToRemove.forEach {
            respuestaIdMap.remove(it)
        }

        Logger.d(TAG, "Limpiadas ${keysToRemove.size} respuestas para inspección $inspeccionId")
    }

    /**
     * Limpia todas las respuestas en memoria
     */
    fun limpiarTodas() {
        val cantidad = respuestasEnMemoria.size
        respuestasEnMemoria.clear()
        respuestaIdMap.clear()
        Logger.d(TAG, "Limpiadas $cantidad respuestas de todas las inspecciones")
    }
}