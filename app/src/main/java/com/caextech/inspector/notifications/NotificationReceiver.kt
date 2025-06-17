package com.caextech.inspector.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.caextech.inspector.utils.Logger

/**
 * BroadcastReceiver que recibe las alarmas programadas y encola el trabajo de notificación.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Logger.d("NotificationReceiver", "Alarma recibida: ${intent.action}")

        when (intent.action) {
            ACTION_INSPECTION_ALARM -> {
                val inspeccionId = intent.getLongExtra(EXTRA_INSPECCION_ID, -1L)

                if (inspeccionId != -1L) {
                    Logger.d("NotificationReceiver", "Procesando alarma para inspección $inspeccionId")
                    enqueueNotificationWork(context, inspeccionId)
                } else {
                    Logger.w("NotificationReceiver", "ID de inspección inválido en la alarma")
                }
            }
            else -> {
                Logger.w("NotificationReceiver", "Acción de alarma desconocida: ${intent.action}")
            }
        }
    }

    /**
     * Encola un trabajo de WorkManager para mostrar la notificación.
     */
    private fun enqueueNotificationWork(context: Context, inspeccionId: Long) {
        try {
            val inputData = Data.Builder()
                .putLong(NotificationWorker.KEY_INSPECCION_ID, inspeccionId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(inputData)
                .addTag("inspection_notification_$inspeccionId")
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)

            Logger.d("NotificationReceiver", "Trabajo de notificación encolado para inspección $inspeccionId")
        } catch (e: Exception) {
            Logger.e("NotificationReceiver", "Error al encolar trabajo de notificación", e)
        }
    }

    companion object {
        const val ACTION_INSPECTION_ALARM = "com.caextech.inspector.INSPECTION_ALARM"
        const val EXTRA_INSPECCION_ID = "extra_inspeccion_id"
    }
}