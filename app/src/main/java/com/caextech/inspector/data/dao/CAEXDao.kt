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
}