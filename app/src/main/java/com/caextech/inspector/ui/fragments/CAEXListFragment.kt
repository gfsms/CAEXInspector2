package com.caextech.inspector.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.databinding.FragmentCaexListBinding
import com.caextech.inspector.ui.adapters.CAEXAdapter
import com.caextech.inspector.ui.viewmodels.CAEXViewModel

/**
 * Fragment for displaying the list of CAEX equipments.
 */
class CAEXListFragment : Fragment() {

    private var _binding: FragmentCaexListBinding? = null
    private val binding get() = _binding!!

    private lateinit var caexViewModel: CAEXViewModel
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

        // Initialize ViewModel
        val application = requireActivity().application as CAEXInspectorApp
        caexViewModel = ViewModelProvider(
            this,
            CAEXViewModel.CAEXViewModelFactory(application.caexRepository)
        )[CAEXViewModel::class.java]

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        caexAdapter = CAEXAdapter { caex ->
            // Handle CAEX item click
            // For example, show CAEX details or start inspection
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = caexAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        // Observe CAEX list
        caexViewModel.allCAEX.observe(viewLifecycleOwner) { caexList ->
            caexAdapter.submitList(caexList)

            // Show empty view if list is empty
            if (caexList.isEmpty()) {
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
        fun newInstance() = CAEXListFragment()
    }
}