package com.caextech.inspector.data.repository

import com.caextech.inspector.data.dao.CAEXDao
import com.caextech.inspector.data.dao.InspeccionDao
import com.caextech.inspector.data.entities.CAEX
import com.caextech.inspector.data.entities.Inspeccion
import com.caextech.inspector.data.models.CAEXConInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Repositorio para operaciones relacionadas con los equipos CAEX.
 *
 * Esta clase proporciona métodos para acceder y manipular datos de CAEX,
 * sirviendo como una capa de abstracción entre la base de datos y la UI.
 */
class CAEXRepository(
    private val caexDao: CAEXDao,
    private val inspeccionDao: InspeccionDao
) {

    // Obtener todos los CAEX como Flow
    val allCAEX: Flow<List<CAEX>> = caexDao.getAllCAEX()

    // Obtener todos los CAEX con información de inspecciones
    val allCAEXWithInfo: Flow<List<CAEXConInfo>> =
        caexDao.getAllCAEX().combine(inspeccionDao.getAllInspecciones()) { caexList, inspeccionesList ->
            caexList.map { caex ->
                val inspecciones = inspeccionesList.filter { it.caexId == caex.caexId }
                createCAEXConInfo(caex, inspecciones)
            }
        }

    /**
     * Busca CAEX por texto (número o modelo)
     */
    fun searchCAEX(searchText: String): Flow<List<CAEXConInfo>> {
        return allCAEXWithInfo.combine(allCAEX) { caexWithInfo, _ ->
            if (searchText.isBlank()) {
                caexWithInfo
            } else {
                caexWithInfo.filter { caexInfo ->
                    caexInfo.caex.numeroIdentificador.toString().contains(searchText, ignoreCase = true) ||
                            caexInfo.caex.modelo.contains(searchText, ignoreCase = true)
                }
            }
        }
    }

    /**
     * Filtra CAEX por modelo
     */
    fun filterCAEXByModel(modelo: String): Flow<List<CAEXConInfo>> {
        return allCAEXWithInfo.combine(allCAEX) { caexWithInfo, _ ->
            if (modelo == "TODOS") {
                caexWithInfo
            } else {
                caexWithInfo.filter { it.caex.modelo == modelo }
            }
        }
    }

    /**
     * Busca y filtra CAEX combinado
     */
    fun searchAndFilterCAEX(searchText: String, modelo: String): Flow<List<CAEXConInfo>> {
        return allCAEXWithInfo.combine(allCAEX) { caexWithInfo, _ ->
            var filtered = caexWithInfo

            // Aplicar filtro de modelo
            if (modelo != "TODOS") {
                filtered = filtered.filter { it.caex.modelo == modelo }
            }

            // Aplicar filtro de búsqueda
            if (searchText.isNotBlank()) {
                filtered = filtered.filter { caexInfo ->
                    caexInfo.caex.numeroIdentificador.toString().contains(searchText, ignoreCase = true) ||
                            caexInfo.caex.modelo.contains(searchText, ignoreCase = true)
                }
            }

            filtered
        }
    }

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

    /**
     * Crea un objeto CAEXConInfo a partir de un CAEX y sus inspecciones
     */
    private fun createCAEXConInfo(caex: CAEX, inspecciones: List<Inspeccion>): CAEXConInfo {
        val totalInspecciones = inspecciones.size

        // Buscar inspecciones abiertas
        val tieneInspeccionPendiente = inspecciones.any {
            it.estado == Inspeccion.ESTADO_ABIERTA || it.estado == Inspeccion.ESTADO_PENDIENTE_CIERRE
        }

        // Buscar última inspección cerrada
        val ultimaInspeccion = inspecciones
            .filter { it.estado == Inspeccion.ESTADO_CERRADA || it.estado == Inspeccion.ESTADO_PENDIENTE_CIERRE }
            .maxByOrNull { it.fechaCreacion }

        return CAEXConInfo(
            caex = caex,
            totalInspecciones = totalInspecciones,
            fechaUltimaInspeccion = ultimaInspeccion?.fechaCreacion,
            tipoUltimaInspeccion = ultimaInspeccion?.tipo,
            estadoUltimaInspeccion = ultimaInspeccion?.estado,
            tieneInspeccionPendiente = tieneInspeccionPendiente
        )
    }
}