package com.caextech.inspector.ui.fragments

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.caextech.inspector.CAEXInspectorApp
import com.caextech.inspector.R
import com.caextech.inspector.data.entities.Inspeccion
import com.caextech.inspector.databinding.FragmentOpenInspectionsBinding
import com.caextech.inspector.ui.adapters.InspectionAdapter
import com.caextech.inspector.ui.inspection.CreateDeliveryInspectionActivity
import com.caextech.inspector.ui.inspection.InspectionQuestionnaireActivity
import com.caextech.inspector.ui.viewmodels.InspeccionViewModel
import com.caextech.inspector.data.relations.InspeccionConCAEX // Adjust the package name
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
            handleInspectionClick(inspeccion)
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

    /**
     * Handles clicks on inspection items.
     * For regular open inspections, continues the inspection.
     * For "Pending closure" inspections, offers to create a delivery inspection.
     */
    private fun handleInspectionClick(inspeccion: InspeccionConCAEX) {
        val inspectionId = inspeccion.inspeccion.inspeccionId
        val inspectionState = inspeccion.inspeccion.estado

        if (inspectionState == Inspeccion.ESTADO_PENDIENTE_CIERRE) {
            // Check if this inspection already has a delivery inspection
            inspeccionViewModel.tieneInspeccionEntregaAsociada(inspectionId).observe(viewLifecycleOwner) { tieneEntrega ->
                if (tieneEntrega) {
                    // Already has a delivery inspection, find it and continue it
                    inspeccionViewModel.getInspeccionEntregaByRecepcion(inspectionId).observe(viewLifecycleOwner) { entrega ->
                        if (entrega != null) {
                            // Navigate to the delivery inspection
                            val intent = Intent(requireContext(), InspectionQuestionnaireActivity::class.java).apply {
                                putExtra(InspectionQuestionnaireActivity.EXTRA_INSPECCION_ID, entrega.inspeccionId)
                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(requireContext(), "Error al obtener la inspección de entrega", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Ask if user wants to create a delivery inspection
                    showCreateDeliveryDialog(inspectionId)
                }
            }
        } else {
            // Regular open inspection, continue normally
            val intent = Intent(requireContext(), InspectionQuestionnaireActivity::class.java).apply {
                putExtra(InspectionQuestionnaireActivity.EXTRA_INSPECCION_ID, inspectionId)
            }
            startActivity(intent)
        }
    }

    /**
     * Shows a dialog asking if the user wants to create a delivery inspection.
     */
    private fun showCreateDeliveryDialog(recepcionId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.create_delivery_inspection)
            .setMessage("Esta inspección está en estado pendiente de cierre. ¿Desea crear una inspección de entrega?")
            .setPositiveButton("Sí, crear") { _, _ ->
                // Create delivery inspection directly instead of launching another activity
                val inspector = "Inspector de Entrega" // Default values
                val supervisor = "Supervisor de Entrega"

                // Show loading indicator
                val progressDialog = ProgressDialog(requireContext())
                progressDialog.setMessage("Creando inspección de entrega...")
                progressDialog.setCancelable(false)
                progressDialog.show()

                // Create the inspection
                inspeccionViewModel.crearInspeccionEntrega(recepcionId, inspector, supervisor)

                // Observe the result
                inspeccionViewModel.operationStatus.observe(viewLifecycleOwner) { status ->
                    progressDialog.dismiss()

                    when (status) {
                        is InspeccionViewModel.OperationStatus.Success -> {
                            Toast.makeText(requireContext(), status.message, Toast.LENGTH_SHORT).show()

                            // Navigate directly to questionnaire
                            val intent = Intent(requireContext(), InspectionQuestionnaireActivity::class.java).apply {
                                putExtra(InspectionQuestionnaireActivity.EXTRA_INSPECCION_ID, status.id)
                            }
                            startActivity(intent)
                        }
                        is InspeccionViewModel.OperationStatus.Error -> {
                            Toast.makeText(requireContext(), "Error: ${status.message}", Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                }
            }
            .setNegativeButton("No, cancelar", null)
            .show()
    }

    /**
     * Creates a delivery inspection for the given reception inspection.
     */
    private fun createDeliveryInspection(recepcionId: Long) {
        // Get the reception inspection info to pass to the delivery inspection
        inspeccionViewModel.getInspeccionConCAEXById(recepcionId).observe(viewLifecycleOwner) { recepcion ->
            if (recepcion != null) {
                // Launch an activity to collect inspector and supervisor names
                val intent = Intent(requireContext(), CreateDeliveryInspectionActivity::class.java).apply {
                    putExtra(CreateDeliveryInspectionActivity.EXTRA_INSPECCION_RECEPCION_ID, recepcionId)
                    putExtra(CreateDeliveryInspectionActivity.EXTRA_CAEX_ID, recepcion.caex.caexId)
                    putExtra(CreateDeliveryInspectionActivity.EXTRA_CAEX_NOMBRE, recepcion.caex.getNombreCompleto())
                }
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Error al obtener la inspección de recepción", Toast.LENGTH_SHORT).show()
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