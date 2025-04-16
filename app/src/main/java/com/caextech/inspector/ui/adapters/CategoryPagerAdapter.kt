package com.caextech.inspector.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.caextech.inspector.data.relations.CategoriaConPreguntas
import com.caextech.inspector.ui.fragments.CategoryQuestionsFragment

/**
 * Adapter for the ViewPager2 to display categories and their questions in tabs.
 *
 * This adapter creates CategoryQuestionsFragment instances for each category,
 * passing the necessary data for displaying questions specific to that category.
 */
class CategoryPagerAdapter(
    activity: FragmentActivity,
    private val inspeccionId: Long,
    private val modeloCAEX: String,
    private val categorias: List<CategoriaConPreguntas>
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = categorias.size

    override fun createFragment(position: Int): Fragment {
        val categoria = categorias[position]

        // Create a new fragment for this category with the relevant data
        return CategoryQuestionsFragment.newInstance(
            inspeccionId,
            categoria.categoria.categoriaId,
            modeloCAEX
        )
    }
}