package com.caextech.inspector.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.caextech.inspector.data.AppDatabase
import com.caextech.inspector.data.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Utilidad para manejar la exportación e importación de datos de la aplicación.
 *
 * Esta clase encapsula toda la lógica compleja necesaria para:
 * - Serializar la base de datos a JSON
 * - Manejar archivos ZIP
 * - Copiar fotografías manteniendo la estructura
 * - Validar la integridad de los datos
 *
 * La arquitectura del respaldo es:
 * - metadata.json: Información sobre el respaldo (versión, fecha, etc.)
 * - database.json: Todos los datos de Room serializados
 * - photos/: Directorio con todas las fotografías
 */
object BackupUtils {

    // Versión del formato de respaldo para compatibilidad futura
    private const val BACKUP_VERSION = 1
    private const val METADATA_FILE = "metadata.json"
    private const val DATABASE_FILE = "database.json"
    private const val PHOTOS_DIR = "photos/"

    /**
     * Exporta toda la base de datos y fotografías a un archivo ZIP.
     *
     * El proceso detallado es:
     * 1. Crear directorio temporal de trabajo
     * 2. Generar metadata.json con información del respaldo
     * 3. Exportar cada tabla de la base de datos a database.json
     * 4. Copiar todas las fotografías al directorio photos/
     * 5. Comprimir todo en un ZIP
     * 6. Limpiar archivos temporales
     *
     * @param context Contexto de la aplicación
     * @param database Instancia de la base de datos Room
     * @param outputFile Archivo ZIP de salida
     * @param progressCallback Callback para reportar progreso a la UI
     * @return true si la exportación fue exitosa, false en caso contrario
     */
    suspend fun exportToZip(
        context: Context,
        database: AppDatabase,
        outputFile: File,
        progressCallback: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Crear directorio temporal para trabajar
            val tempDir = File(context.cacheDir, "backup_temp_${System.currentTimeMillis()}")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }

