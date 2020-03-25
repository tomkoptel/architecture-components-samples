package com.example.android.navigationadvancedsample

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

internal class NavigationBarDelegate(
        private val fragment: Fragment,
        @param:IdRes private val navBarId: Int,
        @param:IdRes private val containerId: Int
) {
    fun onActivityCreated(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            setupNavigation()
        }
    }

    fun onRestoreInstanceState(nop: Bundle?) {
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupNavigation()
    }

    private fun setupNavigation() {
        val bottomNavigationView = fragment.view?.findViewById<BottomNavigationView>(navBarId)
        fragment.activity?.let { activity ->
            bottomNavigationView?.let { view ->
                if (activity is AppCompatActivity) {
                    val viewModel = ViewModelProvider(activity).get(NavigationViewModel::class.java)
                    viewModel.currentNavController = setupBottomNavigationBar(view, activity)
                } else {
                    throw IllegalStateException("The activity should be decendant from AppCompatActivity")
                }
            }
        }
    }

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigationBar(bottomNavigationView: BottomNavigationView, activity: AppCompatActivity): LiveData<NavController> {
        val navGraphIds = listOf(R.navigation.home, R.navigation.list, R.navigation.form)

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = bottomNavigationView.setupWithNavController(
                navGraphIds = navGraphIds,
                fragmentManager = activity.supportFragmentManager,
                containerId = containerId,
                intent = activity.intent
        )

        // Whenever the selected controller changes, setup the action bar.
        controller.observe(fragment.viewLifecycleOwner, Observer { navController ->
            activity.setupActionBarWithNavController(navController)
        })

        return controller
    }
}
