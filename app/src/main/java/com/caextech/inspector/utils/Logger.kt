package com.caextech.inspector.utils

import android.util.Log

/**
 * Utility class for logging in the application.
 * Provides consistent logging with standardized tag format.
 */
object Logger {

    // Flag to enable/disable logging (can be changed for production)
    private const val LOGGING_ENABLED = true

    // Base tag for all logs from this app
    private const val BASE_TAG = "CAEXInspector"

    /**
     * Log a debug message.
     *
     * @param tag Component-specific tag
     * @param message Message to log
     */
    fun d(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.d("$BASE_TAG:$tag", message)
        }
    }

    /**
     * Log an info message.
     *
     * @param tag Component-specific tag
     * @param message Message to log
     */
    fun i(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.i("$BASE_TAG:$tag", message)
        }
    }

    /**
     * Log a warning message.
     *
     * @param tag Component-specific tag
     * @param message Message to log
     */
    fun w(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.w("$BASE_TAG:$tag", message)
        }
    }

    /**
     * Log an error message.
     *
     * @param tag Component-specific tag
     * @param message Message to log
     */
    fun e(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.e("$BASE_TAG:$tag", message)
        }
    }

    /**
     * Log an error message with an exception.
     *
     * @param tag Component-specific tag
     * @param message Message to log
     * @param throwable Exception to log
     */
    fun e(tag: String, message: String, throwable: Throwable) {
        if (LOGGING_ENABLED) {
            Log.e("$BASE_TAG:$tag", message, throwable)
        }
    }

    /**
     * Log a verbose message.
     *
     * @param tag Component-specific tag
     * @param message Message to log
     */
    fun v(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.v("$BASE_TAG:$tag", message)
        }
    }
}