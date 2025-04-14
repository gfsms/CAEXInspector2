package com.caextech.inspector.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.caextech.inspector.data.dao.FotoDao
import com.caextech.inspector.data.entities.Foto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repositorio para operaciones relacionadas con las fotos de evidencia.
 *
 * Esta clase proporciona métodos para acceder y manipular datos de fotos,
 * sirviendo como una capa de abstracción entre la base de datos y la UI.
 * También maneja operaciones del sistema de archivos para almacenar imágenes.
 */
class FotoRepository(
    private val fotoDao: FotoDao,
    private val appContext: Context
) {

    /**
     * Obtiene todas las fotos para una respuesta específica.
     *
     * @param respuestaId ID de la respuesta
     * @return Flow de lista de fotos
     */
    fun getFotosByRespuesta(respuestaId: Long): Flow<List<Foto>> {
        return fotoDao.getFotosByRespuesta(respuestaId)
    }

    /**
     * Obtiene todas las fotos para una inspección específica.
     *
     * @param inspeccionId ID de la inspección
     * @return Flow de lista de fotos
     */
    fun getFotosByInspeccion(inspeccionId: Long): Flow<List<Foto>> {
        return fotoDao.getFotosByInspeccion(inspeccionId)
    }

    /**
     * Crea un archivo temporal para almacenar una foto de la cámara.
     *
     * @return Uri del archivo creado y la ruta del archivo como un Par
     * @throws IOException si hay un error al crear el archivo
     */
    fun crearArchivoTemporalParaFoto(): Pair<Uri, String> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        val storageDir = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        val authority = "${appContext.packageName}.fileprovider"
        val imageUri = FileProvider.getUriForFile(
            appContext,
            authority,
            imageFile
        )

        return Pair(imageUri, imageFile.absolutePath)
    }

    /**
     * Guarda una foto asociada a una respuesta en la base de datos.
     *
     * @param respuestaId ID de la respuesta a la que pertenece esta foto
     * @param rutaArchivo Ruta del archivo de imagen en el almacenamiento
     * @param descripcion Descripción opcional de la foto
     * @return ID de la foto guardada
     */
    suspend fun guardarFoto(
        respuestaId: Long,
        rutaArchivo: String,
        descripcion: String = ""
    ): Long {
        val foto = Foto(
            respuestaId = respuestaId,
            rutaArchivo = rutaArchivo,
            descripcion = descripcion
        )

        return fotoDao.insertFoto(foto)
    }

    /**
     * Elimina una foto de la base de datos y del almacenamiento.
     *
     * @param foto La foto a eliminar
     */
    suspend fun deleteFoto(foto: Foto) {
        // Primero eliminar el archivo físico
        val file = File(foto.rutaArchivo)
        if (file.exists()) {
            file.delete()
        }

        // Luego eliminar el registro de la base de datos
        fotoDao.deleteFoto(foto)
    }

    /**
     * Obtiene una foto específica por su ID.
     *
     * @param fotoId ID de la foto
     * @return La foto o null si no existe
     */
    suspend fun getFotoById(fotoId: Long): Foto? {
        return fotoDao.getFotoById(fotoId)
    }

    /**
     * Cuenta el número de fotos para una respuesta.
     *
     * @param respuestaId ID de la respuesta
     * @return Número de fotos
     */
    suspend fun countFotosByRespuesta(respuestaId: Long): Int {
        return fotoDao.countFotosByRespuesta(respuestaId)
    }

    /**
     * Cuenta el número de fotos para una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @return Número de fotos
     */
    suspend fun countFotosByInspeccion(inspeccionId: Long): Int {
        return fotoDao.countFotosByInspeccion(inspeccionId)
    }

    /**
     * Copia un archivo de imagen desde una URI temporal a un archivo permanente.
     *
     * @param uri URI de la imagen temporal
     * @return Ruta del archivo permanente o null si hay un error
     */
    fun copiarImagenDesdeTemporal(uri: Uri): String? {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "INSPECCION_${timeStamp}.jpg"

            // Directorio para almacenar las imágenes permanentemente
            val storageDir = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imagenFinal = File(storageDir, imageFileName)

            // Abrir streams y copiar la imagen
            appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(imagenFinal).use { outputStream ->
                    val buffer = ByteArray(4 * 1024) // buffer de 4K
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
            }

            return imagenFinal.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Elimina todas las fotos asociadas a una respuesta.
     *
     * @param respuestaId ID de la respuesta
     */
    suspend fun deleteFotosByRespuesta(respuestaId: Long) {
        // Obtener todas las fotos para eliminar los archivos físicos
        val fotosFlow = fotoDao.getFotosByRespuesta(respuestaId)
        val fotos = fotosFlow.firstOrNull() ?: emptyList()

        for (foto in fotos) {
            val file = File(foto.rutaArchivo)
            if (file.exists()) {
                file.delete()
            }
        }

        // Eliminar los registros de la base de datos
        fotoDao.deleteFotosByRespuesta(respuestaId)
    }

    /**
     * Elimina todas las fotos asociadas a una inspección.
     *
     * @param inspeccionId ID de la inspección
     */
    suspend fun deleteFotosByInspeccion(inspeccionId: Long) {
        // Obtener todas las fotos para eliminar los archivos físicos
        val fotosFlow = fotoDao.getFotosByInspeccion(inspeccionId)
        val fotos = fotosFlow.firstOrNull() ?: emptyList()

        for (foto in fotos) {
            val file = File(foto.rutaArchivo)
            if (file.exists()) {
                file.delete()
            }
        }

        // Eliminar los registros de la base de datos
        fotoDao.deleteFotosByInspeccion(inspeccionId)
    }
}