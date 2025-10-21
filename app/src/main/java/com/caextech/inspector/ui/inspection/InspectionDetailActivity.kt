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
        binding.tipoText.text = when(inspeccion.tipo) {
            "RECEPCION" -> "Recepción"
            "ENTREGA" -> "Entrega"
            else -> inspeccion.tipo
        }
        binding.estadoText.text = when(inspeccion.estado) {
            "ABIERTA" -> "Abierta"
            "PENDIENTE_CIERRE" -> "Pendiente de Cierre"
            "CERRADA" -> "Cerrada"
            else -> inspeccion.estado
        }
        binding.inspectorText.text = inspeccion.nombreInspector
        binding.supervisorText.text = inspeccion.nombreSupervisor

        // Formatear fechas
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        binding.fechaCreacionText.text = sdf.format(Date(inspeccion.fechaCreacion))

        // Manejar fecha estimada
        handleFechaEstimada(inspeccion, sdf)

        // Mostrar fecha de finalización
        if (inspeccion.fechaFinalizacion != null) {
            binding.fechaFinalizacionText.text = sdf.format(Date(inspeccion.fechaFinalizacion))
        } else {
            binding.fechaFinalizacionText.text = "Pendiente"
        }

        // Mostrar métricas o hallazgos según estado
        handleContentDisplay(inspeccion)

        // Mostrar comentarios
        binding.comentariosText.text = inspeccion.comentariosGenerales.ifEmpty { "Sin comentarios" }
    }

    private fun handleFechaEstimada(inspeccion: com.caextech.inspector.data.entities.Inspeccion, sdf: SimpleDateFormat) {
        when {
            // Si la inspección tiene fecha estimada propia, mostrarla
            inspeccion.fechaTerminoEstimada != null -> {
                binding.fechaEstimadaText.text = sdf.format(Date(inspeccion.fechaTerminoEstimada))
            }
            // Si es inspección de entrega, buscar fecha estimada de la recepción
            inspeccion.tipo == "ENTREGA" && inspeccion.inspeccionRecepcionId != null -> {
                // Cargar la fecha de la recepción de forma asíncrona pero garantizar actualización
                loadFechaEstimadaFromRecepcion(inspeccion.inspeccionRecepcionId, sdf)
            }
            // Si no hay fecha estimada disponible
            else -> {
                binding.fechaEstimadaText.text = "No definida"
            }
        }
    }

    private fun loadFechaEstimadaFromRecepcion(recepcionId: Long, sdf: SimpleDateFormat) {
        // Establecer texto temporal
        binding.fechaEstimadaText.text = "Cargando..."

        inspeccionViewModel.getInspeccionConCAEXById(recepcionId).observe(this) { recepcion ->
            binding.fechaEstimadaText.text = if (recepcion?.inspeccion?.fechaTerminoEstimada != null) {
                sdf.format(Date(recepcion.inspeccion.fechaTerminoEstimada))
            } else {
                "No definida"
            }
        }
    }

    private fun handleContentDisplay(inspeccion: com.caextech.inspector.data.entities.Inspeccion) {
        when {
            // Recepción abierta/pendiente - mostrar hallazgos
            inspeccion.tipo == "RECEPCION" && inspeccion.estado in listOf("ABIERTA", "PENDIENTE_CIERRE") -> {
                showHallazgos(inspeccion.inspeccionId)
            }

            // Entrega cerrada - métricas del ciclo completo
            inspeccion.tipo == "ENTREGA" && inspeccion.fechaFinalizacion != null && inspeccion.inspeccionRecepcionId != null -> {
                showMetricasCicloCompleto(inspeccion)
            }

            // Cualquier inspección cerrada - métricas individuales
            inspeccion.fechaFinalizacion != null -> {
                showMetricasIndividuales(inspeccion)
            }

            // Inspección abierta que no es recepción (ej: entrega abierta)
            inspeccion.estado == "ABIERTA" -> {
                showHallazgos(inspeccion.inspeccionId)
            }

            // Otros casos - ocultar ambos
            else -> {
                binding.metricasCard.visibility = View.GONE
                binding.hallazgosCard.visibility = View.GONE
            }
        }

        // Configurar constraints según qué tarjeta esté visible
        updateConstraints()
    }

    private fun showHallazgos(inspeccionId: Long) {
        binding.metricasCard.visibility = View.GONE
        mostrarHallazgosPendientes(inspeccionId)
    }

    private fun showMetricasCicloCompleto(inspeccion: com.caextech.inspector.data.entities.Inspeccion) {
        binding.hallazgosCard.visibility = View.GONE
        binding.metricasCard.visibility = View.VISIBLE

        inspeccionViewModel.getInspeccionConCAEXById(inspeccion.inspeccionRecepcionId!!).observe(this) { recepcion ->
            recepcion?.let {
                val metricas = InspectionAnalytics.obtenerMetricasEntregaParaExportacion(inspeccion, it.inspeccion)
                binding.duracionText.text = "${metricas["duracionRealFormateada"]} (ciclo completo)"
                binding.desviacionText.text = metricas["desviacionEstimadaFormateada"] as? String ?: "N/A"
                binding.estadoTiempoText.text = metricas["estadoTiempo"] as? String ?: "N/A"
            }
        }
    }

    private fun showMetricasIndividuales(inspeccion: com.caextech.inspector.data.entities.Inspeccion) {
        binding.hallazgosCard.visibility = View.GONE
        binding.metricasCard.visibility = View.VISIBLE

        val metricas = InspectionAnalytics.obtenerMetricasParaExportacion(inspeccion)
        binding.duracionText.text = metricas["duracionRealFormateada"] as? String ?: "N/A"
        binding.desviacionText.text = metricas["desviacionEstimadaFormateada"] as? String ?: "N/A"
        binding.estadoTiempoText.text = metricas["estadoTiempo"] as? String ?: "N/A"
    }

    private fun updateConstraints() {
        // Ajustar constraints según qué tarjeta esté visible
        val params = binding.comentariosCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams

        when {
            binding.hallazgosCard.visibility == View.VISIBLE -> {
                params.topToBottom = binding.hallazgosCard.id
            }
            binding.metricasCard.visibility == View.VISIBLE -> {
                params.topToBottom = binding.metricasCard.id
            }
            else -> {
                params.topToBottom = binding.fechasCard.id
            }
        }

        binding.comentariosCard.layoutParams = params
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_INSPECCION_ID = "extra_inspeccion_id"
    }
}