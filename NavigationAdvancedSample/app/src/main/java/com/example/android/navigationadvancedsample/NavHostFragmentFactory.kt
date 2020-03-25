package com.example.android.navigationadvancedsample

import android.util.Log
import android.util.SparseArray
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.set
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.android.navigationadvancedsample.navigation.attachNavHostFragment
import com.example.android.navigationadvancedsample.navigation.detachNavHostFragment

class NavHostFragmentFactory (
        private val target: AppCompatActivity,
        private val navigationViewModel: NavigationViewModel,
        private val navGraphIds: List<Int>,
        @param:IdRes private val containerId: Int
) {
    fun onCreate() {
        val graphIdToTagMap = SparseArray<String>()

        navGraphIds.forEachIndexed { index, navGraphId ->
            val fragmentTag = getFragmentTag(index)

            // Find or create the Navigation host fragment
            val navHostFragment = obtainNavHostFragment(
                target.supportFragmentManager,
                fragmentTag,
                navGraphId,
                containerId
            )
            val graphId = navHostFragment.navController.graph.id
            graphIdToTagMap[graphId] = fragmentTag

            // Attach or detach nav host fragment depending on whether it's the selected item.
            val firstFragment = index == 0
            if (firstFragment) {
//                selectedNavController.value = navHostFragment.navController
                val isPrimaryNavFragment = index == 0
                Log.d("NavigationExtensions", "isPrimaryNavFragment=$isPrimaryNavFragment")
                attachNavHostFragment(target.supportFragmentManager, navHostFragment, isPrimaryNavFragment)
            } else {
                detachNavHostFragment(target.supportFragmentManager, navHostFragment)
            }
        }
    }

    private fun obtainNavHostFragment(
            fragmentManager: FragmentManager,
            fragmentTag: String,
            navGraphId: Int,
            containerId: Int
    ): NavHostFragment {
        // If the Nav Host fragment exists, return it
        val existingFragment = fragmentManager.findFragmentByTag(fragmentTag) as NavHostFragment?
        existingFragment?.let { return it }

        // Otherwise, create it and return it.
        val navHostFragment = NavHostFragment.create(navGraphId)
        fragmentManager.beginTransaction()
                .add(containerId, navHostFragment, fragmentTag)
                .commitNow()
        return navHostFragment
    }

    private fun getFragmentTag(index: Int) = "bottomNavigation#$index"
}
