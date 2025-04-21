package com.caextech.inspector.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que representa una respuesta a una pregunta en una inspección.
 *
 * Cada respuesta está vinculada a una inspección y a una pregunta específicas.
 * El estado puede ser "Conforme" o "No Conforme" para inspecciones de recepción,
 * o "Aceptado" o "Rechazado" para inspecciones de entrega.
 * Para las respuestas "No Conforme" o "Rechazado", se registran comentarios,
 * tipo de acción y el ID del aviso u orden de trabajo.
 */
@Entity(
    tableName = "respuestas",
    foreignKeys = [
        ForeignKey(
            entity = Inspeccion::class,
            parentColumns = ["inspeccionId"],
            childColumns = ["inspeccionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Pregunta::class,
            parentColumns = ["preguntaId"],
            childColumns = ["preguntaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("inspeccionId"),
        Index("preguntaId"),
        Index(value = ["inspeccionId", "preguntaId"], unique = true)
    ]
)
data class Respuesta(
    @PrimaryKey(autoGenerate = true)
    val respuestaId: Long = 0,

    // ID de la inspección a la que pertenece esta respuesta
    val inspeccionId: Long,

    // ID de la pregunta a la que responde
    val preguntaId: Long,

    // Estado de la respuesta: CONFORME, NO_CONFORME, ACEPTADO, RECHAZADO
    val estado: String,

    // Comentarios (obligatorio para respuestas NO_CONFORME y RECHAZADO)
    val comentarios: String = "",

    // Tipo de acción para NO_CONFORME o RECHAZADO: INMEDIATO o PROGRAMADO
    val tipoAccion: String? = null,

    // ID del aviso (para acciones INMEDIATO) u orden de trabajo (para PROGRAMADO)
    val idAvisoOrdenTrabajo: String? = null,

    // Fecha de creación de la respuesta
    val fechaCreacion: Long = System.currentTimeMillis(),

    // Fecha de última modificación
    val fechaModificacion: Long = System.currentTimeMillis()
) {
    companion object {
        // Constantes para los estados de respuesta de recepción
        const val ESTADO_CONFORME = "CONFORME"
        const val ESTADO_NO_CONFORME = "NO_CONFORME"

        // Constantes para los estados de respuesta de entrega
        const val ESTADO_ACEPTADO = "ACEPTADO"
        const val ESTADO_RECHAZADO = "RECHAZADO"

        // Constantes para los tipos de acción
        const val ACCION_INMEDIATO = "INMEDIATO"
        const val ACCION_PROGRAMADO = "PROGRAMADO"
    }

    /**
     * Verifica si esta respuesta es válida según su estado.
     *
     * @return true si la respuesta es válida, false en caso contrario
     */
    fun esValida(): Boolean {
        // Si es CONFORME o ACEPTADO, no necesitamos verificar nada más
        if (estado == ESTADO_CONFORME || estado == ESTADO_ACEPTADO) {
            return true
        }

        // Si es NO_CONFORME o RECHAZADO, debe tener comentarios
        if (estado == ESTADO_NO_CONFORME || estado == ESTADO_RECHAZADO) {
            if (comentarios.isBlank()) {
                return false
            }

            // Para respuestas en inspección de cierre, no requieren tipo de acción ni ID SAP
            if (estado == ESTADO_RECHAZADO) {
                return true
            }

            // Para NO_CONFORME, debe tener un tipo de acción y un ID de aviso/orden válido
            if (tipoAccion.isNullOrBlank()) {
                return false
            }

            // Debe tener un ID de aviso/orden
            if (idAvisoOrdenTrabajo.isNullOrBlank()) {
                return false
            }

            // El tipo de acción debe ser válido
            if (tipoAccion != ACCION_INMEDIATO && tipoAccion != ACCION_PROGRAMADO) {
                return false
            }
        }

        return true
    }
}