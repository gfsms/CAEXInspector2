package com.caextech.inspector.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.caextech.inspector.data.entities.Foto
import com.caextech.inspector.data.entities.Pregunta
import com.caextech.inspector.data.entities.Respuesta

/**
 * Clase de relación que combina una Respuesta con su Pregunta y Fotos asociadas.
 *
 * Esta clase proporciona toda la información necesaria para mostrar una respuesta
 * completa en la interfaz de usuario, incluyendo el texto de la pregunta y
 * las fotos de evidencia (si existen).
 */
data class RespuestaConDetalles(
    @Embedded val respuesta: Respuesta,

    @Relation(
        parentColumn = "preguntaId",
        entityColumn = "preguntaId"
    )
    val pregunta: Pregunta,

    @Relation(
        parentColumn = "respuestaId",
        entityColumn = "respuestaId"
    )
    val fotos: List<Foto> = emptyList()
) {
    /**
     * Verifica si esta respuesta tiene fotos asociadas.
     *
     * @return true si hay al menos una foto, false en caso contrario
     */
    fun tieneFotos(): Boolean {
        return fotos.isNotEmpty()
    }

    /**
     * Verifica si la respuesta es conforme.
     *
     * @return true si la respuesta es conforme, false si es no conforme
     */
    fun esConforme(): Boolean {
        return respuesta.estado == Respuesta.ESTADO_CONFORME
    }

    /**
     * Obtiene una descripción del estado de la respuesta.
     *
     * @return Un string descriptivo del estado
     */
    fun getEstadoDescriptivo(): String {
        return when (respuesta.estado) {
            Respuesta.ESTADO_CONFORME -> "Conforme"
            Respuesta.ESTADO_NO_CONFORME -> "No Conforme"
            else -> "Estado desconocido"
        }
    }

    /**
     * Obtiene una descripción del tipo de acción para respuestas no conformes.
     *
     * @return Un string descriptivo del tipo de acción, o null si no aplica
     */
    fun getTipoAccionDescriptivo(): String? {
        return when (respuesta.tipoAccion) {
            Respuesta.ACCION_INMEDIATO -> "Acción Inmediata - Aviso: ${respuesta.idAvisoOrdenTrabajo}"
            Respuesta.ACCION_PROGRAMADO -> "Acción Programada - OT: ${respuesta.idAvisoOrdenTrabajo}"
            else -> null
        }
    }
}