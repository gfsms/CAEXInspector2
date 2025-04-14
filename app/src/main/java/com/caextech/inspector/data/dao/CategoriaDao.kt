package com.caextech.inspector.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.caextech.inspector.data.entities.Categoria
import com.caextech.inspector.data.relations.CategoriaConPreguntas
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones relacionadas con las categorías de inspección.
 */
@Dao
interface CategoriaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoria(categoria: Categoria): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategorias(categorias: List<Categoria>): List<Long>

    @Update
    suspend fun updateCategoria(categoria: Categoria)

    @Delete
    suspend fun deleteCategoria(categoria: Categoria)

    @Query("SELECT * FROM categorias WHERE categoriaId = :categoriaId")
    suspend fun getCategoriaById(categoriaId: Long): Categoria?

    @Query("SELECT * FROM categorias ORDER BY orden ASC")
    fun getAllCategorias(): Flow<List<Categoria>>

    @Query("SELECT * FROM categorias WHERE modeloAplicable = :modelo OR modeloAplicable = 'TODOS' ORDER BY orden ASC")
    fun getCategoriasByModelo(modelo: String): Flow<List<Categoria>>

    @Transaction
    @Query("SELECT * FROM categorias WHERE categoriaId = :categoriaId")
    suspend fun getCategoriaConPreguntasById(categoriaId: Long): CategoriaConPreguntas?

    @Transaction
    @Query("SELECT * FROM categorias ORDER BY orden ASC")
    fun getAllCategoriasConPreguntas(): Flow<List<CategoriaConPreguntas>>

    @Transaction
    @Query("SELECT * FROM categorias WHERE modeloAplicable = :modelo OR modeloAplicable = 'TODOS' ORDER BY orden ASC")
    fun getCategoriasConPreguntasByModelo(modelo: String): Flow<List<CategoriaConPreguntas>>

    @Query("SELECT COUNT(*) FROM categorias")
    suspend fun countCategorias(): Int
}