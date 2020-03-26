package com.example.android.navigationadvancedsample

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import com.example.android.navigationadvancedsample.navigation.attachNavHostFragment
import com.example.android.navigationadvancedsample.navigation.detachNavHostFragment

internal fun FragmentActivity.setUpMultipleNavBackStacks(@IdRes containerId: Int) {
    val viewModel = NavigationViewModel(this)
    val navGraphIds = viewModel.navGraphIds

    navGraphIds.forEachIndexed { index, navGraphId ->
        val fragmentTag = getFragmentTag(index)

        // Find or create the Navigation host fragment
        val navHostFragment = obtainNavHostFragment(
            supportFragmentManager,
            fragmentTag,
            navGraphId,
            containerId
        )
        val navController = navHostFragment.navController
        val graphId = navController.graph.id
        viewModel.registerTag(key = graphId, tag = fragmentTag)
        viewModel.registerTag(key = navGraphId, tag = fragmentTag)

        // Attach or detach nav host fragment depending on whether it's the selected item.
        val isPrimaryNavFragment = index == 0
        if (isPrimaryNavFragment) {
            attachNavHostFragment(supportFragmentManager, navHostFragment, true)
        } else {
            detachNavHostFragment(supportFragmentManager, navHostFragment)
        }
    }
}

internal fun FragmentActivity.handleDeepLinks(@IdRes containerId: Int) {
    val viewModel = NavigationViewModel(this)
    val navGraphIds = viewModel.navGraphIds

    val map = mutableMapOf<NavHostFragment, Boolean>()
    navGraphIds.forEachIndexed { index, navGraphId ->
        val fragmentTag = getFragmentTag(index)
        val navHostFragment = obtainNavHostFragment(
            supportFragmentManager,
            fragmentTag,
            navGraphId,
            containerId
        )
        val wasHandled = navHostFragment.navController.handleDeepLink(intent)
        map.put(navHostFragment, wasHandled)
    }

    val wasHandled = map.values.reduce { a, b -> a || b }
    if (wasHandled) {
        map.forEach { (navHostFragment, wasHandled) ->
            val navController = navHostFragment.navController
            if (wasHandled) {
                viewModel.currentSelectedTab = navController.graph.id
                attachNavHostFragment(supportFragmentManager, navHostFragment, true)
            } else {
                detachNavHostFragment(supportFragmentManager, navHostFragment)
            }
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
