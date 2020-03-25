package com.example.android.navigationadvancedsample

import android.util.SparseArray
import androidx.core.util.forEach
import androidx.core.util.set
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController

class NavigationViewModel : ViewModel() {
    private val graphIdToTagMap = SparseArray<String>()

    val navGraphIds: List<Int> = listOf(R.navigation.home, R.navigation.list, R.navigation.form)
    var currentSelectedTab: Int = R.id.home
    var currentNavController: LiveData<NavController>? = null

    fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    fun findTag(key: Int) : String = checkNotNull(graphIdToTagMap[key]) {
        "We have issue in registering tag for provided key=$key."
    }

    val navFragmentTags: Collection<String> get() {
        val list = mutableListOf<String>()
        graphIdToTagMap.forEach { _, value -> list.add(value) }
        return list
    }

    fun registerTag(key: Int, tag: String) {
        graphIdToTagMap[key] = tag
    }

    companion object {
        operator fun invoke(activity: FragmentActivity): NavigationViewModel {
            return ViewModelProvider(activity).get(NavigationViewModel::class.java)
        }
    }
}
