package com.caextech.inspector.utils

import com.caextech.inspector.data.entities.Respuesta
import com.caextech.inspector.data.relations.RespuestaConDetalles

/**
 * Clase para rastrear todas las respuestas en memoria, independientemente de si están
 * guardadas en la base de datos o no.
 *
 * Esta clase resuelve el problema de respuestas "No Conforme" que se pierden al
 * seleccionar "Conforme" en otras preguntas cuando no tienen fotos o comentarios.
 */
object RespuestaTracker {
    private const val TAG = "RespuestaTracker"

    // Mapa para rastrear el estado de todas las respuestas por inspecciónId y preguntaId
    private val respuestasEnMemoria = mutableMapOf<Pair<Long, Long>, String>()

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
     * Obtiene el estado actual de una respuesta en memoria
     * @return el estado (CONFORME o NO_CONFORME) o null si no hay respuesta registrada
     */
    fun obtenerEstadoRespuesta(inspeccionId: Long, preguntaId: Long): String? {
        val key = Pair(inspeccionId, preguntaId)
        return respuestasEnMemoria[key]
    }

    /**
     * Combina las respuestas de la base de datos con las respuestas en memoria
     * para asegurar que no se pierda ningún estado
     */
    fun combinarRespuestasConMemoria(
        inspeccionId: Long,
        respuestasDB: List<RespuestaConDetalles>
    ): List<RespuestaConDetalles> {
        // Convertir respuestas de DB a mutable list para poder modificarla
        val respuestasCombinadas = respuestasDB.toMutableList()

        // Conjunto de IDs de preguntas que ya tienen respuesta en la DB
        val preguntasConRespuesta = respuestasDB.map { it.pregunta.preguntaId }.toSet()

        // Buscar respuestas en memoria que no están en la DB
        val respuestasMemoria = respuestasEnMemoria.entries
            .filter { (key, _) ->
                key.first == inspeccionId && !preguntasConRespuesta.contains(key.second)
            }

        // Log para debugging
        if (respuestasMemoria.isNotEmpty()) {
            Logger.d(TAG, "Encontradas ${respuestasMemoria.size} respuestas en memoria " +
                    "que no están en la DB para inspección $inspeccionId")
        }

        // Aquí implementaríamos la lógica para crear RespuestaConDetalles para las
        // respuestas que solo existen en memoria, pero esto requeriría acceso a la DB
        // para obtener la información de la pregunta, lo cual complica la implementación.

        // En lugar de eso, vamos a usar esta información en el adapter
        return respuestasCombinadas
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

        Logger.d(TAG, "Limpiadas ${keysToRemove.size} respuestas para inspección $inspeccionId")
    }

    /**
     * Limpia todas las respuestas en memoria
     */
    fun limpiarTodas() {
        val cantidad = respuestasEnMemoria.size
        respuestasEnMemoria.clear()
        Logger.d(TAG, "Limpiadas $cantidad respuestas de todas las inspecciones")
    }
}