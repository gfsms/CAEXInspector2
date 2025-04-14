package com.caextech.inspector.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.caextech.inspector.data.entities.Categoria
import com.caextech.inspector.data.entities.Pregunta

/**
 * Clase de relación que combina una Categoría con todas sus Preguntas.
 *
 * Esta clase se utiliza para mostrar las categorías y sus preguntas
 * en la interfaz de usuario del formulario de inspección.
 */
data class CategoriaConPreguntas(
    @Embedded val categoria: Categoria,

    @Relation(
        parentColumn = "categoriaId",
        entityColumn = "categoriaId"
    )
    val preguntas: List<Pregunta>
) {
    /**
     * Filtra las preguntas para un modelo específico de CAEX.
     *
     * @param modelo El modelo de CAEX ("797F" o "798AC")
     * @return Una lista de preguntas aplicables al modelo especificado
     */
    fun getPreguntasParaModelo(modelo: String): List<Pregunta> {
        return preguntas.filter {
            it.modeloAplicable == modelo ||
                    it.modeloAplicable == Pregunta.MODELO_TODOS
        }.sortedBy { it.orden }
    }

    /**
     * Verifica si esta categoría tiene preguntas para un modelo específico.
     *
     * @param modelo El modelo de CAEX ("797F" o "798AC")
     * @return true si hay al menos una pregunta aplicable, false en caso contrario
     */
    fun tienePreguntas(modelo: String): Boolean {
        return getPreguntasParaModelo(modelo).isNotEmpty()
    }
}