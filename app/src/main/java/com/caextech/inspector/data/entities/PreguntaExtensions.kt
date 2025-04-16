package com.caextech.inspector.data.entities

/**
 * Extension functions for the Pregunta entity.
 */

/**
 * Gets the category name for this question.
 *
 * Note: This is a placeholder. In a real implementation, we would look up the category name
 * from the database or pass it in with the question.
 */
fun Pregunta.getCategoriaName(): String {
    // In a real implementation, we would need to fetch this from the database
    // For now, just return a placeholder based on the categoriaId
    return when (categoriaId) {
        1L -> "Condiciones Generales"
        2L -> "Cabina Operador"
        3L -> "Sistema de Dirección"
        4L -> "Sistema de frenos"
        5L -> "Motor Diesel"
        6L -> "Suspensiones delanteras"
        7L -> "Suspensiones traseras"
        8L -> "Sistema estructural"
        9L -> "Sistema eléctrico"
        else -> "Categoría $categoriaId"
    }
}