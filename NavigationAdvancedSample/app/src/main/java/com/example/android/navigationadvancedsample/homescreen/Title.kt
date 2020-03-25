/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.navigationadvancedsample.homescreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.android.navigationadvancedsample.NavigationBarDelegate
import com.example.android.navigationadvancedsample.R

/**
 * Shows the main title screen with a button that navigates to [About].
 */
class Title : Fragment() {
//    private val delegate = NavigationBarDelegate(
//            fragment = this,
//            navBarId = R.id.bottom_nav,
//            containerId = R.id.nav_host_container
//    )
//
//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//        delegate.onActivityCreated(savedInstanceState)
//    }
//
////    override fun onViewStateRestored(savedInstanceState: Bundle?) {
////        super.onViewStateRestored(savedInstanceState)
////        delegate.onRestoreInstanceState(savedInstanceState)
////    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_title, container, false)

        view.findViewById<Button>(R.id.about_btn).setOnClickListener {
            findNavController().navigate(R.id.action_title_to_about)
        }
        return view
    }
}
