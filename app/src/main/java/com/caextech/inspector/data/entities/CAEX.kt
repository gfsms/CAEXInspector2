package com.caextech.inspector.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un camión de extracción (CAEX).
 *
 * Almacena la información básica de identificación de cada equipo.
 * Los equipos pueden ser de dos modelos: 797F o 798AC, cada uno con
 * un rango específico de números de identificación.
 */
@Entity(tableName = "caex")
data class CAEX(
    @PrimaryKey(autoGenerate = true)
    val caexId: Long = 0,

    // El identificador del CAEX (301-339, 365-366 para 797F; 340-352 para 798AC)
    val numeroIdentificador: Int,

    // El modelo del CAEX (797F o 798AC)
    val modelo: String,

    // Fecha de creación del registro
    val fechaRegistro: Long = System.currentTimeMillis()
) {
    /**
     * Valida si el número identificador es válido para el modelo especificado.
     *
     * @return true si el número es válido para el modelo, false en caso contrario
     */
    fun esIdentificadorValido(): Boolean {
        return when (modelo) {
            "797F" -> (numeroIdentificador in 301..339) ||
                    (numeroIdentificador == 365) ||
                    (numeroIdentificador == 366)
            "798AC" -> numeroIdentificador in 340..352
            else -> false
        }
    }

    /**
     * Devuelve una representación visual del equipo para mostrar en la UI.
     */
    fun getNombreCompleto(): String {
        return "CAEX $modelo #$numeroIdentificador"
    }
}