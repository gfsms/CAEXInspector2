package com.caextech.inspector.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.caextech.inspector.utils.Logger

/**
 * Clase para programar y cancelar alarmas de inspecciones.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Programa una alarma para notificar cuando una inspección debe terminar.
     *
     * @param inspeccionId ID de la inspección
     * @param fechaTermino Fecha y hora en que debe dispararse la alarma
     */
    fun programarAlarmaInspeccion(inspeccionId: Long, fechaTermino: Long) {
        try {
            // Verificar permisos para Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Logger.w("AlarmScheduler", "No se puede programar alarmas exactas. Permisos insuficientes.")
                // Opcionalmente, dirigir al usuario a configuración
                // val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                // context.startActivity(intent)
                return
            }

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_INSPECTION_ALARM
                putExtra(NotificationReceiver.EXTRA_INSPECCION_ID, inspeccionId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                inspeccionId.toInt(), // Usar el ID de inspección como request code único
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Programar la alarma exacta
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    fechaTermino,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    fechaTermino,
                    pendingIntent
                )
            }

            Logger.d("AlarmScheduler", "Alarma programada para inspección $inspeccionId en $fechaTermino")
        } catch (e: Exception) {
            Logger.e("AlarmScheduler", "Error al programar alarma para inspección $inspeccionId", e)
        }
    }

    /**
     * Cancela la alarma programada para una inspección.
     *
     * @param inspeccionId ID de la inspección
     */
    fun cancelarAlarmaInspeccion(inspeccionId: Long) {
        try {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_INSPECTION_ALARM
                putExtra(NotificationReceiver.EXTRA_INSPECCION_ID, inspeccionId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                inspeccionId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()

            Logger.d("AlarmScheduler", "Alarma cancelada para inspección $inspeccionId")
        } catch (e: Exception) {
            Logger.e("AlarmScheduler", "Error al cancelar alarma para inspección $inspeccionId", e)
        }
    }

    /**
     * Verifica si se pueden programar alarmas exactas en Android 12+.
     */
    fun puedesProgramarAlarmasExactas(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}