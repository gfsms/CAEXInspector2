package com.caextech.inspector.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.caextech.inspector.data.entities.Foto
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones relacionadas con las fotos de evidencia.
 */
@Dao
interface FotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoto(foto: Foto): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFotos(fotos: List<Foto>): List<Long>

    @Update
    suspend fun updateFoto(foto: Foto)

    @Delete
    suspend fun deleteFoto(foto: Foto)

    @Query("SELECT * FROM fotos WHERE fotoId = :fotoId")
    suspend fun getFotoById(fotoId: Long): Foto?

    @Query("SELECT * FROM fotos WHERE respuestaId = :respuestaId ORDER BY fechaCreacion ASC")
    fun getFotosByRespuesta(respuestaId: Long): Flow<List<Foto>>

    @Query("""
        SELECT f.* FROM fotos f
        JOIN respuestas r ON f.respuestaId = r.respuestaId
        WHERE r.inspeccionId = :inspeccionId
        ORDER BY f.fechaCreacion ASC
    """)
    fun getFotosByInspeccion(inspeccionId: Long): Flow<List<Foto>>

    @Query("SELECT COUNT(*) FROM fotos WHERE respuestaId = :respuestaId")
    suspend fun countFotosByRespuesta(respuestaId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM fotos f
        JOIN respuestas r ON f.respuestaId = r.respuestaId
        WHERE r.inspeccionId = :inspeccionId
    """)
    suspend fun countFotosByInspeccion(inspeccionId: Long): Int

    @Query("DELETE FROM fotos WHERE respuestaId = :respuestaId")
    suspend fun deleteFotosByRespuesta(respuestaId: Long)

    @Query("""
        DELETE FROM fotos 
        WHERE respuestaId IN (
            SELECT respuestaId FROM respuestas WHERE inspeccionId = :inspeccionId
        )
    """)
    suspend fun deleteFotosByInspeccion(inspeccionId: Long)
}