package com.caextech.inspector.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que representa una fotografía tomada como evidencia para una respuesta.
 *
 * Las fotos se toman como evidencia cuando una pregunta se marca como "No Conforme".
 * Cada foto está asociada a una respuesta específica y contiene la ruta del archivo
 * en el almacenamiento del dispositivo.
 */
@Entity(
    tableName = "fotos",
    foreignKeys = [
        ForeignKey(
            entity = Respuesta::class,
            parentColumns = ["respuestaId"],
            childColumns = ["respuestaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("respuestaId")]
)
data class Foto(
    @PrimaryKey(autoGenerate = true)
    val fotoId: Long = 0,

    // ID de la respuesta a la que está asociada esta foto
    val respuestaId: Long,

    // Ruta del archivo de imagen en el almacenamiento
    val rutaArchivo: String,

    // Descripción opcional de la foto
    val descripcion: String = "",

    // Fecha de creación de la foto
    val fechaCreacion: Long = System.currentTimeMillis()
) {
    /**
     * Obtiene el nombre del archivo a partir de la ruta completa.
     *
     * @return El nombre del archivo sin la ruta
     */
    fun getNombreArchivo(): String {
        return rutaArchivo.substringAfterLast('/')
    }
}