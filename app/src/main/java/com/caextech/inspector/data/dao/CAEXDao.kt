package com.caextech.inspector.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.caextech.inspector.data.entities.CAEX
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones relacionadas con los equipos CAEX.
 */
@Dao
interface CAEXDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCAEX(caex: CAEX): Long

    @Update
    suspend fun updateCAEX(caex: CAEX)

    @Delete
    suspend fun deleteCAEX(caex: CAEX)

    @Query("SELECT * FROM caex WHERE caexId = :caexId")
    suspend fun getCAEXById(caexId: Long): CAEX?

    @Query("SELECT * FROM caex ORDER BY modelo ASC, numeroIdentificador ASC")
    fun getAllCAEX(): Flow<List<CAEX>>

    @Query("SELECT * FROM caex WHERE modelo = :modelo ORDER BY numeroIdentificador ASC")
    fun getCAEXByModelo(modelo: String): Flow<List<CAEX>>

    @Query("SELECT * FROM caex WHERE numeroIdentificador = :numeroIdentificador LIMIT 1")
    suspend fun getCAEXByNumeroIdentificador(numeroIdentificador: Int): CAEX?

    @Query("SELECT COUNT(*) FROM caex")
    suspend fun countCAEX(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM caex WHERE numeroIdentificador = :numeroIdentificador LIMIT 1)")
    suspend fun existeCAEXConNumeroIdentificador(numeroIdentificador: Int): Boolean

    /**
     * Obtiene estadísticas de conformidad para un CAEX específico
     */
    @Query("""
    SELECT 
        :caexId as caexId,
        COUNT(DISTINCT r.respuestaId) as totalRespuestas,
        COUNT(CASE WHEN r.estado IN ('NO_CONFORME', 'RECHAZADO') THEN r.respuestaId END) as totalHallazgos
    FROM respuestas r
    JOIN inspecciones i ON r.inspeccionId = i.inspeccionId  
    WHERE i.caexId = :caexId AND i.estado = 'CERRADA'
""")
    suspend fun getEstadisticasConformidadCAEX(caexId: Long): ConformidadStats?

    /**
     * Obtiene estadísticas de conformidad para todos los CAEX
     */
    @Query("""
    SELECT 
        i.caexId,
        COUNT(DISTINCT r.respuestaId) as totalRespuestas,
        COUNT(CASE WHEN r.estado IN ('NO_CONFORME', 'RECHAZADO') THEN r.respuestaId END) as totalHallazgos
    FROM respuestas r
    JOIN inspecciones i ON r.inspeccionId = i.inspeccionId  
    WHERE i.estado = 'CERRADA'
    GROUP BY i.caexId
""")
    suspend fun getAllEstadisticasConformidad(): List<ConformidadStats>

    /**
     * Data class para las estadísticas de conformidad
     * AGREGAR esta data class al archivo CAEXDao.kt:
     */
    data class ConformidadStats(
        val caexId: Long = 0,
        val totalRespuestas: Int = 0,
        val totalHallazgos: Int = 0
    ) {
        fun getPorcentajeConformidad(): Float {
            return if (totalRespuestas > 0) {
                ((totalRespuestas - totalHallazgos).toFloat() / totalRespuestas) * 100f
            } else {
                100f
            }
        }
    }
}