            try {
                // Paso 1: Crear metadata
                progressCallback("Creando metadatos del respaldo...")
                val metadataFile = File(tempDir, METADATA_FILE)
                createMetadataFile(metadataFile)

                // Paso 2: Exportar base de datos
                progressCallback("Exportando base de datos...")
                val databaseFile = File(tempDir, DATABASE_FILE)
                exportDatabase(database, databaseFile, progressCallback)

                // Paso 3: Copiar fotografías
                progressCallback("Copiando fotografías...")
                val photosDir = File(tempDir, PHOTOS_DIR)
                photosDir.mkdirs()
                copyPhotos(context, database, photosDir, progressCallback)

                // Paso 4: Crear archivo ZIP
                progressCallback("Creando archivo de respaldo...")
                createZipFile(tempDir, outputFile)

                progressCallback("Respaldo completado")
                true

            } finally {
                // Siempre limpiar archivos temporales
                tempDir.deleteRecursively()
            }

        } catch (e: Exception) {
            Logger.e("BackupUtils", "Error durante la exportación", e)
            false
        }
    }

    /**
     * Crea el archivo de metadatos con información sobre el respaldo.
     *
     * Los metadatos incluyen:
     * - version: Versión del formato para compatibilidad
     * - timestamp: Cuándo se creó el respaldo
     * - appVersion: Versión de la app que creó el respaldo
     * - deviceInfo: Información básica del dispositivo
     */
    private fun createMetadataFile(file: File) {
        val metadata = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("timestamp", System.currentTimeMillis())
            put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            put("appVersion", "1.0") // Idealmente obtener de BuildConfig
            put("deviceInfo", JSONObject().apply {
                put("manufacturer", android.os.Build.MANUFACTURER)
                put("model", android.os.Build.MODEL)
                put("androidVersion", android.os.Build.VERSION.SDK_INT)
            })
        }

        file.writeText(metadata.toString(2)) // El 2 es para pretty-print con indentación
    }

    /**
     * Exporta toda la base de datos a un archivo JSON.
     *
     * La estructura del JSON será:
     * {
     *   "caex": [...],
     *   "categorias": [...],
     *   "preguntas": [...],
     *   "inspecciones": [...],
     *   "respuestas": [...],
     *   "fotos": [...]
     * }
     *
     * Cada tabla se exporta como un array de objetos JSON.
     */
    private suspend fun exportDatabase(
        database: AppDatabase,
        file: File,
        progressCallback: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val jsonDatabase = JSONObject()

        // Exportar tabla CAEX
        progressCallback("Exportando equipos CAEX...")
        val caexList = database.caexDao().getAllCAEX()
        val caexArray = JSONArray()
        caexList.first().forEach { caex: CAEX ->
            caexArray.put(caexToJson(caex))
        }
        jsonDatabase.put("caex", caexArray)

        // Exportar tabla Categorias
        progressCallback("Exportando categorías...")
        val categoriasList = database.categoriaDao().getAllCategorias()
        val categoriasArray = JSONArray()
        categoriasList.first().forEach { categoria: Categoria ->
            categoriasArray.put(categoriaToJson(categoria))
        }
        jsonDatabase.put("categorias", categoriasArray)

        // Exportar tabla Preguntas
        progressCallback("Exportando preguntas...")
        val preguntasList = database.preguntaDao().getAllPreguntas()
        val preguntasArray = JSONArray()
        preguntasList.first().forEach { pregunta: Pregunta ->
            preguntasArray.put(preguntaToJson(pregunta))
        }
        jsonDatabase.put("preguntas", preguntasArray)

        // Exportar tabla Inspecciones
        progressCallback("Exportando inspecciones...")
        val inspeccionesList = database.inspeccionDao().getAllInspecciones()
        val inspeccionesArray = JSONArray()
        inspeccionesList.first().forEach { inspeccion: Inspeccion ->
            inspeccionesArray.put(inspeccionToJson(inspeccion))
        }
        jsonDatabase.put("inspecciones", inspeccionesArray)

        // Para respuestas necesitamos obtener todas las inspecciones primero
        progressCallback("Exportando respuestas...")
        val respuestasArray = JSONArray()
        val inspecciones = inspeccionesList.first()

        inspecciones.forEach { inspeccion: Inspeccion ->
            val respuestas = database.respuestaDao().getRespuestasByInspeccion(inspeccion.inspeccionId)
            respuestas.first().forEach { respuesta: Respuesta ->
                respuestasArray.put(respuestaToJson(respuesta))
            }
        }
        jsonDatabase.put("respuestas", respuestasArray)

        // Exportar tabla Fotos
        progressCallback("Exportando referencias de fotos...")
        val fotosArray = JSONArray()
        inspecciones.forEach { inspeccion: Inspeccion ->
            val fotos = database.fotoDao().getFotosByInspeccion(inspeccion.inspeccionId)
            fotos.first().forEach { foto: Foto ->
                fotosArray.put(fotoToJson(foto))
            }
        }
        jsonDatabase.put("fotos", fotosArray)

        // Escribir el JSON al archivo
        file.writeText(jsonDatabase.toString(2))
    }

    /**
     * Copia todas las fotografías al directorio de respaldo.
     *
     * Este método:
     * 1. Obtiene todas las fotos de la base de datos
     * 2. Para cada foto, copia el archivo físico al directorio de respaldo
     * 3. Mantiene el nombre original del archivo para facilitar la restauración
     */
    private suspend fun copyPhotos(
        context: Context,
        database: AppDatabase,
        photosDir: File,
        progressCallback: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        // Obtener todas las inspecciones para luego obtener sus fotos
        val inspecciones = database.inspeccionDao().getAllInspecciones().first()

        var photoCount = 0
        var totalPhotos = 0

        // Primero contar el total de fotos para mostrar progreso
        inspecciones.forEach { inspeccion: Inspeccion ->
            val fotos = database.fotoDao().getFotosByInspeccion(inspeccion.inspeccionId).first()
            totalPhotos += fotos.size
        }

        // Ahora copiar las fotos
        inspecciones.forEach { inspeccion: Inspeccion ->
            val fotos = database.fotoDao().getFotosByInspeccion(inspeccion.inspeccionId).first()

            fotos.forEach { foto: Foto ->
                photoCount++
                progressCallback("Copiando foto $photoCount de $totalPhotos...")

                val sourceFile = File(foto.rutaArchivo)
                if (sourceFile.exists()) {
                    val fileName = sourceFile.name
                    val destFile = File(photosDir, fileName)

                    try {
                        sourceFile.copyTo(destFile, overwrite = true)
                    } catch (e: Exception) {
                        Logger.w("BackupUtils", "No se pudo copiar foto: ${foto.rutaArchivo}")
                    }
                }
            }
        }
    }

    /**
     * Crea el archivo ZIP final con todo el contenido del respaldo.
     *
     * Utiliza ZipOutputStream para comprimir eficientemente todos los archivos
     * y directorios en un único archivo ZIP.
     */
    private fun createZipFile(sourceDir: File, outputFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zipOut ->
            sourceDir.walkTopDown().filter { it.relativeTo(sourceDir).path.isNotEmpty() }
                .forEach { file ->
                // Calcular la ruta relativa
                val relativePath = file.relativeTo(sourceDir).path

                // Saltar el directorio raíz
                if (relativePath.isEmpty()) return@forEach

                // Convertir separadores de Windows a Unix
                val zipPath = relativePath.replace(File.separatorChar, '/')

                if (file.isDirectory) {
                    val dirEntry = if (zipPath.endsWith('/')) zipPath else "$zipPath/"
                    zipOut.putNextEntry(ZipEntry(dirEntry))
                    zipOut.closeEntry()
                } else {
                    zipOut.putNextEntry(ZipEntry(zipPath))
                    file.inputStream().use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            }
        }
    }

    /**
     * Importa datos desde un archivo ZIP de respaldo.
     *
     * El proceso es el inverso de la exportación:
     * 1. Validar el archivo ZIP
     * 2. Extraer y validar metadatos
     * 3. Verificar compatibilidad de versión
     * 4. Limpiar base de datos existente (si replaceAll es true)
     * 5. Importar todos los datos
     * 6. Restaurar fotografías
     *
     * @param context Contexto de la aplicación
     * @param database Instancia de la base de datos Room
     * @param zipUri URI del archivo ZIP a importar
     * @param replaceAll Si es true, reemplaza todos los datos. Si es false, combina con existentes
     * @param progressCallback Callback para reportar progreso
     * @return true si la importación fue exitosa, false en caso contrario
     */
    suspend fun importFromZip(
        context: Context,
        database: AppDatabase,
        zipUri: Uri,
        replaceAll: Boolean = true,
        progressCallback: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Crear directorio temporal para extraer
            val tempDir = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }

            try {
                logZipEntries(context, zipUri)
                // Paso 1: Extraer ZIP
                progressCallback("Extrayendo archivo de respaldo...")
                extractZipFile(context, zipUri, tempDir)

                // Paso 2: Validar metadatos
                progressCallback("Validando respaldo...")
                val metadataFile = File(tempDir, METADATA_FILE)
                if (!metadataFile.exists()) {
                    Logger.e("BackupUtils", "Archivo de metadatos no encontrado")
                    return@withContext false
                }

                val metadata = JSONObject(metadataFile.readText())
                val version = metadata.getInt("version")

                // Verificar compatibilidad de versión
                if (version > BACKUP_VERSION) {
                    Logger.e("BackupUtils", "Versión de respaldo no compatible: $version")
                    return@withContext false
                }

                // Paso 3: Importar base de datos
                progressCallback("Importando base de datos...")
                val databaseFile = File(tempDir, DATABASE_FILE)
                if (!databaseFile.exists()) {
                    Logger.e("BackupUtils", "Archivo de base de datos no encontrado")
                    return@withContext false
                }

                if (replaceAll) {
                    // Limpiar base de datos antes de importar
                    progressCallback("Limpiando datos existentes...")
                    database.clearAllTables()
                    importDatabaseReplace(database, databaseFile, progressCallback)
                } else {
                    // Combinar con datos existentes
                    importDatabase(database, databaseFile, progressCallback)
                }

                // Paso 4: Restaurar fotografías
                progressCallback("Restaurando fotografías...")
                val photosDir = File(tempDir, PHOTOS_DIR)
                if (photosDir.exists()) {
                    restorePhotos(context, photosDir, progressCallback)
                }

                progressCallback("Importación completada")
                true

            } finally {
                // Limpiar archivos temporales
                tempDir.deleteRecursively()
            }

        } catch (e: Exception) {
            Logger.e("BackupUtils", "Error durante la importación", e)
            false
        }
    }

    /**
     * Extrae el contenido del archivo ZIP al directorio temporal.
     */
    private fun extractZipFile(context: Context, zipUri: Uri, outputDir: File) {
        try {
            Logger.d("BackupUtils", "Iniciando extracción de ZIP desde URI: $zipUri")

            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                    var entry: ZipEntry? = try {
                        zipIn.nextEntry
                    } catch (e: Exception) {
                        Logger.w("BackupUtils", "Error en primera entrada, continuando...")
                        null
                    }

                    while (entry != null) {
                        try {
                            val entryName = entry.name.trim('/')

                            // Saltar entradas vacías o raíz
                            if (entryName.isEmpty()) {
                                Logger.w("BackupUtils", "Saltando entrada vacía: ${entry.name}")
                                zipIn.closeEntry()
                                entry = try {
                                    zipIn.nextEntry
                                } catch (e: Exception) {
                                    Logger.w("BackupUtils", "Error en entrada, continuando...")
                                    null
                                }
                                continue
                            }

                            val file = File(outputDir, entryName)
                            Logger.d("BackupUtils", "Procesando entrada: '$entryName' -> ${file.absolutePath}")

                            if (entry.isDirectory) {
                                file.mkdirs()
                            } else {
                                file.parentFile?.mkdirs()
                                file.outputStream().use { output ->
                                    zipIn.copyTo(output)
                                }
                            }
                        } catch (e: Exception) {
                            Logger.e("BackupUtils", "Error procesando entrada: ${entry?.name}", e)
                        } finally {
                            zipIn.closeEntry()
                            entry = try {
                                zipIn.nextEntry
                            } catch (e: Exception) {
                                Logger.w("BackupUtils", "Error obteniendo siguiente entrada")
                                null
                            }
                        }
                    }
                }
            } ?: throw IOException("No se pudo abrir el archivo ZIP")
        } catch (e: Exception) {
            Logger.e("BackupUtils", "Error al extraer ZIP: ${e.message}", e)
            throw e
        }
    }
    /**
     * Importa los datos desde el archivo JSON a la base de datos.
     *
     * IMPORTANTE: Este método NO limpia la base de datos existente,
     * permitiendo combinar datos de múltiples dispositivos.
     * Los IDs se regeneran para evitar conflictos.
     */
    private suspend fun importDatabase(
        database: AppDatabase,
        file: File,
        progressCallback: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val jsonDatabase = JSONObject(file.readText())

        // Mapeos para traducir IDs viejos a nuevos
        val caexIdMap = mutableMapOf<Long, Long>()
        val categoriaIdMap = mutableMapOf<Long, Long>()
        val preguntaIdMap = mutableMapOf<Long, Long>()
        val inspeccionIdMap = mutableMapOf<Long, Long>()
        val respuestaIdMap = mutableMapOf<Long, Long>()

        progressCallback("Importando datos...")

        // Importar CAEX - NO importar si ya existe el mismo número
        progressCallback("Importando equipos CAEX...")
        val caexArray = jsonDatabase.getJSONArray("caex")
        for (i in 0 until caexArray.length()) {
            val caexJson = caexArray.getJSONObject(i)
            val oldId = caexJson.getLong("caexId")
            val numeroIdentificador = caexJson.getInt("numeroIdentificador")

            // Verificar si ya existe este CAEX
            var existingCaex = database.caexDao().getCAEXByNumeroIdentificador(numeroIdentificador)
            if (existingCaex == null) {
                // Crear nuevo CAEX sin ID (Room lo generará)
                val newCaex = CAEX(
                    numeroIdentificador = numeroIdentificador,
                    modelo = caexJson.getString("modelo"),
                    fechaRegistro = caexJson.getLong("fechaRegistro")
                )
                val newId = database.caexDao().insertCAEX(newCaex)
                caexIdMap[oldId] = newId
            } else {
                // Usar el ID existente
                caexIdMap[oldId] = existingCaex.caexId
            }
        }
        // Importar Categorías - NO duplicar si ya existen
        progressCallback("Importando categorías...")
        val categoriasArray = jsonDatabase.getJSONArray("categorias")
        for (i in 0 until categoriasArray.length()) {
            val categoriaJson = categoriasArray.getJSONObject(i)
            val oldId = categoriaJson.getLong("categoriaId")
            val nombre = categoriaJson.getString("nombre")

            // Las categorías son fijas, usar las existentes
            val existingCategorias = database.categoriaDao().getAllCategorias().first()
            val existingCategoria = existingCategorias.find { it.nombre == nombre }

            if (existingCategoria != null) {
                categoriaIdMap[oldId] = existingCategoria.categoriaId
            }
        }

        // Importar Preguntas - NO duplicar si ya existen
        progressCallback("Importando preguntas...")
        val preguntasArray = jsonDatabase.getJSONArray("preguntas")
        for (i in 0 until preguntasArray.length()) {
            val preguntaJson = preguntasArray.getJSONObject(i)
            val oldId = preguntaJson.getLong("preguntaId")
            val texto = preguntaJson.getString("texto")
            val oldCategoriaId = preguntaJson.getLong("categoriaId")

            // Las preguntas son fijas, usar las existentes
            val newCategoriaId = categoriaIdMap[oldCategoriaId]
            if (newCategoriaId != null) {
                val existingPreguntas = database.preguntaDao().getPreguntasByCategoria(newCategoriaId).first()
                val existingPregunta = existingPreguntas.find { it.texto == texto }

                if (existingPregunta != null) {
                    preguntaIdMap[oldId] = existingPregunta.preguntaId
                }
            }
        }

        // Importar Inspecciones con nuevos IDs
        progressCallback("Importando inspecciones...")
        val inspeccionesArray = jsonDatabase.getJSONArray("inspecciones")
        for (i in 0 until inspeccionesArray.length()) {
            val inspeccionJson = inspeccionesArray.getJSONObject(i)
            val oldId = inspeccionJson.getLong("inspeccionId")
            val oldCaexId = inspeccionJson.getLong("caexId")

            val newCaexId = caexIdMap[oldCaexId]
            if (newCaexId != null) {
                // Verificar si ya existe una inspección similar
                val fechaCreacion = inspeccionJson.getLong("fechaCreacion")
                val tipo = inspeccionJson.getString("tipo")
                val nombreInspector = inspeccionJson.getString("nombreInspector")

                val existingInspecciones = database.inspeccionDao().getAllInspecciones().first()
                val existingInspeccion = existingInspecciones.find {
                    it.caexId == newCaexId &&
                            it.fechaCreacion == fechaCreacion &&
                            it.tipo == tipo &&
                            it.nombreInspector == nombreInspector
                }

                if (existingInspeccion != null) {
                    // Usar la inspección existente
                    inspeccionIdMap[oldId] = existingInspeccion.inspeccionId
                } else {
                    // Crear nueva inspección
                    val newInspeccion = Inspeccion(
                        caexId = newCaexId,
                        tipo = tipo,
                        estado = inspeccionJson.getString("estado"),
                        nombreInspector = nombreInspector,
                        nombreSupervisor = inspeccionJson.getString("nombreSupervisor"),
                        inspeccionRecepcionId = null,
                        fechaCreacion = fechaCreacion,
                        fechaFinalizacion = if (inspeccionJson.isNull("fechaFinalizacion")) null else inspeccionJson.getLong("fechaFinalizacion"),
                        comentariosGenerales = inspeccionJson.getString("comentariosGenerales"),
                        fechaTerminoEstimada = if (inspeccionJson.isNull("fechaTerminoEstimada")) null else inspeccionJson.getLong("fechaTerminoEstimada")
                    )
                    val newId = database.inspeccionDao().insertInspeccion(newInspeccion)
                    inspeccionIdMap[oldId] = newId
                }
            }
        }

        // Actualizar las referencias de inspeccionRecepcionId
        for (i in 0 until inspeccionesArray.length()) {
            val inspeccionJson = inspeccionesArray.getJSONObject(i)
            val oldId = inspeccionJson.getLong("inspeccionId")
            if (!inspeccionJson.isNull("inspeccionRecepcionId")) {
                val oldRecepcionId = inspeccionJson.getLong("inspeccionRecepcionId")
                val newId = inspeccionIdMap[oldId]
                val newRecepcionId = inspeccionIdMap[oldRecepcionId]

                if (newId != null && newRecepcionId != null) {
                    val inspeccion = database.inspeccionDao().getInspeccionById(newId)
                    if (inspeccion != null) {
                        val updated = inspeccion.copy(inspeccionRecepcionId = newRecepcionId)
                        database.inspeccionDao().updateInspeccion(updated)
                    }
                }
            }
        }
        // Importar Respuestas con nuevos IDs
        progressCallback("Importando respuestas...")
        val respuestasArray = jsonDatabase.getJSONArray("respuestas")
        for (i in 0 until respuestasArray.length()) {
            val respuestaJson = respuestasArray.getJSONObject(i)
            val oldId = respuestaJson.getLong("respuestaId")
            val oldInspeccionId = respuestaJson.getLong("inspeccionId")
            val oldPreguntaId = respuestaJson.getLong("preguntaId")

            val newInspeccionId = inspeccionIdMap[oldInspeccionId]
            val newPreguntaId = preguntaIdMap[oldPreguntaId]

            if (newInspeccionId != null && newPreguntaId != null) {
                // Verificar si ya existe una respuesta para esta pregunta en esta inspección
                val existingRespuesta = database.respuestaDao().getRespuestaPorInspeccionYPregunta(
                    newInspeccionId, newPreguntaId
                )

                if (existingRespuesta == null) {
                    val newRespuesta = Respuesta(
                        inspeccionId = newInspeccionId,
                        preguntaId = newPreguntaId,
                        estado = respuestaJson.getString("estado"),
                        comentarios = respuestaJson.getString("comentarios"),
                        tipoAccion = if (respuestaJson.isNull("tipoAccion")) null else respuestaJson.getString("tipoAccion"),
                        idAvisoOrdenTrabajo = if (respuestaJson.isNull("idAvisoOrdenTrabajo")) null else respuestaJson.getString("idAvisoOrdenTrabajo"),
                        fechaCreacion = respuestaJson.getLong("fechaCreacion"),
                        fechaModificacion = respuestaJson.getLong("fechaModificacion")
                    )
                    val newId = database.respuestaDao().insertRespuesta(newRespuesta)
                    respuestaIdMap[oldId] = newId
                } else {
                    respuestaIdMap[oldId] = existingRespuesta.respuestaId
                }
            }
        }

        // Importar Fotos con nuevos IDs
        progressCallback("Importando referencias de fotos...")
        val fotosArray = jsonDatabase.getJSONArray("fotos")
        for (i in 0 until fotosArray.length()) {
            val fotoJson = fotosArray.getJSONObject(i)
            val oldRespuestaId = fotoJson.getLong("respuestaId")

            val newRespuestaId = respuestaIdMap[oldRespuestaId]
            if (newRespuestaId != null) {
                val newFoto = Foto(
                    respuestaId = newRespuestaId,
                    rutaArchivo = fotoJson.getString("rutaArchivo"),
                    descripcion = fotoJson.getString("descripcion"),
                    fechaCreacion = fotoJson.getLong("fechaCreacion")
                )
                database.fotoDao().insertFoto(newFoto)
            }
        }
    }

    /**
     * Importa los datos reemplazando todo lo existente (comportamiento original).
     */
    private suspend fun importDatabaseReplace(
        database: AppDatabase,
        file: File,
        progressCallback: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val jsonDatabase = JSONObject(file.readText())

        // Importar CAEX
        progressCallback("Importando equipos CAEX...")
        val caexArray = jsonDatabase.getJSONArray("caex")
        for (i in 0 until caexArray.length()) {
            val caexJson = caexArray.getJSONObject(i)
            val caex = jsonToCAEX(caexJson)
            database.caexDao().insertCAEX(caex)
        }

        // Importar Categorías
        progressCallback("Importando categorías...")
        val categoriasArray = jsonDatabase.getJSONArray("categorias")
        for (i in 0 until categoriasArray.length()) {
            val categoriaJson = categoriasArray.getJSONObject(i)
            val categoria = jsonToCategoria(categoriaJson)
            database.categoriaDao().insertCategoria(categoria)
        }

        // Importar Preguntas
        progressCallback("Importando preguntas...")
        val preguntasArray = jsonDatabase.getJSONArray("preguntas")
        for (i in 0 until preguntasArray.length()) {
            val preguntaJson = preguntasArray.getJSONObject(i)
            val pregunta = jsonToPregunta(preguntaJson)
            database.preguntaDao().insertPregunta(pregunta)
        }

        // Importar Inspecciones
        progressCallback("Importando inspecciones...")
        val inspeccionesArray = jsonDatabase.getJSONArray("inspecciones")
        for (i in 0 until inspeccionesArray.length()) {
            val inspeccionJson = inspeccionesArray.getJSONObject(i)
            val inspeccion = jsonToInspeccion(inspeccionJson)
            database.inspeccionDao().insertInspeccion(inspeccion)
        }

        // Importar Respuestas
        progressCallback("Importando respuestas...")
        val respuestasArray = jsonDatabase.getJSONArray("respuestas")
        for (i in 0 until respuestasArray.length()) {
            val respuestaJson = respuestasArray.getJSONObject(i)
            val respuesta = jsonToRespuesta(respuestaJson)
            database.respuestaDao().insertRespuesta(respuesta)
        }

        // Importar Fotos
        progressCallback("Importando referencias de fotos...")
        val fotosArray = jsonDatabase.getJSONArray("fotos")
        for (i in 0 until fotosArray.length()) {
            val fotoJson = fotosArray.getJSONObject(i)
            val foto = jsonToFoto(fotoJson)
            database.fotoDao().insertFoto(foto)
        }
    }

    /**
     * Restaura las fotografías desde el directorio de respaldo al almacenamiento de la app.
     */
    private fun restorePhotos(
        context: Context,
        photosDir: File,
        progressCallback: (String) -> Unit
    ) {
        val appPhotosDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (appPhotosDir == null || !appPhotosDir.exists()) {
            appPhotosDir?.mkdirs()
        }

        val photos = photosDir.listFiles() ?: return
        var photoCount = 0
        val totalPhotos = photos.size

        photos.forEach { photoFile ->
            photoCount++
            progressCallback("Restaurando foto $photoCount de $totalPhotos...")

            val destFile = File(appPhotosDir, photoFile.name)
            try {
                photoFile.copyTo(destFile, overwrite = true)
            } catch (e: Exception) {
                Logger.w("BackupUtils", "No se pudo restaurar foto: ${photoFile.name}")
            }
        }
    }

    // ========== Funciones de conversión JSON ==========
    // Estas funciones convierten entre entidades de Room y objetos JSON

    private fun caexToJson(caex: CAEX): JSONObject = JSONObject().apply {
        put("caexId", caex.caexId)
        put("numeroIdentificador", caex.numeroIdentificador)
        put("modelo", caex.modelo)
        put("fechaRegistro", caex.fechaRegistro)
    }

    private fun jsonToCAEX(json: JSONObject): CAEX = CAEX(
        caexId = json.getLong("caexId"),
        numeroIdentificador = json.getInt("numeroIdentificador"),
        modelo = json.getString("modelo"),
        fechaRegistro = json.getLong("fechaRegistro")
    )

    private fun categoriaToJson(categoria: Categoria): JSONObject = JSONObject().apply {
        put("categoriaId", categoria.categoriaId)
        put("nombre", categoria.nombre)
        put("orden", categoria.orden)
        put("modeloAplicable", categoria.modeloAplicable)
        put("fechaCreacion", categoria.fechaCreacion)
    }

    private fun jsonToCategoria(json: JSONObject): Categoria = Categoria(
        categoriaId = json.getLong("categoriaId"),
        nombre = json.getString("nombre"),
        orden = json.getInt("orden"),
        modeloAplicable = json.getString("modeloAplicable"),
        fechaCreacion = json.getLong("fechaCreacion")
    )

    private fun preguntaToJson(pregunta: Pregunta): JSONObject = JSONObject().apply {
        put("preguntaId", pregunta.preguntaId)
        put("texto", pregunta.texto)
        put("categoriaId", pregunta.categoriaId)
        put("orden", pregunta.orden)
        put("modeloAplicable", pregunta.modeloAplicable)
        put("fechaCreacion", pregunta.fechaCreacion)
    }

    private fun jsonToPregunta(json: JSONObject): Pregunta = Pregunta(
        preguntaId = json.getLong("preguntaId"),
        texto = json.getString("texto"),
        categoriaId = json.getLong("categoriaId"),
        orden = json.getInt("orden"),
        modeloAplicable = json.getString("modeloAplicable"),
        fechaCreacion = json.getLong("fechaCreacion")
    )

    private fun inspeccionToJson(inspeccion: Inspeccion): JSONObject = JSONObject().apply {
        put("inspeccionId", inspeccion.inspeccionId)
        put("caexId", inspeccion.caexId)
        put("tipo", inspeccion.tipo)
        put("estado", inspeccion.estado)
        put("nombreInspector", inspeccion.nombreInspector)
        put("nombreSupervisor", inspeccion.nombreSupervisor)
        put("inspeccionRecepcionId", inspeccion.inspeccionRecepcionId ?: JSONObject.NULL)
        put("fechaCreacion", inspeccion.fechaCreacion)
        put("fechaFinalizacion", inspeccion.fechaFinalizacion ?: JSONObject.NULL)
        put("comentariosGenerales", inspeccion.comentariosGenerales)
        put("fechaTerminoEstimada", inspeccion.fechaTerminoEstimada ?: JSONObject.NULL)
    }

    private fun jsonToInspeccion(json: JSONObject): Inspeccion = Inspeccion(
        inspeccionId = json.getLong("inspeccionId"),
        caexId = json.getLong("caexId"),
        tipo = json.getString("tipo"),
        estado = json.getString("estado"),
        nombreInspector = json.getString("nombreInspector"),
        nombreSupervisor = json.getString("nombreSupervisor"),
        inspeccionRecepcionId = if (json.isNull("inspeccionRecepcionId")) null else json.getLong("inspeccionRecepcionId"),
        fechaCreacion = json.getLong("fechaCreacion"),
        fechaFinalizacion = if (json.isNull("fechaFinalizacion")) null else json.getLong("fechaFinalizacion"),
        comentariosGenerales = json.getString("comentariosGenerales"),
        fechaTerminoEstimada = if (json.isNull("fechaTerminoEstimada")) null else json.getLong("fechaTerminoEstimada")

    )

    private fun respuestaToJson(respuesta: Respuesta): JSONObject = JSONObject().apply {
        put("respuestaId", respuesta.respuestaId)
        put("inspeccionId", respuesta.inspeccionId)
        put("preguntaId", respuesta.preguntaId)
        put("estado", respuesta.estado)
        put("comentarios", respuesta.comentarios)
        put("tipoAccion", respuesta.tipoAccion ?: JSONObject.NULL)
        put("idAvisoOrdenTrabajo", respuesta.idAvisoOrdenTrabajo ?: JSONObject.NULL)
        put("fechaCreacion", respuesta.fechaCreacion)
        put("fechaModificacion", respuesta.fechaModificacion)
    }

    private fun jsonToRespuesta(json: JSONObject): Respuesta = Respuesta(
        respuestaId = json.getLong("respuestaId"),
        inspeccionId = json.getLong("inspeccionId"),
        preguntaId = json.getLong("preguntaId"),
        estado = json.getString("estado"),
        comentarios = json.getString("comentarios"),
        tipoAccion = if (json.isNull("tipoAccion")) null else json.getString("tipoAccion"),
        idAvisoOrdenTrabajo = if (json.isNull("idAvisoOrdenTrabajo")) null else json.getString("idAvisoOrdenTrabajo"),
        fechaCreacion = json.getLong("fechaCreacion"),
        fechaModificacion = json.getLong("fechaModificacion")
    )

    private fun fotoToJson(foto: Foto): JSONObject = JSONObject().apply {
        put("fotoId", foto.fotoId)
        put("respuestaId", foto.respuestaId)
        put("rutaArchivo", foto.rutaArchivo)
        put("descripcion", foto.descripcion)
        put("fechaCreacion", foto.fechaCreacion)
    }

    private fun jsonToFoto(json: JSONObject): Foto = Foto(
        fotoId = json.getLong("fotoId"),
        respuestaId = json.getLong("respuestaId"),
        rutaArchivo = json.getString("rutaArchivo"),
        descripcion = json.getString("descripcion"),
        fechaCreacion = json.getLong("fechaCreacion")
    )
    private fun logZipEntries(context: Context, zipUri: Uri) {
        try {
            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                val zipIn = ZipInputStream(BufferedInputStream(inputStream))
                var entry: ZipEntry? = zipIn.nextEntry

                while (entry != null) {
                    Logger.d("ZipDebug", "Entry: ${entry.name} | Size: ${entry.size} | Directory: ${entry.isDirectory}")
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        } catch (e: Exception) {
            Logger.e("ZipDebug", "Error al leer entradas ZIP", e)
        }
    }
}