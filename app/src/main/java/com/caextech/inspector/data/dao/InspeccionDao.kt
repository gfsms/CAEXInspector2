package com.caextech.inspector.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.caextech.inspector.data.entities.Inspeccion
import com.caextech.inspector.data.relations.InspeccionCompleta
import com.caextech.inspector.data.relations.InspeccionConCAEX
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones relacionadas con las inspecciones.
 */
@Dao
interface InspeccionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspeccion(inspeccion: Inspeccion): Long

    @Update
    suspend fun updateInspeccion(inspeccion: Inspeccion)

    @Delete
    suspend fun deleteInspeccion(inspeccion: Inspeccion)

    @Query("SELECT * FROM inspecciones WHERE inspeccionId = :inspeccionId")
    suspend fun getInspeccionById(inspeccionId: Long): Inspeccion?

    @Query("SELECT * FROM inspecciones ORDER BY fechaCreacion DESC")
    fun getAllInspecciones(): Flow<List<Inspeccion>>

    @Query("SELECT * FROM inspecciones WHERE estado = :estado ORDER BY fechaCreacion DESC")
    fun getInspeccionesByEstado(estado: String): Flow<List<Inspeccion>>

    @Query("SELECT * FROM inspecciones WHERE caexId = :caexId ORDER BY fechaCreacion DESC")
    fun getInspeccionesByCAEX(caexId: Long): Flow<List<Inspeccion>>

    @Query("""
        SELECT * FROM inspecciones 
        WHERE caexId = :caexId AND estado = :estado 
        ORDER BY fechaCreacion DESC
    """)
    fun getInspeccionesByCAEXYEstado(caexId: Long, estado: String): Flow<List<Inspeccion>>

    @Query("""
        SELECT * FROM inspecciones 
        WHERE tipo = :tipo 
        ORDER BY fechaCreacion DESC
    """)
    fun getInspeccionesByTipo(tipo: String): Flow<List<Inspeccion>>

    @Query("""
        SELECT * FROM inspecciones 
        WHERE tipo = :tipo AND estado = :estado 
        ORDER BY fechaCreacion DESC
    """)
    fun getInspeccionesByTipoYEstado(tipo: String, estado: String): Flow<List<Inspeccion>>

    @Transaction
    @Query("SELECT * FROM inspecciones WHERE inspeccionId = :inspeccionId")
    suspend fun getInspeccionConCAEXById(inspeccionId: Long): InspeccionConCAEX?

    @Transaction
    @Query("SELECT * FROM inspecciones ORDER BY fechaCreacion DESC")
    fun getAllInspeccionesConCAEX(): Flow<List<InspeccionConCAEX>>

    @Transaction
    @Query("SELECT * FROM inspecciones WHERE estado = :estado ORDER BY fechaCreacion DESC")
    fun getInspeccionesConCAEXByEstado(estado: String): Flow<List<InspeccionConCAEX>>

    @Transaction
    @Query("SELECT * FROM inspecciones WHERE tipo = :tipo ORDER BY fechaCreacion DESC")
    fun getInspeccionesConCAEXByTipo(tipo: String): Flow<List<InspeccionConCAEX>>

    @Transaction
    @Query("""
        SELECT * FROM inspecciones 
        WHERE tipo = :tipo AND estado = :estado 
        ORDER BY fechaCreacion DESC
    """)
    fun getInspeccionesConCAEXByTipoYEstado(tipo: String, estado: String): Flow<List<InspeccionConCAEX>>

    @Transaction
    @Query("SELECT * FROM inspecciones WHERE inspeccionId = :inspeccionId")
    suspend fun getInspeccionCompletaById(inspeccionId: Long): InspeccionCompleta?

    @Query("""
        SELECT COUNT(*) FROM inspecciones 
        WHERE caexId = :caexId AND tipo = :tipo AND estado = :estado
    """)
    suspend fun countInspeccionesByCAEXTipoYEstado(caexId: Long, tipo: String, estado: String): Int

    @Query("""
        SELECT i.* FROM inspecciones i
        JOIN caex c ON i.caexId = c.caexId
        WHERE c.modelo = :modeloCAEX AND i.estado = :estado AND i.tipo = :tipo
        ORDER BY i.fechaCreacion DESC
    """)
    fun getInspeccionesByModeloCAEXEstadoYTipo(modeloCAEX: String, estado: String, tipo: String): Flow<List<Inspeccion>>

    @Transaction
    @Query("""
        SELECT i.* FROM inspecciones i
        JOIN caex c ON i.caexId = c.caexId
        WHERE c.modelo = :modeloCAEX AND i.estado = :estado AND i.tipo = :tipo
        ORDER BY i.fechaCreacion DESC
    """)
    fun getInspeccionesConCAEXByModeloEstadoYTipo(modeloCAEX: String, estado: String, tipo: String): Flow<List<InspeccionConCAEX>>
}