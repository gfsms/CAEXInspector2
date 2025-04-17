package com.caextech.inspector.utils

import android.util.Log

/**
 * Clase utilitaria para registro de logs en la aplicación.
 * Proporciona una interfaz unificada y consistente para los registros.
 */
object Logger {
    // Flag para habilitar/deshabilitar el logging (se puede cambiar para producción)
    private const val LOGGING_ENABLED = true

    // Prefijo base para todos los logs de esta aplicación
    private const val BASE_TAG = "CAEXInspector"

    /**
     * Registra un mensaje de nivel debug.
     *
     * @param tag Etiqueta específica del componente
     * @param message Mensaje a registrar
     */
    fun d(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.d("$BASE_TAG:$tag", message)
        }
    }

    /**
     * Registra un mensaje de nivel info.
     *
     * @param tag Etiqueta específica del componente
     * @param message Mensaje a registrar
     */
    fun i(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.i("$BASE_TAG:$tag", message)
        }
    }

    /**
     * Registra un mensaje de nivel warning.
     *
     * @param tag Etiqueta específica del componente
     * @param message Mensaje a registrar
     */
    fun w(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.w("$BASE_TAG:$tag", message)
        }
    }

    /**
     * Registra un mensaje de nivel error.
     *
     * @param tag Etiqueta específica del componente
     * @param message Mensaje a registrar
     */
    fun e(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.e("$BASE_TAG:$tag", message)
        }
    }

    /**
     * Registra un mensaje de nivel error con una excepción.
     *
     * @param tag Etiqueta específica del componente
     * @param message Mensaje a registrar
     * @param throwable Excepción a registrar
     */
    fun e(tag: String, message: String, throwable: Throwable) {
        if (LOGGING_ENABLED) {
            Log.e("$BASE_TAG:$tag", message, throwable)
        }
    }

    /**
     * Registra un mensaje de nivel verbose.
     *
     * @param tag Etiqueta específica del componente
     * @param message Mensaje a registrar
     */
    fun v(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.v("$BASE_TAG:$tag", message)
        }
    }

    /**
     * Registra el estado de un objeto para depuración.
     *
     * @param tag Etiqueta específica del componente
     * @param label Etiqueta descriptiva para el objeto
     * @param value Valor a registrar
     */
    fun state(tag: String, label: String, value: Any?) {
        if (LOGGING_ENABLED) {
            Log.d("$BASE_TAG:$tag", "STATE → $label: $value")
        }
    }
}