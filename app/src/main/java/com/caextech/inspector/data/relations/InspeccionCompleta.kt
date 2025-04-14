package com.caextech.inspector.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.caextech.inspector.data.entities.CAEX
import com.caextech.inspector.data.entities.Inspeccion
import com.caextech.inspector.data.entities.Respuesta

/**
 * Clase de relación que encapsula una inspección completa con todas sus respuestas.
 *
 * Esta clase proporciona acceso a todos los datos necesarios para mostrar
 * o procesar una inspección: el equipo inspeccionado, la inspección en sí,
 * y todas las respuestas registradas.
 */
data class InspeccionCompleta(
    @Embedded val inspeccion: Inspeccion,

    @Relation(
        parentColumn = "caexId",
        entityColumn = "caexId"
    )
    val caex: CAEX,

    @Relation(
        parentColumn = "inspeccionId",
        entityColumn = "inspeccionId"
    )
    val respuestas: List<Respuesta>
) {
    /**
     * Verifica si la inspección está completa (todas las preguntas tienen respuesta).
     *
     * @param totalPreguntas El número total de preguntas que deben responderse
     * @return true si todas las preguntas tienen respuesta, false en caso contrario
     */
    fun estaCompleta(totalPreguntas: Int): Boolean {
        return respuestas.size == totalPreguntas
    }

    /**
     * Cuenta el número de respuestas "No Conforme".
     *
     * @return El número de respuestas marcadas como "No Conforme"
     */
    fun contarNoConformes(): Int {
        return respuestas.count { it.estado == Respuesta.ESTADO_NO_CONFORME }
    }

    /**
     * Verifica si la inspección puede cerrarse.
     * Una inspección puede cerrarse si está completa y todas las respuestas son válidas.
     *
     * @param totalPreguntas El número total de preguntas que deben responderse
     * @return true si la inspección puede cerrarse, false en caso contrario
     */
    fun puedecerrarse(totalPreguntas: Int): Boolean {
        // Verificar que la inspección esté completa
        if (!estaCompleta(totalPreguntas)) {
            return false
        }

        // Verificar que todas las respuestas sean válidas
        return respuestas.all { it.esValida() }
    }
}