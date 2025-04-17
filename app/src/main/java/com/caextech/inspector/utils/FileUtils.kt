package com.caextech.inspector.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilidades para manejo de archivos en la aplicación.
 */
object FileUtils {
    private const val TAG = "FileUtils"

    /**
     * Crea un archivo temporal para almacenar una foto capturada con la cámara.
     *
     * @param context El contexto de la aplicación
     * @return Un par con la Uri pública del archivo y la ruta del archivo
     * @throws IOException si hay un error al crear el archivo
     */
    fun createTempImageFile(context: Context): Pair<Uri, String> {
        try {
            // Crear un nombre de archivo único basado en la marca de tiempo
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"

            // Crear el archivo en el directorio de imágenes de la aplicación
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: throw IOException("No se pudo acceder al directorio de almacenamiento externo")

            // Asegurar que el directorio existe
            if (!storageDir.exists()) {
                if (!storageDir.mkdirs()) {
                    throw IOException("No se pudo crear el directorio de almacenamiento")
                }
            }

            val imageFile = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            )

            // Registrar la ruta para depuración
            Logger.d(TAG, "Archivo temporal creado en: ${imageFile.absolutePath}")

            // Convertir la ruta del archivo en una URI que la cámara pueda usar
            val authority = "${context.packageName}.fileprovider"
            val imageUri = FileProvider.getUriForFile(
                context,
                authority,
                imageFile
            )

            // Registrar la URI para depuración
            Logger.d(TAG, "URI generada para el archivo: $imageUri")

            return Pair(imageUri, imageFile.absolutePath)
        } catch (e: Exception) {
            Logger.e(TAG, "Error al crear archivo temporal: ${e.message}", e)
            throw e
        }
    }

    /**
     * Copia una imagen desde una URI a un archivo permanente en el almacenamiento de la aplicación.
     *
     * @param context El contexto de la aplicación
     * @param sourceUri La URI de la imagen a copiar
     * @return La ruta del archivo destino, o null si hubo un error
     */
    fun copyImageToAppStorage(context: Context, sourceUri: Uri): String? {
        try {
            // Crear un nombre de archivo único
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "EVIDENCIA_${timeStamp}.jpg"

            // Obtener el directorio de imágenes
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: return null

            // Asegurar que el directorio existe
            if (!storageDir.exists() && !storageDir.mkdirs()) {
                Logger.e(TAG, "No se pudo crear el directorio de imágenes")
                return null
            }

            val destinationFile = File(storageDir, imageFileName)
            Logger.d(TAG, "Copiando imagen a: ${destinationFile.absolutePath}")

            // Abrir streams y copiar la imagen
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        val buffer = ByteArray(4 * 1024) // buffer de 4K
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                        outputStream.flush()
                    }
                } ?: run {
                    Logger.e(TAG, "No se pudo abrir la URI de origen")
                    return null
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error al copiar archivo: ${e.message}", e)
                // Si falla, eliminar el archivo de destino parcial
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }
                return null
            }

            Logger.d(TAG, "Imagen copiada exitosamente")
            return destinationFile.absolutePath
        } catch (e: Exception) {
            Logger.e(TAG, "Error al copiar imagen: ${e.message}", e)
            return null
        }
    }

    /**
     * Crea un directorio para almacenar los archivos PDF generados.
     *
     * @param context El contexto de la aplicación
     * @return El directorio creado
     */
    fun getOrCreatePdfDirectory(context: Context): File {
        val pdfDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "pdfs")
        if (!pdfDir.exists()) {
            if (!pdfDir.mkdirs()) {
                Logger.w(TAG, "No se pudo crear el directorio PDF")
            }
        }
        return pdfDir
    }

    /**
     * Crea un archivo para un nuevo PDF.
     *
     * @param context El contexto de la aplicación
     * @param fileName Nombre del archivo (sin extensión)
     * @return El archivo creado
     */
    fun createPdfFile(context: Context, fileName: String): File {
        val pdfDir = getOrCreatePdfDirectory(context)
        return File(pdfDir, "$fileName.pdf")
    }

    /**
     * Obtiene una URI compartible para un archivo.
     *
     * @param context El contexto de la aplicación
     * @param file El archivo para el que se requiere la URI
     * @return La URI del archivo que puede compartirse con otras aplicaciones
     */
    fun getUriForFile(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Limpia archivos temporales antiguos para evitar acumulación
     *
     * @param context El contexto de la aplicación
     * @param maxAgeInMillis Edad máxima de los archivos en milisegundos (por defecto 24 horas)
     */
    fun cleanupTempFiles(context: Context, maxAgeInMillis: Long = 24 * 60 * 60 * 1000) {
        try {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir != null && storageDir.exists()) {
                val now = System.currentTimeMillis()
                val tempFiles = storageDir.listFiles { file ->
                    file.isFile && file.name.startsWith("JPEG_") &&
                            (now - file.lastModified() > maxAgeInMillis)
                }

                tempFiles?.forEach { file ->
                    if (file.delete()) {
                        Logger.d(TAG, "Archivo temporal eliminado: ${file.name}")
                    } else {
                        Logger.w(TAG, "No se pudo eliminar archivo temporal: ${file.name}")
                    }
                }

                Logger.d(TAG, "Limpieza de archivos temporales completada. ${tempFiles?.size ?: 0} archivos eliminados")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error al limpiar archivos temporales: ${e.message}", e)
        }
    }
}