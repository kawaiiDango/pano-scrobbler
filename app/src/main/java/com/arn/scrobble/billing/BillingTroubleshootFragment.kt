package com.arn.scrobble.billing


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.ExtrasConsts
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ContentBillingToubleshootBinding
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.google.android.material.transition.MaterialSharedAxis


class BillingTroubleshootFragment : Fragment() {

    private var _binding: ContentBillingToubleshootBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.X)

        _binding = ContentBillingToubleshootBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.setupInsets()

        binding.billingTroubleshootText.text = if (ExtrasConsts.isFossBuild)
            getString(R.string.billing_troubleshoot_github, 6, "October 2024")
        else
            getString(R.string.billing_troubleshoot)

        binding.close.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
