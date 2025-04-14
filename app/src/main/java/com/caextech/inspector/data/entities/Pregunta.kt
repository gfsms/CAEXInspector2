package com.caextech.inspector.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que representa una pregunta de inspección.
 *
 * Cada pregunta pertenece a una categoría y se evalúa durante una inspección
 * como "Conforme" o "No Conforme". Las preguntas pueden ser específicas para
 * un modelo de CAEX o aplicar a ambos.
 */
@Entity(
    tableName = "preguntas",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["categoriaId"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoriaId")]
)
data class Pregunta(
    @PrimaryKey(autoGenerate = true)
    val preguntaId: Long = 0,

    // Texto de la pregunta (ej. "Extintores contra incendio habilitados...")
    val texto: String,

    // ID de la categoría a la que pertenece esta pregunta
    val categoriaId: Long,

    // Orden de presentación de la pregunta dentro de su categoría
    val orden: Int,

    // Modelo de CAEX al que aplica esta pregunta ("797F", "798AC", o "TODOS")
    val modeloAplicable: String,

    // Fecha de creación del registro
    val fechaCreacion: Long = System.currentTimeMillis()
) {
    companion object {
        // Constantes para los modelos de CAEX
        const val MODELO_797F = "797F"
        const val MODELO_798AC = "798AC"
        const val MODELO_TODOS = "TODOS"
    }
}