package com.caextech.inspector.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.caextech.inspector.data.entities.CAEX
import com.caextech.inspector.data.repository.CAEXRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de equipos CAEX.
 *
 * Esta clase expone datos observables y proporciona métodos para interactuar
 * con los datos de CAEX de manera coherente con el ciclo de vida de la UI.
 */
class CAEXViewModel(private val repository: CAEXRepository) : ViewModel() {

    // LiveData que contiene todos los CAEX
    val allCAEX: LiveData<List<CAEX>> = repository.allCAEX.asLiveData()

    /**
     * Obtiene CAEX por modelo.
     *
     * @param modelo El modelo de CAEX (797F o 798AC)
     * @return LiveData con la lista de CAEX del modelo especificado
     */
    fun getCAEXByModelo(modelo: String): LiveData<List<CAEX>> {
        return repository.getCAEXByModelo(modelo).asLiveData()
    }

    /**
     * Inserta un nuevo CAEX.
     *
     * @param numeroIdentificador Número identificador del CAEX
     * @param modelo Modelo del CAEX (797F o 798AC)
     */
    fun insertCAEX(numeroIdentificador: Int, modelo: String) = viewModelScope.launch {
        try {
            val caex = CAEX(numeroIdentificador = numeroIdentificador, modelo = modelo)
            repository.insert(caex)
        } catch (e: Exception) {
            // Manejar la excepción (se podría usar un LiveData para notificar a la UI)
        }
    }

    /**
     * Elimina un CAEX.
     *
     * @param caex El CAEX a eliminar
     */
    fun deleteCAEX(caex: CAEX) = viewModelScope.launch {
        repository.delete(caex)
    }

    /**
     * Actualiza un CAEX existente.
     *
     * @param caex El CAEX con los datos actualizados
     */
    fun updateCAEX(caex: CAEX) = viewModelScope.launch {
        try {
            repository.update(caex)
        } catch (e: Exception) {
            // Manejar la excepción
        }
    }

    /**
     * Factory para crear instancias de CAEXViewModel con el repositorio correcto.
     */
    class CAEXViewModelFactory(private val repository: CAEXRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CAEXViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CAEXViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}