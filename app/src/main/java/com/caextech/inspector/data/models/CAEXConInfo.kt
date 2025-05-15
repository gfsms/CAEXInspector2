package com.caextech.inspector.data.models

import com.caextech.inspector.data.entities.CAEX

/**
 * Modelo de datos enriquecido para mostrar CAEX con información de inspecciones
 */
data class CAEXConInfo(
    val caex: CAEX,
    val totalInspecciones: Int = 0,
    val fechaUltimaInspeccion: Long? = null,
    val tipoUltimaInspeccion: String? = null,
    val estadoUltimaInspeccion: String? = null,
    val tieneInspeccionPendiente: Boolean = false
) {
    /**
     * Determina el estado actual del equipo
     */
    fun getEstadoEquipo(): EstadoEquipo {
        return when {
            tieneInspeccionPendiente -> EstadoEquipo.CON_INSPECCION_PENDIENTE
            estadoUltimaInspeccion == "PENDIENTE_CIERRE" -> EstadoEquipo.PENDIENTE_ENTREGA
            tipoUltimaInspeccion == "ENTREGA" -> EstadoEquipo.OPERATIVO
            tipoUltimaInspeccion == "RECEPCION" -> EstadoEquipo.EN_MANTENIMIENTO
            totalInspecciones == 0 -> EstadoEquipo.SIN_INSPECCIONES
            else -> EstadoEquipo.DESCONOCIDO
        }
    }

    /**
     * Obtiene la descripción del estado para mostrar en la UI
     */
    fun getEstadoDescripcion(): String {
        return when (getEstadoEquipo()) {
            EstadoEquipo.OPERATIVO -> "Operativo"
            EstadoEquipo.EN_MANTENIMIENTO -> "En Mantenimiento"
            EstadoEquipo.PENDIENTE_ENTREGA -> "Pendiente Entrega"
            EstadoEquipo.CON_INSPECCION_PENDIENTE -> "Inspección Abierta"
            EstadoEquipo.SIN_INSPECCIONES -> "Sin Inspecciones"
            EstadoEquipo.DESCONOCIDO -> "Estado Desconocido"
        }
    }

    /**
     * Obtiene el color del indicador de estado
     */
    fun getEstadoColor(): Int {
        return when (getEstadoEquipo()) {
            EstadoEquipo.OPERATIVO -> com.caextech.inspector.R.color.status_conforme
            EstadoEquipo.EN_MANTENIMIENTO -> com.caextech.inspector.R.color.status_no_conforme
            EstadoEquipo.PENDIENTE_ENTREGA -> com.caextech.inspector.R.color.status_pendiente_cierre
            EstadoEquipo.CON_INSPECCION_PENDIENTE -> com.caextech.inspector.R.color.status_pending
            EstadoEquipo.SIN_INSPECCIONES -> android.R.color.darker_gray
            EstadoEquipo.DESCONOCIDO -> android.R.color.darker_gray
        }
    }

    /**
     * Obtiene el ícono del indicador de estado
     */
    fun getEstadoIcono(): Int {
        return when (getEstadoEquipo()) {
            EstadoEquipo.OPERATIVO -> android.R.drawable.ic_menu_agenda
            EstadoEquipo.EN_MANTENIMIENTO -> android.R.drawable.ic_dialog_alert
            EstadoEquipo.PENDIENTE_ENTREGA -> android.R.drawable.ic_menu_upload
            EstadoEquipo.CON_INSPECCION_PENDIENTE -> android.R.drawable.ic_menu_edit
            EstadoEquipo.SIN_INSPECCIONES -> android.R.drawable.ic_menu_info_details
            EstadoEquipo.DESCONOCIDO -> android.R.drawable.ic_dialog_alert
        }
    }
}

/**
 * Enum para representar el estado del equipo
 */
enum class EstadoEquipo {
    OPERATIVO,              // Última inspección fue entrega
    EN_MANTENIMIENTO,       // Última inspección fue recepción
    PENDIENTE_ENTREGA,      // Inspección de recepción pendiente de cierre
    CON_INSPECCION_PENDIENTE, // Tiene inspección abierta actual
    SIN_INSPECCIONES,       // No tiene inspecciones
    DESCONOCIDO             // Estado no determinable
}