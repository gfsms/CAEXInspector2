package com.caextech.inspector.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.caextech.inspector.data.relations.CategoriaConPreguntas
import com.caextech.inspector.ui.fragments.DeliveryCategoryQuestionsFragment

/**
 * Adapter for the ViewPager2 to display categories and their questions in tabs for delivery inspections.
 *
 * This adapter creates DeliveryCategoryQuestionsFragment instances for each category,
 * passing the necessary data for displaying questions specific to that category,
 * including highlighting questions that were marked as "No Conforme" in the reception inspection.
 */
class DeliveryCategoryPagerAdapter(
    activity: FragmentActivity,
    private val inspeccionId: Long,
    private val inspeccionRecepcionId: Long,
    private val modeloCAEX: String,
    private val categorias: List<CategoriaConPreguntas>,
    private var preguntasNoConformes: Set<Long> = emptySet()
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = categorias.size

    override fun createFragment(position: Int): Fragment {
        val categoria = categorias[position]

        // Create a new fragment for this category with the relevant data
        return DeliveryCategoryQuestionsFragment.newInstance(
            inspeccionId,
            inspeccionRecepcionId,
            categoria.categoria.categoriaId,
            modeloCAEX,
            ArrayList(preguntasNoConformes)
        )
    }

    /**
     * Updates the set of questions that were marked as "No Conforme" in the reception inspection.
     * This will trigger a recreation of the fragments.
     */
    fun updateNoConformePreguntas(noConformePreguntas: Set<Long>) {
        this.preguntasNoConformes = noConformePreguntas
        notifyDataSetChanged()
    }
}