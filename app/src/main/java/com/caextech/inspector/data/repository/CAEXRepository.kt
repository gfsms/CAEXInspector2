package com.caextech.inspector.data.repository

import com.caextech.inspector.data.dao.CAEXDao
import com.caextech.inspector.data.entities.CAEX
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para operaciones relacionadas con los equipos CAEX.
 *
 * Esta clase proporciona métodos para acceder y manipular datos de CAEX,
 * sirviendo como una capa de abstracción entre la base de datos y la UI.
 */
class CAEXRepository(private val caexDao: CAEXDao) {

    // Obtener todos los CAEX como Flow
    val allCAEX: Flow<List<CAEX>> = caexDao.getAllCAEX()

    // Obtener CAEX por modelo como Flow
    fun getCAEXByModelo(modelo: String): Flow<List<CAEX>> {
        return caexDao.getCAEXByModelo(modelo)
    }

    // Insertar un nuevo CAEX
    suspend fun insert(caex: CAEX): Long {
        // Validar que el identificador sea válido para el modelo
        if (!caex.esIdentificadorValido()) {
            throw IllegalArgumentException("El número identificador ${caex.numeroIdentificador} no es válido para el modelo ${caex.modelo}")
        }

        // Verificar que no exista otro CAEX con el mismo identificador
        if (caexDao.existeCAEXConNumeroIdentificador(caex.numeroIdentificador)) {
            throw IllegalArgumentException("Ya existe un CAEX con el número identificador ${caex.numeroIdentificador}")
        }

        return caexDao.insertCAEX(caex)
    }

    // Actualizar un CAEX existente
    suspend fun update(caex: CAEX) {
        // Validar que el identificador sea válido para el modelo
        if (!caex.esIdentificadorValido()) {
            throw IllegalArgumentException("El número identificador ${caex.numeroIdentificador} no es válido para el modelo ${caex.modelo}")
        }

        // Verificar que no exista otro CAEX con el mismo identificador (excepto el actual)
        val existente = caexDao.getCAEXByNumeroIdentificador(caex.numeroIdentificador)
        if (existente != null && existente.caexId != caex.caexId) {
            throw IllegalArgumentException("Ya existe un CAEX con el número identificador ${caex.numeroIdentificador}")
        }

        caexDao.updateCAEX(caex)
    }

    // Eliminar un CAEX
    suspend fun delete(caex: CAEX) {
        caexDao.deleteCAEX(caex)
    }

    // Obtener un CAEX por su ID
    suspend fun getCAEXById(id: Long): CAEX? {
        return caexDao.getCAEXById(id)
    }

    // Obtener un CAEX por su número identificador
    suspend fun getCAEXByNumeroIdentificador(numeroIdentificador: Int): CAEX? {
        return caexDao.getCAEXByNumeroIdentificador(numeroIdentificador)
    }

    // Contar el número total de CAEX
    suspend fun countCAEX(): Int {
        return caexDao.countCAEX()
    }
}