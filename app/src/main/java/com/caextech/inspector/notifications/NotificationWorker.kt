package com.caextech.inspector.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.R
import com.caextech.inspector.data.entities.Inspeccion
import com.caextech.inspector.ui.inspection.InspectionDetailActivity
import com.caextech.inspector.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker que maneja la creación y muestra de notificaciones para inspecciones.
 */
class NotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val inspeccionId = inputData.getLong(KEY_INSPECCION_ID, -1L)

            if (inspeccionId == -1L) {
                Logger.w("NotificationWorker", "ID de inspección inválido")
                return@withContext Result.failure()
            }

            Logger.d("NotificationWorker", "Procesando notificación para inspección $inspeccionId")

            // Obtener detalles de la inspección desde la base de datos
            val app = context.applicationContext as CAEXInspectorApp
            val inspeccionCompleta = app.inspeccionRepository.getInspeccionConCAEXById(inspeccionId)

            if (inspeccionCompleta == null) {
                Logger.w("NotificationWorker", "Inspección $inspeccionId no encontrada")
                return@withContext Result.failure()
            }

            val inspeccion = inspeccionCompleta.inspeccion
            val caex = inspeccionCompleta.caex

            // No mostrar notificación si la inspección ya está cerrada
            if (inspeccion.estado == Inspeccion.ESTADO_CERRADA) {
                Logger.d("NotificationWorker", "Inspección $inspeccionId ya está cerrada, no se muestra notificación")
                return@withContext Result.success()
            }

            // Crear y mostrar la notificación
            showInspectionNotification(inspeccion, caex.getNombreCompleto())

            Logger.d("NotificationWorker", "Notificación mostrada para inspección $inspeccionId")
            Result.success()

        } catch (e: Exception) {
            Logger.e("NotificationWorker", "Error al procesar notificación", e)
            Result.failure()
        }
    }

    /**
     * Crea y muestra la notificación de inspección.
     */
    private fun showInspectionNotification(inspeccion: Inspeccion, caexNombre: String) {
        createNotificationChannel()

        // Intent para abrir InspectionDetailActivity al hacer clic en la notificación
        val intent = Intent(context, InspectionDetailActivity::class.java).apply {
            putExtra(InspectionDetailActivity.EXTRA_INSPECCION_ID, inspeccion.inspeccionId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            inspeccion.inspeccionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir la notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_inspection) // Crear este ícono o usar uno existente
            .setContentTitle("Inspección debe finalizar")
            .setContentText("La inspección de $caexNombre debe finalizar ahora")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("La inspección ${inspeccion.tipo.lowercase()} del equipo $caexNombre (ID: ${inspeccion.inspeccionId}) ha alcanzado su fecha estimada de término.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_add, // Crear este ícono o usar uno existente
                "Ver Inspección",
                pendingIntent
            )
            .build()

        // Mostrar la notificación
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(inspeccion.inspeccionId.toInt(), notification)
    }

    /**
     * Crea el canal de notificación para Android 8.0+.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificaciones de Inspecciones"
            val descriptionText = "Notificaciones cuando las inspecciones deben finalizar"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val KEY_INSPECCION_ID = "inspeccion_id"
        private const val CHANNEL_ID = "inspection_notifications"
    }
}