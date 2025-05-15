package com.caextech.inspector.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.databinding.FragmentCaexListBinding
import com.caextech.inspector.ui.adapters.CAEXAdapter
import com.caextech.inspector.ui.inspection.CreateInspectionActivity
import com.caextech.inspector.ui.inspection.InspectionQuestionnaireActivity
import com.caextech.inspector.ui.viewmodels.CAEXViewModel
import com.caextech.inspector.ui.viewmodels.InspeccionViewModel
import com.caextech.inspector.data.models.CAEXConInfo

/**
 * Fragment for displaying and managing the list of CAEX equipments.
 */
class CAEXListFragment : Fragment() {

    private var _binding: FragmentCaexListBinding? = null
    private val binding get() = _binding!!

    private lateinit var caexViewModel: CAEXViewModel
    private lateinit var inspeccionViewModel: InspeccionViewModel
    private lateinit var caexAdapter: CAEXAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCaexListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModels
        initViewModels()

        // Setup UI components
        setupRecyclerView()
        setupSearch()
        setupFilters()

        // Observe data
        observeViewModel()
    }

    private fun initViewModels() {
        val application = requireActivity().application as CAEXInspectorApp

        caexViewModel = ViewModelProvider(
            this,
            CAEXViewModel.CAEXViewModelFactory(application.caexRepository)
        )[CAEXViewModel::class.java]

        inspeccionViewModel = ViewModelProvider(
            this,
            InspeccionViewModel.InspeccionViewModelFactory(
                application.inspeccionRepository,
                application.caexRepository
            )
        )[InspeccionViewModel::class.java]
    }

    private fun setupRecyclerView() {
        caexAdapter = CAEXAdapter(
            onItemClick = { caexInfo ->
                // Handle CAEX item click - could show details or inspection history
                // For now, we'll open the create inspection activity
                handleCAEXClick(caexInfo)
            },
            onCreateInspectionClick = { caexInfo ->
                // Handle create inspection button click
                handleCreateInspectionClick(caexInfo)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = caexAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                caexViewModel.setSearchText(s.toString())
            }
        })
    }

    private fun setupFilters() {
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedChipId = checkedIds.firstOrNull()
            val model = when (selectedChipId) {
                binding.chip797F.id -> "797F"
                binding.chip798AC.id -> "798AC"
                else -> "TODOS"
            }
            caexViewModel.setModelFilter(model)
        }
    }

    private fun observeViewModel() {
        // Observe filtered CAEX list
        caexViewModel.filteredCAEXWithInfo.observe(viewLifecycleOwner) { caexList ->
            caexAdapter.submitList(caexList)

            // Show/hide empty view
            updateEmptyView(caexList.isEmpty())
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun handleCAEXClick(caexInfo: CAEXConInfo) {
        // Check if CAEX has pending inspection
        if (caexInfo.tieneInspeccionPendiente) {
            // Navigate to continue inspection
            // We would need to get the inspection ID and navigate to InspectionQuestionnaireActivity
            // For now, just show create inspection
            handleCreateInspectionClick(caexInfo)
        } else {
            // Could show CAEX details or inspection history
            // For now, go to create inspection
            handleCreateInspectionClick(caexInfo)
        }
    }

    private fun handleCreateInspectionClick(caexInfo: CAEXConInfo) {
        val caex = caexInfo.caex

        // Check current status to determine the appropriate action
        when {
            caexInfo.tieneInspeccionPendiente -> {
                // Has open inspection - would need to get inspection ID and continue
                // This requires additional logic to find the open inspection
                // For now, show create inspection dialog
                showCreateInspectionDialog(caex.caexId, caex.getNombreCompleto())
            }

            caexInfo.estadoUltimaInspeccion == "PENDIENTE_CIERRE" -> {
                // Reception inspection pending closure - should create delivery inspection
                // This also requires additional logic to find the reception inspection
                // For now, show create inspection dialog
                showCreateInspectionDialog(caex.caexId, caex.getNombreCompleto())
            }

            else -> {
                // Create new reception inspection
                showCreateInspectionDialog(caex.caexId, caex.getNombreCompleto())
            }
        }
    }

    private fun showCreateInspectionDialog(caexId: Long, caexName: String) {
        val intent = Intent(requireContext(), CreateInspectionActivity::class.java).apply {
            // Use the correct extra names from CreateInspectionActivity
            putExtra("EXTRA_CAEX_ID", caexId)
            putExtra("EXTRA_CAEX_NAME", caexName)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = CAEXListFragment()
    }
}