/*
 * Copyright 2024 The Android Open Source Project
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

package com.arn.scrobble.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.reflect.getDeclaredComposableMethod
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.compose.LocalFragment
import com.arn.scrobble.utils.UiUtils.expandIfNeeded
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * This class provides a [Fragment] wrapper around a composable function that is loaded via
 * reflection. The composable function has access to this fragment instance via [LocalFragment].
 *
 * This class is constructed via a factory method: make sure you add `import
 * androidx.navigation.fragment.compose.ComposableFragment.Companion.ComposableFragment`
 */
class ComposableBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val composableMethod by lazy {
        val arguments = requireArguments()
        val fullyQualifiedName =
            checkNotNull(arguments.getString(ARG)) {
                "Instances of ComposableFragment must be created with the factory function " +
                        "ComposableFragment(fullyQualifiedName)"
            }
        val (className, methodName) = fullyQualifiedName.split("$")
        val clazz = Class.forName(className)
        clazz.getDeclaredComposableMethod(methodName)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Consider using Fragment.content from fragment-compose once it is stable
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CompositionLocalProvider(LocalFragment provides this@ComposableBottomSheetDialogFragment) {
                    composableMethod.invoke(currentComposer, null)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            expandIfNeeded(it)
        }
    }

    companion object {
        internal const val ARG = "composable"

        /**
         * Creates a new [ComposableBottomSheetDialogFragment] instance that will wrap the Composable method loaded
         * via reflection from [fullyQualifiedName].
         *
         * @param fullyQualifiedName the fully qualified name of the static, no argument Composable
         *   method that this fragment should display. It should be formatted in the format
         *   `com.example.NameOfFileKt/$MethodName`.
         */
        @JvmStatic
        fun ComposableFragment(fullyQualifiedName: String): ComposableBottomSheetDialogFragment {
            return ComposableBottomSheetDialogFragment().apply {
                arguments = bundleOf(ARG to fullyQualifiedName)
            }
        }
    }
}
