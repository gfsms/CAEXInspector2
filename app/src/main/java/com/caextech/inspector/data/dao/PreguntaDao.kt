package com.caextech.inspector.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.caextech.inspector.data.entities.Pregunta
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones relacionadas con las preguntas de inspección.
 */
@Dao
interface PreguntaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPregunta(pregunta: Pregunta): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreguntas(preguntas: List<Pregunta>): List<Long>

    @Update
    suspend fun updatePregunta(pregunta: Pregunta)

    @Delete
    suspend fun deletePregunta(pregunta: Pregunta)

    @Query("SELECT * FROM preguntas WHERE preguntaId = :preguntaId")
    suspend fun getPreguntaById(preguntaId: Long): Pregunta?

    @Query("SELECT * FROM preguntas ORDER BY categoriaId ASC, orden ASC")
    fun getAllPreguntas(): Flow<List<Pregunta>>

    @Query("SELECT * FROM preguntas WHERE categoriaId = :categoriaId ORDER BY orden ASC")
    fun getPreguntasByCategoria(categoriaId: Long): Flow<List<Pregunta>>

    @Query("""
        SELECT * FROM preguntas 
        WHERE (modeloAplicable = :modelo OR modeloAplicable = 'TODOS')
        ORDER BY categoriaId ASC, orden ASC
    """)
    fun getPreguntasByModelo(modelo: String): Flow<List<Pregunta>>

    @Query("""
        SELECT * FROM preguntas 
        WHERE categoriaId = :categoriaId 
        AND (modeloAplicable = :modelo OR modeloAplicable = 'TODOS')
        ORDER BY orden ASC
    """)
    fun getPreguntasByCategoriaYModelo(categoriaId: Long, modelo: String): Flow<List<Pregunta>>

    @Query("""
        SELECT COUNT(*) FROM preguntas 
        WHERE (modeloAplicable = :modelo OR modeloAplicable = 'TODOS')
    """)
    suspend fun countPreguntasByModelo(modelo: String): Int
}