package com.caextech.inspector.ui.inspection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.databinding.ActivityInspectionDetailBinding
import com.caextech.inspector.ui.viewmodels.InspeccionViewModel
import com.caextech.inspector.utils.InspectionAnalytics
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad para mostrar los detalles de una inspección.
 * Se abre desde las notificaciones cuando se alcanza la fecha estimada.
 */
class InspectionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInspectionDetailBinding
    private lateinit var inspeccionViewModel: InspeccionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInspectionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Inicializar ViewModel
        val application = application as CAEXInspectorApp
        inspeccionViewModel = ViewModelProvider(
            this,
            InspeccionViewModel.InspeccionViewModelFactory(
                application.inspeccionRepository,
                application.caexRepository
            )
        )[InspeccionViewModel::class.java]

        // Obtener ID de inspección
        val inspeccionId = intent.getLongExtra(EXTRA_INSPECCION_ID, -1L)
        if (inspeccionId == -1L) {
            finish()
            return
        }

        // Cargar detalles de la inspección
        loadInspectionDetails(inspeccionId)
    }

    private fun loadInspectionDetails(inspeccionId: Long) {
        inspeccionViewModel.getInspeccionConCAEXById(inspeccionId).observe(this) { inspeccionConCAEX ->
            if (inspeccionConCAEX != null) {
                displayInspectionDetails(inspeccionConCAEX)
            } else {
                // Manejar error - inspección no encontrada
                finish()
            }
        }
    }

    private fun displayInspectionDetails(inspeccionConCAEX: com.caextech.inspector.data.relations.InspeccionConCAEX) {
        val inspeccion = inspeccionConCAEX.inspeccion
        val caex = inspeccionConCAEX.caex

        // Configurar título
        supportActionBar?.title = "Detalle de Inspección"
        supportActionBar?.subtitle = caex.getNombreCompleto()

        // Mostrar información básica
        binding.tipoText.text = inspeccion.tipo
        binding.estadoText.text = inspeccion.estado
        binding.inspectorText.text = inspeccion.nombreInspector
        binding.supervisorText.text = inspeccion.nombreSupervisor

        // Formatear fechas
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        binding.fechaCreacionText.text = sdf.format(Date(inspeccion.fechaCreacion))

        inspeccion.fechaFinalizacion?.let {
            binding.fechaFinalizacionText.text = sdf.format(Date(it))
        }

        inspeccion.fechaTerminoEstimada?.let {
            binding.fechaEstimadaText.text = sdf.format(Date(it))
        }

        // Mostrar métricas si la inspección está cerrada
        if (inspeccion.fechaFinalizacion != null) {
            val metricas = InspectionAnalytics.obtenerMetricasParaExportacion(inspeccion)
            binding.duracionText.text = metricas["duracionRealFormateada"] as? String ?: "N/A"
            binding.desviacionText.text = metricas["desviacionEstimadaFormateada"] as? String ?: "N/A"
            binding.estadoTiempoText.text = metricas["estadoTiempo"] as? String ?: "N/A"
        }

        binding.comentariosText.text = inspeccion.comentariosGenerales.ifEmpty { "Sin comentarios" }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_INSPECCION_ID = "extra_inspeccion_id"
    }
}