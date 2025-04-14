package com.caextech.inspector.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.caextech.inspector.data.entities.Respuesta
import com.caextech.inspector.data.relations.RespuestaConDetalles
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones relacionadas con las respuestas de inspección.
 */
@Dao
interface RespuestaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRespuesta(respuesta: Respuesta): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRespuestas(respuestas: List<Respuesta>): List<Long>

    @Update
    suspend fun updateRespuesta(respuesta: Respuesta)

    @Delete
    suspend fun deleteRespuesta(respuesta: Respuesta)

    @Query("SELECT * FROM respuestas WHERE respuestaId = :respuestaId")
    suspend fun getRespuestaById(respuestaId: Long): Respuesta?

    @Query("SELECT * FROM respuestas WHERE inspeccionId = :inspeccionId")
    fun getRespuestasByInspeccion(inspeccionId: Long): Flow<List<Respuesta>>

    @Query("SELECT * FROM respuestas WHERE inspeccionId = :inspeccionId AND estado = :estado")
    fun getRespuestasByInspeccionYEstado(inspeccionId: Long, estado: String): Flow<List<Respuesta>>

    @Query("""
        SELECT * FROM respuestas 
        WHERE inspeccionId = :inspeccionId AND preguntaId = :preguntaId
        LIMIT 1
    """)
    suspend fun getRespuestaPorInspeccionYPregunta(inspeccionId: Long, preguntaId: Long): Respuesta?

    @Transaction
    @Query("SELECT * FROM respuestas WHERE respuestaId = :respuestaId")
    suspend fun getRespuestaConDetallesById(respuestaId: Long): RespuestaConDetalles?

    @Transaction
    @Query("SELECT * FROM respuestas WHERE inspeccionId = :inspeccionId")
    fun getRespuestasConDetallesByInspeccion(inspeccionId: Long): Flow<List<RespuestaConDetalles>>

    @Transaction
    @Query("""
        SELECT r.* FROM respuestas r
        JOIN preguntas p ON r.preguntaId = p.preguntaId
        WHERE r.inspeccionId = :inspeccionId
        ORDER BY p.categoriaId ASC, p.orden ASC
    """)
    fun getRespuestasConDetallesByInspeccionOrdenadas(inspeccionId: Long): Flow<List<RespuestaConDetalles>>

    @Transaction
    @Query("""
        SELECT r.* FROM respuestas r
        JOIN preguntas p ON r.preguntaId = p.preguntaId
        WHERE r.inspeccionId = :inspeccionId AND p.categoriaId = :categoriaId
        ORDER BY p.orden ASC
    """)
    fun getRespuestasConDetallesByInspeccionYCategoria(
        inspeccionId: Long,
        categoriaId: Long
    ): Flow<List<RespuestaConDetalles>>

    @Query("SELECT COUNT(*) FROM respuestas WHERE inspeccionId = :inspeccionId")
    suspend fun countRespuestasByInspeccion(inspeccionId: Long): Int

    @Query("SELECT COUNT(*) FROM respuestas WHERE inspeccionId = :inspeccionId AND estado = :estado")
    suspend fun countRespuestasByInspeccionYEstado(inspeccionId: Long, estado: String): Int

    @Query("DELETE FROM respuestas WHERE inspeccionId = :inspeccionId")
    suspend fun deleteRespuestasByInspeccion(inspeccionId: Long)
}