package com.caextech.inspector.utils

import com.caextech.inspector.data.entities.Inspeccion
import java.util.concurrent.TimeUnit

/**
 * Utilidades para calcular métricas de inspecciones.
 */
object InspectionAnalytics {

    /**
     * Calcula la duración real de una inspección en milisegundos.
     *
     * @param inspeccion La inspección para calcular la duración
     * @return Duración en milisegundos, o null si la inspección no está cerrada
     */
    fun calcularDuracionReal(inspeccion: Inspeccion): Long? {
        return if (inspeccion.fechaFinalizacion != null) {
            inspeccion.fechaFinalizacion - inspeccion.fechaCreacion
        } else {
            null
        }
    }

    /**
     * Calcula la desviación respecto a la fecha estimada de término.
     * Un valor positivo indica que se terminó después de la fecha estimada (retraso).
     * Un valor negativo indica que se terminó antes de la fecha estimada (adelanto).
     *
     * @param inspeccion La inspección para calcular la desviación
     * @return Desviación en milisegundos, o null si no aplica el cálculo
     */
    fun calcularDesviacionEstimada(inspeccion: Inspeccion): Long? {
        return if (inspeccion.fechaFinalizacion != null && inspeccion.fechaTerminoEstimada != null) {
            inspeccion.fechaFinalizacion - inspeccion.fechaTerminoEstimada
        } else {
            null
        }
    }

    /**
     * Formatea una duración en milisegundos a un string legible.
     *
     * @param durationMs Duración en milisegundos
     * @return String formateado (ej: "2 días, 3 horas, 45 minutos")
     */
    fun formatearDuracion(durationMs: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(durationMs)
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs) - TimeUnit.DAYS.toHours(days)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(durationMs))

        val parts = mutableListOf<String>()

        if (days > 0) parts.add("$days día${if (days != 1L) "s" else ""}")
        if (hours > 0) parts.add("$hours hora${if (hours != 1L) "s" else ""}")
        if (minutes > 0) parts.add("$minutes minuto${if (minutes != 1L) "s" else ""}")

        return if (parts.isEmpty()) "0 minutos" else parts.joinToString(", ")
    }

    /**
     * Formatea una desviación con signo apropiado.
     *
     * @param desviacionMs Desviación en milisegundos
     * @return String formateado con + o - según corresponda
     */
    fun formatearDesviacion(desviacionMs: Long): String {
        val signo = if (desviacionMs >= 0) "+" else "-"
        val duracionFormateada = formatearDuracion(kotlin.math.abs(desviacionMs))
        return "$signo$duracionFormateada"
    }

    /**
     * Obtiene métricas completas de una inspección para exportación JSON.
     *
     * @param inspeccion La inspección para analizar
     * @return Map con las métricas calculadas
     */
    fun obtenerMetricasParaExportacion(inspeccion: Inspeccion): Map<String, Any?> {
        val duracionReal = calcularDuracionReal(inspeccion)
        val desviacion = calcularDesviacionEstimada(inspeccion)

        return mapOf(
            "duracionRealMs" to duracionReal,
            "duracionRealFormateada" to duracionReal?.let { formatearDuracion(it) },
            "desviacionEstimadaMs" to desviacion,
            "desviacionEstimadaFormateada" to desviacion?.let { formatearDesviacion(it) },
            "terminoEnFecha" to (desviacion?.let { kotlin.math.abs(it) <= TimeUnit.HOURS.toMillis(1) }), // Dentro de 1 hora
            "estadoTiempo" to when {
                desviacion == null -> "N/A"
                desviacion > TimeUnit.HOURS.toMillis(1) -> "Retrasado"
                desviacion < -TimeUnit.HOURS.toMillis(1) -> "Adelantado"
                else -> "A tiempo"
            }
        )
    }
    /**
     * Calcula métricas para inspección de entrega usando datos de recepción.
     */
    fun obtenerMetricasEntregaParaExportacion(
        inspeccionEntrega: Inspeccion,
        inspeccionRecepcion: Inspeccion
    ): Map<String, Any?> {
        val duracionReal = if (inspeccionEntrega.fechaFinalizacion != null) {
            inspeccionEntrega.fechaFinalizacion - inspeccionRecepcion.fechaCreacion
        } else null

        val desviacion = if (inspeccionEntrega.fechaFinalizacion != null && inspeccionRecepcion.fechaTerminoEstimada != null) {
            inspeccionEntrega.fechaFinalizacion - inspeccionRecepcion.fechaTerminoEstimada
        } else null

        return mapOf(
            "duracionRealMs" to duracionReal,
            "duracionRealFormateada" to duracionReal?.let { formatearDuracion(it) },
            "desviacionEstimadaMs" to desviacion,
            "desviacionEstimadaFormateada" to desviacion?.let { formatearDesviacion(it) },
            "terminoEnFecha" to (desviacion?.let { kotlin.math.abs(it) <= TimeUnit.HOURS.toMillis(1) }),
            "estadoTiempo" to when {
                desviacion == null -> "N/A"
                desviacion > TimeUnit.HOURS.toMillis(1) -> "Retrasado"
                desviacion < -TimeUnit.HOURS.toMillis(1) -> "Adelantado"
                else -> "A tiempo"
            }
        )
    }
}