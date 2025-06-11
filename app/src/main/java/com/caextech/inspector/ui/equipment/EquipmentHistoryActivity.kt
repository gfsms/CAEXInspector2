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