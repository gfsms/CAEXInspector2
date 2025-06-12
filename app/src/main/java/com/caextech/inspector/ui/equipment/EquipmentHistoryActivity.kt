package com.caextech.inspector.ui.equipment

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.databinding.ActivityEquipmentHistoryBinding
import com.caextech.inspector.ui.adapters.EquipmentHistoryPagerAdapter
import com.caextech.inspector.ui.viewmodels.RespuestaViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.caextech.inspector.utils.PdfGenerator
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.caextech.inspector.R
import java.io.File

class EquipmentHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEquipmentHistoryBinding
    private lateinit var respuestaViewModel: RespuestaViewModel
    private lateinit var pagerAdapter: EquipmentHistoryPagerAdapter

    private var caexId: Long = 0
    private var caexName: String = ""

    companion object {
        const val EXTRA_CAEX_ID = "EXTRA_CAEX_ID"
        const val EXTRA_CAEX_NAME = "EXTRA_CAEX_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEquipmentHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener extras
        caexId = intent.getLongExtra(EXTRA_CAEX_ID, 0)
        caexName = intent.getStringExtra(EXTRA_CAEX_NAME) ?: ""

        if (caexId == 0L) {
            finish()
            return
        }

        // Configurar toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Historial - $caexName"

        // Inicializar ViewModel
        initViewModel()

        // Configurar ViewPager y Tabs
        setupViewPagerAndTabs()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_equipment_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export_pdf -> {
                exportToPdf()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportToPdf() {
        respuestaViewModel.getHistorialHallazgosByCAEX(caexId).observe(this) { hallazgos ->
            val fileName = "Historial_${caexName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"

            // Guardar en Downloads público
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            PdfGenerator.generateEquipmentHistoryPdf(this, caexName, hallazgos, file)

            Toast.makeText(this, "PDF guardado en Downloads: $fileName", Toast.LENGTH_LONG).show()
        }
    }
    private fun initViewModel() {
        val application = application as CAEXInspectorApp
        respuestaViewModel = ViewModelProvider(
            this,
            RespuestaViewModel.RespuestaViewModelFactory(application.respuestaRepository)
        )[RespuestaViewModel::class.java]
    }

    private fun setupViewPagerAndTabs() {
        pagerAdapter = EquipmentHistoryPagerAdapter(this, caexId)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Por Fecha"
                1 -> "Por Categoría"
                else -> "Tab $position"
            }
        }.attach()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}