package com.caextech.inspector.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.caextech.inspector.ui.fragments.CAEXListFragment
import com.caextech.inspector.ui.fragments.ClosedInspectionsFragment
import com.caextech.inspector.ui.fragments.OpenInspectionsFragment

/**
 * Adapter for the ViewPager2 in MainActivity.
 * Manages the three main fragments: CAEX List, Open Inspections, and Closed Inspections.
 */
class MainViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    // Tab positions
    companion object {
        const val TAB_CAEX = 0
        const val TAB_OPEN_INSPECTIONS = 1
        const val TAB_CLOSED_INSPECTIONS = 2
        const val TAB_COUNT = 3
    }

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        // Return the appropriate fragment based on position
        return when (position) {
            TAB_CAEX -> CAEXListFragment.newInstance()
            TAB_OPEN_INSPECTIONS -> OpenInspectionsFragment.newInstance()
            TAB_CLOSED_INSPECTIONS -> ClosedInspectionsFragment.newInstance()
            else -> throw IllegalArgumentException("Invalid tab position: $position")
        }
    }
}