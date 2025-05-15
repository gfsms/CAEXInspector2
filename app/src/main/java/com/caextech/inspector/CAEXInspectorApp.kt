package com.caextech.inspector

import android.app.Application
import androidx.room.Room
import com.caextech.inspector.data.AppDatabase
import com.caextech.inspector.data.repository.CAEXRepository
import com.caextech.inspector.data.repository.CategoriaRepository
import com.caextech.inspector.data.repository.FotoRepository
import com.caextech.inspector.data.repository.InspeccionRepository
import com.caextech.inspector.data.repository.PreguntaRepository
import com.caextech.inspector.data.repository.RespuestaRepository

/**
 * Clase de aplicación principal para inicializar la base de datos y los repositorios.
 */
class CAEXInspectorApp : Application() {

    // Inicialización lazy de la base de datos
    val database by lazy { AppDatabase.getDatabase(this) }

    // Repositorios con acceso a la base de datos
    val caexRepository by lazy {
        CAEXRepository(database.caexDao(), database.inspeccionDao())
    }
    val categoriaRepository by lazy { CategoriaRepository(database.categoriaDao()) }
    val preguntaRepository by lazy { PreguntaRepository(database.preguntaDao()) }
    val inspeccionRepository by lazy {
        InspeccionRepository(
            database.inspeccionDao(),
            database.respuestaDao(),
            database.preguntaDao(),
            database.caexDao()
        )
    }
    val respuestaRepository by lazy { RespuestaRepository(database.respuestaDao()) }
    val fotoRepository by lazy { FotoRepository(database.fotoDao(), this) }
}