package com.caextech.inspector.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que representa una inspección realizada a un CAEX.
 *
 * Una inspección puede ser de tipo "Recepción" (al llegar el equipo al taller)
 * o de tipo "Entrega" (cuando el equipo sale del taller después de una intervención).
 * Las inspecciones de entrega están vinculadas a una inspección de recepción previa.
 */
@Entity(
    tableName = "inspecciones",
    foreignKeys = [
        ForeignKey(
            entity = CAEX::class,
            parentColumns = ["caexId"],
            childColumns = ["caexId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Inspeccion::class,
            parentColumns = ["inspeccionId"],
            childColumns = ["inspeccionRecepcionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("caexId"),
        Index("inspeccionRecepcionId")
    ]
)
data class Inspeccion(
    @PrimaryKey(autoGenerate = true)
    val inspeccionId: Long = 0,

    // ID del CAEX que se está inspeccionando
    val caexId: Long,

    // Tipo de inspección: RECEPCION o ENTREGA
    val tipo: String,

    // Estado de la inspección: ABIERTA o CERRADA
    val estado: String,

    // Nombre del inspector que realiza la inspección
    val nombreInspector: String,

    // Nombre del supervisor de taller
    val nombreSupervisor: String,

    // ID de la inspección de recepción relacionada (solo para inspecciones de entrega)
    val inspeccionRecepcionId: Long? = null,

    // Fecha de creación de la inspección
    val fechaCreacion: Long = System.currentTimeMillis(),

    // Fecha de finalización de la inspección (cuando se cierra)
    val fechaFinalizacion: Long? = null,

    // Comentarios generales sobre la inspección
    val comentariosGenerales: String = ""
) {
    companion object {
        // Constantes para los tipos de inspección
        const val TIPO_RECEPCION = "RECEPCION"
        const val TIPO_ENTREGA = "ENTREGA"

        // Constantes para los estados de inspección
        const val ESTADO_ABIERTA = "ABIERTA"
        const val ESTADO_CERRADA = "CERRADA"
    }
}