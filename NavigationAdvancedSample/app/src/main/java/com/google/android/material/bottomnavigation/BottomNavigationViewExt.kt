@file:JvmName("BottomNavigationViewExt")

package com.google.android.material.bottomnavigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.android.navigationadvancedsample.NavigationViewModel
import com.example.android.navigationadvancedsample.R

/**
 * Manages the various graphs needed for a [BottomNavigationView].
 *
 * This sample is a workaround until the Navigation Component supports multiple back stacks.
 */
internal fun Fragment.configureNavController(
    bottomNavigationView: BottomNavigationView
): BottomNavigationView {
    val viewModel = NavigationViewModel(requireActivity())
    val controller = BottomNavigationViewController(
        viewModel = viewModel,
        fragmentManager = requireActivity().supportFragmentManager,
        bottomNavigationView = bottomNavigationView
    )
    viewLifecycleOwner.lifecycle.addObserver(controller)
    return bottomNavigationView
}

private class BottomNavigationViewController(
    private val viewModel: NavigationViewModel,
    private val fragmentManager: FragmentManager,
    private val bottomNavigationView: BottomNavigationView
) : LifecycleObserver {
    private var addOnBackStackChangedListener: () -> Unit = {}

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        setOnNavigationItemSelectedListener()

        // FIXME: Handle deep link
//    setupDeepLinks(navGraphIds, fragmentManager, containerId, intent)

        // Optional: on item reselected, pop back stack to the destination of the graph
        setupItemReselected()

        // Finally, ensure that we update our BottomNavigationView when the back stack changes
        fixFragmentBackStack()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        cleanUp()
    }

    private fun setOnNavigationItemSelectedListener() {
        val navGraphIds = viewModel.navGraphIds

        // Now connect selecting an item with swapping Fragments
        var selectedItemTag = viewModel.findTag(viewModel.currentSelectedTab)
        val firstFragmentGraphId = navGraphIds.first()
        val firstFragmentTag = viewModel.findTag(firstFragmentGraphId)

        // When a navigation item is selected
        bottomNavigationView.selectedItemId = viewModel.currentSelectedTab
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            viewModel.currentSelectedTab = item.itemId

            // Don't do anything if the state is state has already been saved.
            if (fragmentManager.isStateSaved) {
                false
            } else {
                val newlySelectedItemTag = viewModel.findTag(item.itemId)
                if (selectedItemTag != newlySelectedItemTag) {
                    // Pop everything above the first fragment (the "fixed start destination")
                    fragmentManager.popBackStack(firstFragmentTag,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    val selectedFragment = fragmentManager.findFragmentByTag(newlySelectedItemTag)
                        as NavHostFragment

                    // Exclude the first fragment tag because it's always in the back stack.
                    if (firstFragmentTag != newlySelectedItemTag) {
                        // Commit a transaction that cleans the back stack and adds the first fragment
                        // to it, creating the fixed started destination.
                        fragmentManager.beginTransaction()
                            .setCustomAnimations(
                                R.anim.nav_default_enter_anim,
                                R.anim.nav_default_exit_anim,
                                R.anim.nav_default_pop_enter_anim,
                                R.anim.nav_default_pop_exit_anim)
                            .attach(selectedFragment)
                            .setPrimaryNavigationFragment(selectedFragment)
                            .apply {
                                val navFragmentTags = viewModel.navFragmentTags
                                // Detach all other Fragments
                                navFragmentTags.forEach { fragmentTagIter ->
                                    if (fragmentTagIter != newlySelectedItemTag) {
                                        detach(fragmentManager.findFragmentByTag(firstFragmentTag)!!)
                                    }
                                }
                            }
                            .addToBackStack(firstFragmentTag)
                            .setReorderingAllowed(true)
                            .commit()
                    }
                    selectedItemTag = newlySelectedItemTag
                    bottomNavigationView.setTag(R.id.navControllerViewTag, selectedFragment.navController)
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun setupItemReselected() {
        bottomNavigationView.setOnNavigationItemReselectedListener { item ->
            val newlySelectedItemTag = viewModel.findTag(item.itemId)
            val selectedFragment =
                fragmentManager.findFragmentByTag(newlySelectedItemTag) as NavHostFragment
            val navController = selectedFragment.navController
            // Pop the back stack to the start destination of the current navController graph
            navController.popBackStack(
                navController.graph.startDestination, false
            )
        }
    }

    private fun fixFragmentBackStack() {
        val selectedItemTag = viewModel.findTag(bottomNavigationView.selectedItemId)
        val firstFragmentGraphId = viewModel.navGraphIds.first()
        val firstFragmentTag = viewModel.findTag(firstFragmentGraphId)
        val isOnFirstFragment = selectedItemTag == firstFragmentTag

        addOnBackStackChangedListener = {
            if (!isOnFirstFragment && !fragmentManager.isOnBackStack(firstFragmentTag)) {
                bottomNavigationView.selectedItemId = firstFragmentGraphId
            }

            // Reset the graph if the currentDestination is not valid (happens when the back
            // stack is popped after using the back button).
            val selectedNavController = bottomNavigationView.getTag(R.id.navControllerViewTag)
            if (selectedNavController is NavController) {
                if (selectedNavController.currentDestination == null) {
                    selectedNavController.navigate(selectedNavController.graph.id)
                }
            }
        }
        fragmentManager.addOnBackStackChangedListener(addOnBackStackChangedListener)
    }

    private fun cleanUp() {
        bottomNavigationView.setOnNavigationItemReselectedListener(null)
        bottomNavigationView.setOnNavigationItemSelectedListener(null)
        fragmentManager.removeOnBackStackChangedListener(addOnBackStackChangedListener)
    }

    private fun FragmentManager.isOnBackStack(backStackName: String): Boolean {
        val backStackCount = backStackEntryCount
        for (index in 0 until backStackCount) {
            if (getBackStackEntryAt(index).name == backStackName) {
                return true
            }
        }
        return false
    }
}
