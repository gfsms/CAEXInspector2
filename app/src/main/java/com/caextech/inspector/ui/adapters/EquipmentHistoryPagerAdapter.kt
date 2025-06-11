package com.caextech.inspector.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.caextech.inspector.ui.fragments.HistoryByDateFragment
import com.caextech.inspector.ui.fragments.HistoryByCategoryFragment

class EquipmentHistoryPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val caexId: Long
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HistoryByDateFragment.newInstance(caexId)
            1 -> HistoryByCategoryFragment.newInstance(caexId)
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
