package androidx.fragment.app

import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI

internal fun Fragment.setupWithNavController(toolbar: Toolbar): Toolbar {
    val navHostFragment = NavHostFragment.findNavController(this)
    NavigationUI.setupWithNavController(toolbar, navHostFragment)
    return toolbar
}
