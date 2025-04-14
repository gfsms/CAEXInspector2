package com.caextech.inspector.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.caextech.inspector.data.dao.CAEXDao
import com.caextech.inspector.data.dao.CategoriaDao
import com.caextech.inspector.data.dao.FotoDao
import com.caextech.inspector.data.dao.InspeccionDao
import com.caextech.inspector.data.dao.PreguntaDao
import com.caextech.inspector.data.dao.RespuestaDao
import com.caextech.inspector.data.entities.CAEX
import com.caextech.inspector.data.entities.Categoria
import com.caextech.inspector.data.entities.Foto
import com.caextech.inspector.data.entities.Inspeccion
import com.caextech.inspector.data.entities.Pregunta
import com.caextech.inspector.data.entities.Respuesta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Clase principal de la base de datos que actúa como punto de acceso a la misma.
 *
 * Esta base de datos contiene todas las entidades necesarias para la aplicación
 * de inspecciones de CAEX. Incluye un callback para prepoblar la base de datos
 * con datos iniciales cuando se crea por primera vez.
 */
@Database(
    entities = [
        CAEX::class,
        Categoria::class,
        Pregunta::class,
        Inspeccion::class,
        Respuesta::class,
        Foto::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    // DAOs para acceder a las entidades
    abstract fun caexDao(): CAEXDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun preguntaDao(): PreguntaDao
    abstract fun inspeccionDao(): InspeccionDao
    abstract fun respuestaDao(): RespuestaDao
    abstract fun fotoDao(): FotoDao

    companion object {
        // Volatile asegura que los cambios en INSTANCE sean visibles inmediatamente para todos los hilos
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Callback para ejecutar acciones cuando se crea la base de datos
        private class AppDatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        // Prepoblar la base de datos con datos iniciales
                        prepopulateDatabase(database)
                    }
                }
            }
        }

        // Patrón Singleton para obtener la instancia de la base de datos
        fun getDatabase(context: Context): AppDatabase {
            // Si la instancia ya existe, la devolvemos
            return INSTANCE ?: synchronized(this) {
                // Si llegamos aquí, necesitamos crear una nueva instancia
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "caex_inspection_database"
                )
                    .fallbackToDestructiveMigration() // En una app real, considera implementar migraciones adecuadas
                    .addCallback(AppDatabaseCallback())
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Prepobla la base de datos con datos iniciales.
         *
         * @param database La instancia de la base de datos a prepoblar
         */
        private suspend fun prepopulateDatabase(database: AppDatabase) {
            // Insertar equipos CAEX de ejemplo
            val caexDao = database.caexDao()
            val categoriaDao = database.categoriaDao()
            val preguntaDao = database.preguntaDao()

            // Insertar algunos CAEX de ejemplo
            if (caexDao.countCAEX() == 0) {
                val caexEjemplos = listOf(
                    CAEX(numeroIdentificador = 301, modelo = "797F"),
                    CAEX(numeroIdentificador = 302, modelo = "797F"),
                    CAEX(numeroIdentificador = 340, modelo = "798AC"),
                    CAEX(numeroIdentificador = 341, modelo = "798AC")
                )
                caexEjemplos.forEach { caexDao.insertCAEX(it) }
            }

            // Insertar categorías para 797F
            if (categoriaDao.countCategorias() == 0) {
                val categorias = listOf(
                    Categoria(nombre = Categoria.CONDICIONES_GENERALES, orden = 1, modeloAplicable = Categoria.MODELO_TODOS),
                    Categoria(nombre = Categoria.CABINA_OPERADOR, orden = 2, modeloAplicable = Categoria.MODELO_TODOS),
                    Categoria(nombre = Categoria.SISTEMA_DIRECCION, orden = 3, modeloAplicable = Categoria.MODELO_TODOS),
                    Categoria(nombre = Categoria.SISTEMA_FRENOS, orden = 4, modeloAplicable = Categoria.MODELO_TODOS),
                    Categoria(nombre = Categoria.MOTOR_DIESEL, orden = 5, modeloAplicable = Categoria.MODELO_TODOS),
                    Categoria(nombre = Categoria.SUSPENSIONES_DELANTERAS, orden = 6, modeloAplicable = Categoria.MODELO_TODOS),
                    Categoria(nombre = Categoria.SUSPENSIONES_TRASERAS, orden = 7, modeloAplicable = Categoria.MODELO_TODOS),
                    Categoria(nombre = Categoria.SISTEMA_ESTRUCTURAL, orden = 8, modeloAplicable = Categoria.MODELO_TODOS),
                    Categoria(nombre = Categoria.SISTEMA_ELECTRICO, orden = 9, modeloAplicable = Categoria.MODELO_798AC)
                )
                val categoriaIds = categoriaDao.insertCategorias(categorias)

                // Asignar IDs a las categorías para crear preguntas
                val categoriasConIds = categorias.zip(categoriaIds).toMap()

                // Crear preguntas para el 797F - Condiciones Generales
                val preguntasCondicionesGenerales797F = listOf(
                    Pregunta(texto = "Extintores contra incendio habilitados en plataforma cabina operador y con inspección al día", orden = 1, categoriaId = categoriasConIds[categorias[0]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Pulsador parada de emergencia en buen estado", orden = 2, categoriaId = categoriasConIds[categorias[0]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Verificar desgaste excesivo y falta de pernos del aro.", orden = 3, categoriaId = categoriasConIds[categorias[0]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Inspección visual y al dia del sistema AFEX / ANSUR", orden = 4, categoriaId = categoriasConIds[categorias[0]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Pasadores de tolva", orden = 5, categoriaId = categoriasConIds[categorias[0]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Fugas sistemas hidraulicos puntos calientes (Motor)", orden = 6, categoriaId = categoriasConIds[categorias[0]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Números de identificación caex instalados (frontal, trasero)", orden = 7, categoriaId = categoriasConIds[categorias[0]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Estanque de combustible sin fugas", orden = 8, categoriaId = categoriasConIds[categorias[0]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Estanque de aceite hidráulico sin fugas", orden = 9, categoriaId = categoriasConIds[categorias[0]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Sistema engrese llega a todos los puntos", orden = 10, categoriaId = categoriasConIds[categorias[0]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Tren de bombas sistema hidráulico sin fugas", orden = 11, categoriaId = categoriasConIds[categorias[0]]!!, modeloAplicable = Pregunta.MODELO_798AC)
                )

                // Crear preguntas para Cabina Operador (común para ambos modelos)
                val preguntasCabinaOperador = listOf(
                    Pregunta(texto = "Panel de alarmas en buen estado", orden = 1, categoriaId = categoriasConIds[categorias[1]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Asiento operador y de copiloto en buen estado (chequear cinturon de seguridad en ambos asientos, apoya brazos, riel de desplazamiento, pulmón de aire)", orden = 2, categoriaId = categoriasConIds[categorias[1]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Espejos en buen estado, sin rayaduras", orden = 3, categoriaId = categoriasConIds[categorias[1]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Revisar bitacora del equipo (dejar registro)", orden = 4, categoriaId = categoriasConIds[categorias[1]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Radio musical y parlantes en buen estado", orden = 5, categoriaId = categoriasConIds[categorias[1]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Testigo indicador virage funcionando (intermitente)", orden = 6, categoriaId = categoriasConIds[categorias[1]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Funcionamiento bocina", orden = 7, categoriaId = categoriasConIds[categorias[1]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Funcionamiento limpia parabrisas", orden = 8, categoriaId = categoriasConIds[categorias[1]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Funcionamiento alza vidrios", orden = 9, categoriaId = categoriasConIds[categorias[1]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Funcionamiento de A/C", orden = 10, categoriaId = categoriasConIds[categorias[1]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Parasol en buen estado", orden = 11, categoriaId = categoriasConIds[categorias[1]]!!, modeloAplicable = Pregunta.MODELO_TODOS)
                )

                // Crear preguntas para Sistema de Dirección
                val preguntasSistemaDireccion = listOf(
                    Pregunta(texto = "Barra de dirección en buen estado", orden = 1, categoriaId = categoriasConIds[categorias[2]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Estado de acumuladores", orden = 2, categoriaId = categoriasConIds[categorias[2]]!!, modeloAplicable = Pregunta.MODELO_798AC),
                    Pregunta(texto = "Fugas de aceite por bombas/cañerías / mangueras / conectores", orden = 3, categoriaId = categoriasConIds[categorias[2]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Cilindros de dirección sin fugas de aceite / sin daños", orden = 4, categoriaId = categoriasConIds[categorias[2]]!!, modeloAplicable = Pregunta.MODELO_797F)
                )

                // Crear preguntas para Sistema de Frenos
                val preguntasSistemaFrenos = listOf(
                    Pregunta(texto = "Fugas de aceite por cañerías / mangueras / conectores", orden = 1, categoriaId = categoriasConIds[categorias[3]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Gabinete hidráulico sin fugas de aceite", orden = 2, categoriaId = categoriasConIds[categorias[3]]!!, modeloAplicable = Pregunta.MODELO_TODOS)
                )

                // Crear preguntas para Motor Diesel
                val preguntasMotorDiesel = listOf(
                    Pregunta(texto = "Fugas de aceite por cañerías / mangueras / conectores", orden = 1, categoriaId = categoriasConIds[categorias[4]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Fugas de combustibles por cañerías / mangueras / turbos / carter", orden = 2, categoriaId = categoriasConIds[categorias[4]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Fugas de refrigerante", orden = 3, categoriaId = categoriasConIds[categorias[4]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Mangueras con roce y/o sueltas", orden = 4, categoriaId = categoriasConIds[categorias[4]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Cables eléctricos sin roce y ruteados bajo estándar", orden = 5, categoriaId = categoriasConIds[categorias[4]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Boquillas sistema AFEX bien direccionadas", orden = 6, categoriaId = categoriasConIds[categorias[4]]!!, modeloAplicable = Pregunta.MODELO_797F)
                )

                // Crear preguntas para Suspensiones Delanteras
                val preguntasSuspensionesDelanteras = listOf(
                    Pregunta(texto = "Estado de sello protector vástago (altura susp.)", orden = 1, categoriaId = categoriasConIds[categorias[5]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Fugas de aceite o grasa", orden = 2, categoriaId = categoriasConIds[categorias[5]]!!, modeloAplicable = Pregunta.MODELO_TODOS)
                )

                // Crear preguntas para Suspensiones Traseras
                val preguntasSuspensionesTraseras = listOf(
                    Pregunta(texto = "Suspensión izquierda con pasador despalzado", orden = 1, categoriaId = categoriasConIds[categorias[6]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Suspensión derecha con pasador desplazado", orden = 2, categoriaId = categoriasConIds[categorias[6]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Articulaciones lubricadas", orden = 3, categoriaId = categoriasConIds[categorias[6]]!!, modeloAplicable = Pregunta.MODELO_TODOS)
                )

                // Crear preguntas para Sistema Estructural
                val preguntasSistemaEstructural = listOf(
                    Pregunta(texto = "Baranda o cadena acceso a escalas emergencia.", orden = 1, categoriaId = categoriasConIds[categorias[7]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Barandas plataforma cabina operador", orden = 2, categoriaId = categoriasConIds[categorias[7]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Barandas escalera de acceso", orden = 3, categoriaId = categoriasConIds[categorias[7]]!!, modeloAplicable = Pregunta.MODELO_TODOS),
                    Pregunta(texto = "Escalera de acceso flotante", orden = 4, categoriaId = categoriasConIds[categorias[7]]!!, modeloAplicable = Pregunta.MODELO_TODOS)
                )

                // Crear preguntas para Sistema Eléctrico (solo 798AC)
                val preguntasSistemaElectrico = listOf(
                    Pregunta(texto = "Alternador sin fugas o cantaminantes", orden = 1, categoriaId = categoriasConIds[categorias[8]]!!, modeloAplicable = Pregunta.MODELO_798AC),
                    Pregunta(texto = "Ducto de ventilación sistema enfriamiento  buen estado", orden = 2, categoriaId = categoriasConIds[categorias[8]]!!, modeloAplicable = Pregunta.MODELO_798AC),
                    Pregunta(texto = "Gabinetes convertidora con candado", orden = 3, categoriaId = categoriasConIds[categorias[8]]!!, modeloAplicable = Pregunta.MODELO_798AC),
                    Pregunta(texto = "Estado sistema de parriillas", orden = 4, categoriaId = categoriasConIds[categorias[8]]!!, modeloAplicable = Pregunta.MODELO_798AC)
                )

                // Insertar todas las preguntas
                val todasLasPreguntas = preguntasCondicionesGenerales797F +
                        preguntasCabinaOperador +
                        preguntasSistemaDireccion +
                        preguntasSistemaFrenos +
                        preguntasMotorDiesel +
                        preguntasSuspensionesDelanteras +
                        preguntasSuspensionesTraseras +
                        preguntasSistemaEstructural +
                        preguntasSistemaElectrico

                preguntaDao.insertPreguntas(todasLasPreguntas)
            }
        }
    }
}