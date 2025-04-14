package com.caextech.inspector.data.repository

import com.caextech.inspector.data.dao.PreguntaDao
import com.caextech.inspector.data.entities.Pregunta
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para operaciones relacionadas con las preguntas de inspección.
 *
 * Esta clase proporciona métodos para acceder y manipular datos de preguntas,
 * sirviendo como una capa de abstracción entre la base de datos y la UI.
 */
class PreguntaRepository(private val preguntaDao: PreguntaDao) {

    // Obtener todas las preguntas como Flow
    val allPreguntas: Flow<List<Pregunta>> = preguntaDao.getAllPreguntas()

    // Obtener preguntas por categoría como Flow
    fun getPreguntasByCategoria(categoriaId: Long): Flow<List<Pregunta>> {
        return preguntaDao.getPreguntasByCategoria(categoriaId)
    }

    // Obtener preguntas por modelo como Flow
    fun getPreguntasByModelo(modelo: String): Flow<List<Pregunta>> {
        return preguntaDao.getPreguntasByModelo(modelo)
    }

    // Obtener preguntas por categoría y modelo como Flow
    fun getPreguntasByCategoriaYModelo(categoriaId: Long, modelo: String): Flow<List<Pregunta>> {
        return preguntaDao.getPreguntasByCategoriaYModelo(categoriaId, modelo)
    }

    // Insertar una nueva pregunta
    suspend fun insert(pregunta: Pregunta): Long {
        return preguntaDao.insertPregunta(pregunta)
    }

    // Insertar varias preguntas a la vez
    suspend fun insertAll(preguntas: List<Pregunta>): List<Long> {
        return preguntaDao.insertPreguntas(preguntas)
    }

    // Actualizar una pregunta existente
    suspend fun update(pregunta: Pregunta) {
        preguntaDao.updatePregunta(pregunta)
    }

    // Eliminar una pregunta
    suspend fun delete(pregunta: Pregunta) {
        preguntaDao.deletePregunta(pregunta)
    }

    // Obtener una pregunta por su ID
    suspend fun getPreguntaById(id: Long): Pregunta? {
        return preguntaDao.getPreguntaById(id)
    }

    // Contar el número total de preguntas para un modelo
    suspend fun countPreguntasByModelo(modelo: String): Int {
        return preguntaDao.countPreguntasByModelo(modelo)
    }
}