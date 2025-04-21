package com.caextech.inspector.ui.inspection

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.R
import com.caextech.inspector.data.entities.Inspeccion
import com.caextech.inspector.data.entities.Respuesta
import com.caextech.inspector.databinding.ActivityInspectionQuestionnaireBinding
import com.caextech.inspector.ui.adapters.CategoryPagerAdapter
import com.caextech.inspector.ui.adapters.DeliveryCategoryPagerAdapter
import com.caextech.inspector.ui.viewmodels.CategoriaPreguntaViewModel
import com.caextech.inspector.ui.viewmodels.InspeccionViewModel
import com.caextech.inspector.ui.viewmodels.RespuestaViewModel
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

/**
 * Activity for displaying and answering the inspection questionnaire.
 * Handles both reception and delivery inspections.
 */
class InspectionQuestionnaireActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInspectionQuestionnaireBinding

    // ViewModels
    private lateinit var inspeccionViewModel: InspeccionViewModel
    private lateinit var categoriaPreguntaViewModel: CategoriaPreguntaViewModel
    private lateinit var respuestaViewModel: RespuestaViewModel

    // Adapter for category tabs and viewpager
    private var categoryPagerAdapter: CategoryPagerAdapter? = null
    private var deliveryCategoryPagerAdapter: DeliveryCategoryPagerAdapter? = null

    // Inspection data
    private var inspeccionId: Long = 0
    private var inspeccionRecepcionId: Long? = null
    private var modeloCAEX: String = ""
    private var tipoInspeccion: String = Inspeccion.TIPO_RECEPCION
    private var preguntasNoConformes = mutableSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityInspectionQuestionnaireBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configure Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get inspection ID from intent
        inspeccionId = intent.getLongExtra(EXTRA_INSPECCION_ID, 0)
        if (inspeccionId == 0L) {
            Toast.makeText(this, "Error: No se especificó una inspección", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize ViewModels
        initViewModels()

        // Load inspection data
        loadInspectionData()

        // Set up NextCategory FAB
        binding.nextCategoryFab.setOnClickListener {
            navigateToNextCategory()
        }
    }

    /**
     * Initializes the ViewModels used in this activity.
     */
    private fun initViewModels() {
        val application = application as CAEXInspectorApp

        inspeccionViewModel = ViewModelProvider(
            this,
            InspeccionViewModel.InspeccionViewModelFactory(
                application.inspeccionRepository,
                application.caexRepository
            )
        )[InspeccionViewModel::class.java]

        categoriaPreguntaViewModel = ViewModelProvider(
            this,
            CategoriaPreguntaViewModel.CategoriaPreguntaViewModelFactory(
                application.categoriaRepository,
                application.preguntaRepository
            )
        )[CategoriaPreguntaViewModel::class.java]

        respuestaViewModel = ViewModelProvider(
            this,
            RespuestaViewModel.RespuestaViewModelFactory(
                application.respuestaRepository
            )
        )[RespuestaViewModel::class.java]
    }

    /**
     * Loads the inspection data and sets up the UI accordingly.
     */
    private fun loadInspectionData() {
        inspeccionViewModel.getInspeccionConCAEXById(inspeccionId).observe(this) { inspeccionConCAEX ->
            if (inspeccionConCAEX == null) {
                Toast.makeText(this, "Error: Inspección no encontrada", Toast.LENGTH_SHORT).show()
                finish()
                return@observe
            }

            // Get inspection type
            tipoInspeccion = inspeccionConCAEX.inspeccion.tipo

            // Get reception inspection ID if this is a delivery inspection
            inspeccionRecepcionId = inspeccionConCAEX.inspeccion.inspeccionRecepcionId

            // If this is a delivery inspection, load no-conforme questions from reception
            if (tipoInspeccion == Inspeccion.TIPO_ENTREGA && inspeccionRecepcionId != null) {
                loadNoConformePreguntas(inspeccionRecepcionId!!)
            }

            // Set title with CAEX info
            val titlePrefix = if (tipoInspeccion == Inspeccion.TIPO_RECEPCION)
                getString(R.string.inspection_questionnaire)
            else
                getString(R.string.delivery_inspection)

            title = "$titlePrefix - ${inspeccionConCAEX.caex.getNombreCompleto()}"

            // Get CAEX model for filtering categories and questions
            modeloCAEX = inspeccionConCAEX.caex.modelo

            // Load categories for this CAEX model
            loadCategoriesForModel(modeloCAEX)
        }
    }

    /**
     * Loads the list of questions that were marked as "No Conforme" in the reception inspection.
     */
    private fun loadNoConformePreguntas(recepcionId: Long) {
        respuestaViewModel.getRespuestasConDetallesByInspeccionYEstado(
            recepcionId,
            Respuesta.ESTADO_NO_CONFORME
        ).observe(this) { noConformes ->
            preguntasNoConformes.clear()
            preguntasNoConformes.addAll(noConformes.map { it.pregunta.preguntaId })

            // Update adapter if already created
            deliveryCategoryPagerAdapter?.updateNoConformePreguntas(preguntasNoConformes)
        }
    }

    /**
     * Loads categories applicable to the given CAEX model and sets up the TabLayout and ViewPager.
     */
    private fun loadCategoriesForModel(modelo: String) {
        categoriaPreguntaViewModel.getCategoriasConPreguntasByModelo(modelo).observe(this) { categorias ->
            if (categorias.isEmpty()) {
                Toast.makeText(this, "No hay categorías definidas para este modelo", Toast.LENGTH_SHORT).show()
                return@observe
            }

            // Filter categories that have at least one question for this model
            val validCategorias = categorias.filter { it.tienePreguntas(modelo) }

            if (tipoInspeccion == Inspeccion.TIPO_RECEPCION) {
                // Initialize the adapter with the inspection ID and categories for reception
                categoryPagerAdapter = CategoryPagerAdapter(
                    this,
                    inspeccionId,
                    modelo,
                    validCategorias
                )

                // Set up ViewPager
                binding.categoryViewPager.adapter = categoryPagerAdapter
            } else {
                // Initialize the adapter with the inspection ID and categories for delivery
                deliveryCategoryPagerAdapter = DeliveryCategoryPagerAdapter(
                    this,
                    inspeccionId,
                    inspeccionRecepcionId ?: 0,
                    modelo,
                    validCategorias,
                    preguntasNoConformes
                )

                // Set up ViewPager
                binding.categoryViewPager.adapter = deliveryCategoryPagerAdapter
            }

            // Connect TabLayout with ViewPager
            TabLayoutMediator(binding.categoryTabLayout, binding.categoryViewPager) { tab, position ->
                tab.text = validCategorias[position].categoria.nombre
            }.attach()
        }
    }

    /**
     * Navigates to the next category tab if possible.
     * If all categories are completed, shows a dialog to proceed to the summary.
     */
    private fun navigateToNextCategory() {
        val adapter = binding.categoryViewPager.adapter ?: return
        val currentPosition = binding.categoryViewPager.currentItem
        val totalCount = adapter.itemCount

        if (currentPosition < totalCount - 1) {
            // Move to the next tab
            binding.categoryViewPager.currentItem = currentPosition + 1
        } else {
            // We're on the last tab, check if all questions are answered
            checkAllQuestionsAnswered()
        }
    }

    /**
     * Checks if all questions have been answered and shows a confirmation dialog
     * to navigate to the NoConformeSummaryActivity if complete.
     */
    private fun checkAllQuestionsAnswered() {
        // Here we would check if all categories have been completed
        // For now, just ask the user if they want to continue

        AlertDialog.Builder(this)
            .setTitle("Completar cuestionario")
            .setMessage("¿Has terminado de responder todas las preguntas?")
            .setPositiveButton("Sí, continuar") { _, _ ->
                navigateToSummary()
            }
            .setNegativeButton("No, revisar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Navigates to the NoConformeSummaryActivity to review Non-Conforming or Rejected responses.
     */
    private fun navigateToSummary() {
        val intent = Intent(this, NoConformeSummaryActivity::class.java).apply {
            putExtra(NoConformeSummaryActivity.EXTRA_INSPECCION_ID, inspeccionId)
            putExtra(NoConformeSummaryActivity.EXTRA_INSPECCION_TIPO, tipoInspeccion)
            if (inspeccionRecepcionId != null) {
                putExtra(NoConformeSummaryActivity.EXTRA_INSPECCION_RECEPCION_ID, inspeccionRecepcionId)
            }
        }
        startActivity(intent)
    }

    /**
     * Handles the Up/Back navigation in the ActionBar.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // Show confirmation dialog before leaving
                showExitConfirmationDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Shows a confirmation dialog when the user tries to exit the questionnaire.
     */
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Salir del cuestionario")
            .setMessage("¿Estás seguro de que quieres salir? Se guardarán las respuestas ingresadas hasta ahora.")
            .setPositiveButton("Sí, salir") { _, _ ->
                finish()
            }
            .setNegativeButton("No, continuar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Handles back button press to show confirmation dialog.
     */
    override fun onBackPressed() {
        showExitConfirmationDialog()
    }

    companion object {
        const val EXTRA_INSPECCION_ID = "extra_inspeccion_id"
    }
}