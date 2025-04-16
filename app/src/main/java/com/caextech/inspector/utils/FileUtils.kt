package com.caextech.inspector.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilidades para manejo de archivos en la aplicación.
 */
object FileUtils {

    /**
     * Crea un archivo temporal para almacenar una foto capturada con la cámara.
     *
     * @param context El contexto de la aplicación
     * @return Un par con la Uri pública del archivo y la ruta del archivo
     */
    fun createTempImageFile(context: Context): Pair<Uri, String> {
        // Crear un nombre de archivo único basado en la marca de tiempo
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        // Crear el archivo en el directorio de imágenes de la aplicación
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        // Convertir la ruta del archivo en una URI que la cámara pueda usar
        val authority = "${context.packageName}.fileprovider"
        val imageUri = FileProvider.getUriForFile(
            context,
            authority,
            imageFile
        )

        return Pair(imageUri, imageFile.absolutePath)
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
            val destinationFile = File(storageDir, imageFileName)

            // Abrir streams y copiar la imagen
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    val buffer = ByteArray(4 * 1024) // buffer de 4K
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
            }

            return destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
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
            pdfDir.mkdirs()
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
}