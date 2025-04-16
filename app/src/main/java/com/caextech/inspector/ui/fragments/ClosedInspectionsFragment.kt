package com.caextech.inspector.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.databinding.FragmentClosedInspectionsBinding
import com.caextech.inspector.ui.adapters.InspectionAdapter
import com.caextech.inspector.ui.viewmodels.InspeccionViewModel

/**
 * Fragment for displaying the list of closed inspections.
 */
class ClosedInspectionsFragment : Fragment() {

    private var _binding: FragmentClosedInspectionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var inspeccionViewModel: InspeccionViewModel
    private lateinit var inspectionAdapter: InspectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClosedInspectionsBinding.inflate(inflater, container, false)
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
            // For closed inspections, we could show details or history
            // We could implement a detail view activity in the future
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = inspectionAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        // Observe closed inspections
        inspeccionViewModel.inspeccionesCerradasConCAEX.observe(viewLifecycleOwner) { inspecciones ->
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
        fun newInstance() = ClosedInspectionsFragment()
    }
}