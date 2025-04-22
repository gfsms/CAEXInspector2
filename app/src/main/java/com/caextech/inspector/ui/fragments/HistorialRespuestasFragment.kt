package com.caextech.inspector.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.R
import com.caextech.inspector.data.entities.Respuesta
import com.caextech.inspector.databinding.FragmentHistorialRespuestasBinding
import com.caextech.inspector.ui.adapters.HistorialRespuestasAdapter
import com.caextech.inspector.ui.viewmodels.InspeccionViewModel
import com.caextech.inspector.ui.viewmodels.RespuestaViewModel
import com.caextech.inspector.utils.Logger

class HistorialRespuestasFragment : DialogFragment() {

    private var _binding: FragmentHistorialRespuestasBinding? = null
    private val binding get() = _binding!!

    private lateinit var respuestaViewModel: RespuestaViewModel
    private lateinit var inspeccionViewModel: InspeccionViewModel
    private lateinit var historialAdapter: HistorialRespuestasAdapter

    private var caexId: Long = 0
    private var preguntaId: Long = 0
    private var inspeccionId: Long = 0
    private var preguntaTexto: String = ""

    companion object {
        private const val ARG_CAEX_ID = "arg_caex_id"
        private const val ARG_PREGUNTA_ID = "arg_pregunta_id"
        private const val ARG_INSPECCION_ID = "arg_inspeccion_id"
        private const val ARG_PREGUNTA_TEXTO = "arg_pregunta_texto"
        private const val TAG = "HistorialRespuestasFragment"

        fun newInstance(
            caexId: Long,
            preguntaId: Long,
            inspeccionId: Long,
            preguntaTexto: String
        ): HistorialRespuestasFragment {
            return HistorialRespuestasFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_CAEX_ID, caexId)
                    putLong(ARG_PREGUNTA_ID, preguntaId)
                    putLong(ARG_INSPECCION_ID, inspeccionId)
                    putString(ARG_PREGUNTA_TEXTO, preguntaTexto)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_CAEXInspector_Dialog)

        arguments?.let {
            caexId = it.getLong(ARG_CAEX_ID)
            preguntaId = it.getLong(ARG_PREGUNTA_ID)
            inspeccionId = it.getLong(ARG_INSPECCION_ID)
            preguntaTexto = it.getString(ARG_PREGUNTA_TEXTO, "")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setWindowAnimations(R.style.DialogAnimation)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistorialRespuestasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar ViewModels
        val application = requireActivity().application as CAEXInspectorApp
        respuestaViewModel = ViewModelProvider(
            this,
            RespuestaViewModel.RespuestaViewModelFactory(
                application.respuestaRepository
            )
        )[RespuestaViewModel::class.java]

        inspeccionViewModel = ViewModelProvider(
            this,
            InspeccionViewModel.InspeccionViewModelFactory(
                application.inspeccionRepository,
                application.caexRepository
            )
        )[InspeccionViewModel::class.java]

        // Configurar UI
        binding.questionTextView.text = preguntaTexto

        // Configurar RecyclerView
        setupRecyclerView()

        // Cargar historial
        loadHistorial()

        // Configurar botón de cerrar
        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        historialAdapter = HistorialRespuestasAdapter(
            onUsarIdSapClicked = { respuesta ->
                val listener = parentFragment as? OnIdSapSelectedListener
                    ?: activity as? OnIdSapSelectedListener

                if (listener != null) {
                    listener.onIdSapSelected(
                        respuesta.respuesta.tipoAccion ?: Respuesta.ACCION_INMEDIATO,
                        respuesta.respuesta.idAvisoOrdenTrabajo ?: ""
                    )
                    dismiss()
                }
            }
        )

        binding.historialRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historialAdapter
        }
    }

    private fun loadHistorial() {
        respuestaViewModel.getHistorialRespuestasNoConformeRechazado(
            caexId,
            preguntaId,
            inspeccionId
        ).observe(viewLifecycleOwner) { historial ->
            if (historial.isNotEmpty()) {
                historialAdapter.submitList(historial)
                binding.emptyHistorialText.visibility = View.GONE
                binding.historialRecyclerView.visibility = View.VISIBLE
            } else {
                binding.emptyHistorialText.visibility = View.VISIBLE
                binding.historialRecyclerView.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface OnIdSapSelectedListener {
        fun onIdSapSelected(tipoAccion: String, idSap: String)
    }
}