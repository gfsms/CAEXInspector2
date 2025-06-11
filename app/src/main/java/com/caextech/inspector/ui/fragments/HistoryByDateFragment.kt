package com.caextech.inspector.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.databinding.FragmentHistoryListBinding
import com.caextech.inspector.ui.adapters.HistoryDateAdapter
import com.caextech.inspector.ui.viewmodels.RespuestaViewModel

class HistoryByDateFragment : Fragment() {
    private var _binding: FragmentHistoryListBinding? = null
    private val binding get() = _binding!!

    private lateinit var respuestaViewModel: RespuestaViewModel
    private lateinit var adapter: HistoryDateAdapter
    private var caexId: Long = 0

    companion object {
        private const val ARG_CAEX_ID = "caex_id"

        fun newInstance(caexId: Long): HistoryByDateFragment {
            return HistoryByDateFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_CAEX_ID, caexId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        caexId = arguments?.getLong(ARG_CAEX_ID) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val application = requireActivity().application as CAEXInspectorApp
        respuestaViewModel = ViewModelProvider(
            this,
            RespuestaViewModel.RespuestaViewModelFactory(application.respuestaRepository)
        )[RespuestaViewModel::class.java]

        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = HistoryDateAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryByDateFragment.adapter
        }
    }

    private fun observeData() {
        // Obtener historial ordenado por fecha
        respuestaViewModel.getHistorialHallazgosByCAEX(caexId).observe(viewLifecycleOwner) { historial ->
            adapter.submitList(historial)
            binding.emptyView.visibility = if (historial.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}