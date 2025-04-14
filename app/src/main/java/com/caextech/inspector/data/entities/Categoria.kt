package com.caextech.inspector.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa una categoría de inspección.
 *
 * Las preguntas del checklist se organizan en categorías para facilitar
 * su visualización y organización (ej. "Condiciones Generales", "Cabina Operador").
 * Cada categoría puede ser específica para un modelo de CAEX o aplicar a ambos.
 */
@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true)
    val categoriaId: Long = 0,

    // Nombre de la categoría (ej. "Condiciones Generales", "Cabina Operador")
    val nombre: String,

    // Orden de presentación de la categoría en la interfaz
    val orden: Int,

    // Modelo de CAEX al que aplica esta categoría ("797F", "798AC", o "TODOS")
    val modeloAplicable: String,

    // Fecha de creación del registro
    val fechaCreacion: Long = System.currentTimeMillis()
) {
    companion object {
        // Constantes para los modelos de CAEX
        const val MODELO_797F = "797F"
        const val MODELO_798AC = "798AC"
        const val MODELO_TODOS = "TODOS"

        // Nombres de las categorías comunes
        const val CONDICIONES_GENERALES = "Condiciones Generales"
        const val CABINA_OPERADOR = "Cabina Operador"
        const val SISTEMA_DIRECCION = "Sistema de Dirección"
        const val SISTEMA_FRENOS = "Sistema de frenos"
        const val MOTOR_DIESEL = "Motor Diesel"
        const val SUSPENSIONES_DELANTERAS = "Suspensiones delanteras"
        const val SUSPENSIONES_TRASERAS = "Suspensiones traseras"
        const val SISTEMA_ESTRUCTURAL = "Sistema estructural"
        const val SISTEMA_ELECTRICO = "Sistema eléctrico"
    }
}