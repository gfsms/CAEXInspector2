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
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.caextech.inspector.ui.adapters.HallazgosAdapter

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
                application.caexRepository,
                application.respuestaRepository
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
    private fun mostrarHallazgosPendientes(inspeccionId: Long) {
        binding.hallazgosCard.visibility = View.VISIBLE

        val adapter = HallazgosAdapter()
        binding.hallazgosRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@InspectionDetailActivity)
            this.adapter = adapter
        }

        inspeccionViewModel.getHallazgosNoConformes(inspeccionId).observe(this) { hallazgos ->
            if (hallazgos.isNotEmpty()) {
                binding.hallazgosRecyclerView.visibility = View.VISIBLE
                binding.noHallazgosText.visibility = View.GONE
                adapter.submitList(hallazgos)
            } else {
                binding.hallazgosRecyclerView.visibility = View.GONE
                binding.noHallazgosText.visibility = View.VISIBLE
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

        // Mostrar fecha estimada
        if (inspeccion.fechaTerminoEstimada != null) {
            binding.fechaEstimadaText.text = sdf.format(Date(inspeccion.fechaTerminoEstimada))
        } else if (inspeccion.tipo == "ENTREGA" && inspeccion.inspeccionRecepcionId != null) {
            inspeccionViewModel.getInspeccionConCAEXById(inspeccion.inspeccionRecepcionId).observe(this) { recepcion ->
                recepcion?.inspeccion?.fechaTerminoEstimada?.let { fechaEstimada ->
                    binding.fechaEstimadaText.text = sdf.format(Date(fechaEstimada))
                }
            }
        }

// Mostrar fecha de finalización solo si está cerrada
        if (inspeccion.fechaFinalizacion != null && inspeccion.estado == "CERRADA") {
            binding.fechaFinalizacionText.text = sdf.format(Date(inspeccion.fechaFinalizacion))
        } else {
            // Ocultar la fila de finalización
            binding.fechaFinalizacionText.text = "Pendiente"
        }

        // Mostrar fecha estimada (de recepción si es entrega)
        if (inspeccion.fechaTerminoEstimada != null) {
            binding.fechaEstimadaText.text = sdf.format(Date(inspeccion.fechaTerminoEstimada))
        } else if (inspeccion.tipo == "ENTREGA" && inspeccion.inspeccionRecepcionId != null) {
            // Obtener fecha estimada de la inspección de recepción
            inspeccionViewModel.getInspeccionConCAEXById(inspeccion.inspeccionRecepcionId).observe(this) { recepcion ->
                recepcion?.inspeccion?.fechaTerminoEstimada?.let { fechaEstimada ->
                    binding.fechaEstimadaText.text = sdf.format(Date(fechaEstimada))
                }
            }
        }


        // Mostrar métricas o hallazgos según estado
        when {
            // Recepción abierta/pendiente - mostrar hallazgos
            inspeccion.tipo == "RECEPCION" && inspeccion.estado in listOf("ABIERTA", "PENDIENTE_CIERRE") -> {
                binding.metricasCard.visibility = View.GONE
                mostrarHallazgosPendientes(inspeccion.inspeccionId)
            }

            // Entrega cerrada - métricas del ciclo completo
            inspeccion.tipo == "ENTREGA" && inspeccion.fechaFinalizacion != null && inspeccion.inspeccionRecepcionId != null -> {
                binding.hallazgosCard.visibility = View.GONE
                inspeccionViewModel.getInspeccionConCAEXById(inspeccion.inspeccionRecepcionId).observe(this) { recepcion ->
                    recepcion?.let {
                        val metricas = InspectionAnalytics.obtenerMetricasEntregaParaExportacion(inspeccion, it.inspeccion)
                        binding.duracionText.text = "${metricas["duracionRealFormateada"]} (ciclo completo)"
                        binding.desviacionText.text = metricas["desviacionEstimadaFormateada"] as? String ?: "N/A"
                        binding.estadoTiempoText.text = metricas["estadoTiempo"] as? String ?: "N/A"
                    }
                }
            }

            // Recepción cerrada - métricas parciales
            inspeccion.fechaFinalizacion != null -> {
                binding.hallazgosCard.visibility = View.GONE
                val metricas = InspectionAnalytics.obtenerMetricasParaExportacion(inspeccion)
                binding.duracionText.text = metricas["duracionRealFormateada"] as? String ?: "N/A"
                binding.desviacionText.text = metricas["desviacionEstimadaFormateada"] as? String ?: "N/A"
                binding.estadoTiempoText.text = metricas["estadoTiempo"] as? String ?: "N/A"
            }

            // Otros casos - ocultar ambos
            else -> {
                binding.metricasCard.visibility = View.GONE
                binding.hallazgosCard.visibility = View.GONE
            }
        }
        // Ajustar constraints según qué tarjeta esté visible
        if (binding.hallazgosCard.visibility == View.VISIBLE) {
            // Comentarios van después de hallazgos
            val params = binding.comentariosCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.topToBottom = binding.hallazgosCard.id
        } else {
            // Comentarios van después de métricas
            val params = binding.comentariosCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.topToBottom = binding.metricasCard.id
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