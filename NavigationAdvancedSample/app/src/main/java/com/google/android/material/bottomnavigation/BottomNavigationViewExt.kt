@file:JvmName("BottomNavigationViewExt")

package com.google.android.material.bottomnavigation

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
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
    val activity = requireActivity()
    val application = activity.application
    val viewModel = NavigationViewModel(activity)
    val factory = SavedStateViewModelFactory(application, this)
    val bottomNavigationViewModel = ViewModelProvider(this, factory).get(BottomNavigationViewModel::class.java)
    
    val controller = BottomNavigationViewController(
        globalNavViewModel = viewModel,
        fragmentManager = requireActivity().supportFragmentManager,
        bottomNavigationView = bottomNavigationView,
        bottomNavigationViewModel = bottomNavigationViewModel
    )
    viewLifecycleOwner.lifecycle.addObserver(controller)

    return bottomNavigationView
}

internal class BottomNavigationViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    val selectedTab: Int
        get() = savedStateHandle.get<Int>("REGULAR_KEY") ?: 0

    fun saveSelectedTab(selectedId: Int) {
        savedStateHandle.set("REGULAR_KEY", selectedId)
    }
}

private class BottomNavigationViewController(
    private val globalNavViewModel: NavigationViewModel,
    private val fragmentManager: FragmentManager,
    private val bottomNavigationView: BottomNavigationView,
    private val bottomNavigationViewModel: BottomNavigationViewModel
) : LifecycleObserver {
    private var backStackChangedListener: BackStackChangedListener? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        restoreState()

        setOnNavigationItemSelectedListener()

        // FIXME: Handle deep link
//    setupDeepLinks(navGraphIds, fragmentManager, containerId, intent)

        // Optional: on item reselected, pop back stack to the destination of the graph
        setupItemReselected()

        // Finally, ensure that we update our BottomNavigationView when the back stack changes
        fixFragmentBackStack()
    }

    /**
     * We are working with unique instance of tab bar and thus we need a way to restore its state
     * the last time we had hosting fragment killed. It means that when we select
     * 'Tab A -> Tab B -> back' we do expect the navigation tab bar to retain its previously selected
     * state.
     */
    private fun restoreState() {
        val restoreSelectedState = restoreSelectedState()
        Log.d("BottomNavigation", addLogSuffix("selectedItemId=${bottomNavigationView.selectedItemId} restoreSelectedState=${restoreSelectedState}"))
        bottomNavigationView.selectedItemId = restoreSelectedState
        globalNavViewModel.currentSelectedTab = restoreSelectedState
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        cleanUp()
    }

    private fun restoreSelectedState(): Int {
        val restoredArg = bottomNavigationViewModel.selectedTab
        return if (restoredArg == 0) {
            globalNavViewModel.currentSelectedTab
        } else {
            restoredArg
        }
    }

    private fun setOnNavigationItemSelectedListener() {
        val navGraphIds = globalNavViewModel.navGraphIds

        // Now connect selecting an item with swapping Fragments
        var selectedItemTag = globalNavViewModel.findTag(globalNavViewModel.currentSelectedTab)
        val firstFragmentGraphId = navGraphIds.first()
        val firstFragmentTag = globalNavViewModel.findTag(firstFragmentGraphId)

        // When a navigation item is selected
        val previousItem = bottomNavigationView.selectedItemId
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            Log.d("BottomNavigation", addLogSuffix("item=${item} id=${item.itemId} previousItem=${previousItem}"))
            bottomNavigationViewModel.saveSelectedTab(previousItem)
            globalNavViewModel.currentSelectedTab = item.itemId

            // Don't do anything if the state is state has already been saved.
            if (fragmentManager.isStateSaved) {
                false
            } else {
                val newlySelectedItemTag = globalNavViewModel.findTag(item.itemId)
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
                                val navFragmentTags = globalNavViewModel.navFragmentTags
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
            val newlySelectedItemTag = globalNavViewModel.findTag(item.itemId)
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
        val changedListener = BackStackChangedListener(fragmentManager, globalNavViewModel, bottomNavigationView)
        fragmentManager.addOnBackStackChangedListener(changedListener)
        backStackChangedListener = changedListener
    }

    private fun cleanUp() {
        bottomNavigationView.setOnNavigationItemReselectedListener(null)
        bottomNavigationView.setOnNavigationItemSelectedListener(null)
        backStackChangedListener?.let { listener ->
            fragmentManager.removeOnBackStackChangedListener(listener)
            backStackChangedListener = null
        }
    }

    private fun addLogSuffix(log: String): String {
        return "\n\t bottomNavigationViewModel=${bottomNavigationViewModel} \n\t navigationViewModel=${globalNavViewModel} \n" +
            "\t bottomNavigationView=${bottomNavigationView} \n\t $log"
    }

    private class BackStackChangedListener(
        private val fragmentManager: FragmentManager,
        private val viewModel: NavigationViewModel,
        private val bottomNavigationView: BottomNavigationView
    ) : FragmentManager.OnBackStackChangedListener {
        override fun onBackStackChanged() {
            val selectedItemTag = viewModel.findTag(bottomNavigationView.selectedItemId)
            val firstFragmentGraphId = viewModel.navGraphIds.first()
            val firstFragmentTag = viewModel.findTag(firstFragmentGraphId)
            val isOnFirstFragment = selectedItemTag == firstFragmentTag

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
}
