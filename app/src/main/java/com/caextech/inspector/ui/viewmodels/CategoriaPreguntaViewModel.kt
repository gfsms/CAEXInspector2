package com.caextech.inspector.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.caextech.inspector.data.entities.Categoria
import com.caextech.inspector.data.entities.Pregunta
import com.caextech.inspector.data.relations.CategoriaConPreguntas
import com.caextech.inspector.data.repository.CategoriaRepository
import com.caextech.inspector.data.repository.PreguntaRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de categorías y preguntas de inspección.
 *
 * Esta clase expone datos observables y proporciona métodos para interactuar
 * con los datos de categorías y preguntas de manera coherente con el ciclo de vida de la UI.
 */
class CategoriaPreguntaViewModel(
    private val categoriaRepository: CategoriaRepository,
    private val preguntaRepository: PreguntaRepository
) : ViewModel() {

    // LiveData para notificar eventos de operaciones
    private val _operationStatus = MutableLiveData<OperationStatus>()
    val operationStatus: LiveData<OperationStatus> = _operationStatus

    // LiveData con todas las categorías
    val allCategorias: LiveData<List<Categoria>> = categoriaRepository.allCategorias.asLiveData()

    // LiveData con todas las categorías con sus preguntas
    val allCategoriasConPreguntas: LiveData<List<CategoriaConPreguntas>> =
        categoriaRepository.allCategoriasConPreguntas.asLiveData()

    // LiveData con todas las preguntas
    val allPreguntas: LiveData<List<Pregunta>> = preguntaRepository.allPreguntas.asLiveData()

    /**
     * Obtiene categorías para un modelo específico.
     *
     * @param modelo El modelo de CAEX (797F o 798AC)
     * @return LiveData con la lista de categorías
     */
    fun getCategoriasByModelo(modelo: String): LiveData<List<Categoria>> {
        return categoriaRepository.getCategoriasByModelo(modelo).asLiveData()
    }

    /**
     * Obtiene categorías con sus preguntas para un modelo específico.
     *
     * @param modelo El modelo de CAEX (797F o 798AC)
     * @return LiveData con la lista de categorías con preguntas
     */
    fun getCategoriasConPreguntasByModelo(modelo: String): LiveData<List<CategoriaConPreguntas>> {
        return categoriaRepository.getCategoriasConPreguntasByModelo(modelo).asLiveData()
    }

    /**
     * Obtiene preguntas para una categoría específica.
     *
     * @param categoriaId ID de la categoría
     * @return LiveData con la lista de preguntas
     */
    fun getPreguntasByCategoria(categoriaId: Long): LiveData<List<Pregunta>> {
        return preguntaRepository.getPreguntasByCategoria(categoriaId).asLiveData()
    }

    /**
     * Obtiene preguntas para un modelo específico.
     *
     * @param modelo El modelo de CAEX (797F o 798AC)
     * @return LiveData con la lista de preguntas
     */
    fun getPreguntasByModelo(modelo: String): LiveData<List<Pregunta>> {
        return preguntaRepository.getPreguntasByModelo(modelo).asLiveData()
    }

    /**
     * Obtiene preguntas para una categoría y modelo específicos.
     *
     * @param categoriaId ID de la categoría
     * @param modelo El modelo de CAEX (797F o 798AC)
     * @return LiveData con la lista de preguntas
     */
    fun getPreguntasByCategoriaYModelo(categoriaId: Long, modelo: String): LiveData<List<Pregunta>> {
        return preguntaRepository.getPreguntasByCategoriaYModelo(categoriaId, modelo).asLiveData()
    }

    /**
     * Obtiene una categoría con sus preguntas por su ID.
     *
     * @param categoriaId ID de la categoría
     */
    fun getCategoriaConPreguntas(categoriaId: Long) = viewModelScope.launch {
        try {
            val categoria = categoriaRepository.getCategoriaConPreguntasById(categoriaId)
            if (categoria != null) {
                _categoriaActual.value = categoria
            } else {
                _operationStatus.value = OperationStatus.Error("Categoría no encontrada")
            }
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
        }
    }

    // LiveData para almacenar la categoría actual con sus preguntas
    private val _categoriaActual = MutableLiveData<CategoriaConPreguntas>()
    val categoriaActual: LiveData<CategoriaConPreguntas> = _categoriaActual

    /**
     * Cuenta el número total de preguntas para un modelo.
     *
     * @param modelo El modelo de CAEX (797F o 798AC)
     * @return El número de preguntas (en un LiveData)
     */
    fun countPreguntasByModelo(modelo: String): LiveData<Int> {
        val result = MutableLiveData<Int>()
        viewModelScope.launch {
            try {
                val count = preguntaRepository.countPreguntasByModelo(modelo)
                result.value = count
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
                result.value = 0
            }
        }
        return result
    }

    /**
     * Clase sellada para representar el estado de las operaciones.
     */
    sealed class OperationStatus {
        data class Success(val message: String, val id: Long = 0) : OperationStatus()
        data class Error(val message: String) : OperationStatus()
    }

    /**
     * Factory para crear instancias de CategoriaPreguntaViewModel con los repositorios correctos.
     */
    class CategoriaPreguntaViewModelFactory(
        private val categoriaRepository: CategoriaRepository,
        private val preguntaRepository: PreguntaRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CategoriaPreguntaViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CategoriaPreguntaViewModel(categoriaRepository, preguntaRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}