package com.caextech.inspector.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.databinding.FragmentOpenInspectionsBinding
import com.caextech.inspector.ui.adapters.InspectionAdapter
import com.caextech.inspector.ui.inspection.InspectionQuestionnaireActivity
import com.caextech.inspector.ui.viewmodels.InspeccionViewModel

/**
 * Fragment for displaying the list of open inspections.
 */
class OpenInspectionsFragment : Fragment() {

    private var _binding: FragmentOpenInspectionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var inspeccionViewModel: InspeccionViewModel
    private lateinit var inspectionAdapter: InspectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOpenInspectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        val application = requireActivity().application as CAEXInspectorApp
        inspeccionViewModel = ViewModelProvider(
            this,
            InspeccionViewModel.InspeccionViewModelFactory(
                application.inspeccionRepository,
                application.caexRepository
            )
        )[InspeccionViewModel::class.java]

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        inspectionAdapter = InspectionAdapter { inspeccion ->
            // Handle inspection item click
            // Continue inspection in InspectionQuestionnaireActivity
            val intent = Intent(requireContext(), InspectionQuestionnaireActivity::class.java).apply {
                putExtra(InspectionQuestionnaireActivity.EXTRA_INSPECCION_ID, inspeccion.inspeccion.inspeccionId)
            }
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = inspectionAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        // Observe open inspections
        inspeccionViewModel.inspeccionesAbiertasConCAEX.observe(viewLifecycleOwner) { inspecciones ->
            inspectionAdapter.submitList(inspecciones)

            // Show empty view if list is empty
            if (inspecciones.isEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = OpenInspectionsFragment()
    }
}