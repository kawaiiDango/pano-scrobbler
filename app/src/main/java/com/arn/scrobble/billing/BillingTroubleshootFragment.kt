package com.arn.scrobble.billing


import android.os.Bundle
import android.transition.Fade
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ContentBillingToubleshootBinding


class BillingTroubleshootFragment : Fragment() {

    private var _binding : ContentBillingToubleshootBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Fade()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = ContentBillingToubleshootBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, getString(R.string.billing_troubleshoot_title))

    }
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
