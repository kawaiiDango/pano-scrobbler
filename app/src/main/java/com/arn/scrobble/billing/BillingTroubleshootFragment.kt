package com.arn.scrobble.billing


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arn.scrobble.databinding.ContentBillingToubleshootBinding
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.google.android.material.transition.MaterialSharedAxis


class BillingTroubleshootFragment : Fragment() {

    private var _binding: ContentBillingToubleshootBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentBillingToubleshootBinding.inflate(inflater, container, false)
        binding.root.setupInsets()

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
