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
import com.caextech.inspector.ui.adapters.HistoryCategoryAdapter
import com.caextech.inspector.ui.viewmodels.RespuestaViewModel
import com.caextech.inspector.data.entities.getCategoriaName

class HistoryByCategoryFragment : Fragment() {
    private var _binding: FragmentHistoryListBinding? = null
    private val binding get() = _binding!!

    private lateinit var respuestaViewModel: RespuestaViewModel
    private lateinit var adapter: HistoryCategoryAdapter
    private var caexId: Long = 0

    companion object {
        private const val ARG_CAEX_ID = "caex_id"

        fun newInstance(caexId: Long): HistoryByCategoryFragment {
            return HistoryByCategoryFragment().apply {
                arguments = Bundle().apply { putLong(ARG_CAEX_ID, caexId) }
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
        respuestaViewModel = ViewModelProvider(this, RespuestaViewModel.RespuestaViewModelFactory(application.respuestaRepository))[RespuestaViewModel::class.java]

        adapter = HistoryCategoryAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryByCategoryFragment.adapter
        }

        respuestaViewModel.getHistorialHallazgosByCAEX(caexId).observe(viewLifecycleOwner) { historial ->
            val grouped = historial.groupBy { it.pregunta.getCategoriaName() }
            adapter.submitList(grouped.toList())
            binding.emptyView.visibility = if (historial.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}