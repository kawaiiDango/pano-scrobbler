package com.arn.scrobble.pref

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arn.scrobble.databinding.ContentDeleteAccountBinding
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.google.android.material.transition.MaterialSharedAxis

class DeleteAccountFragment : Fragment() {
    private var _binding: ContentDeleteAccountBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupAxisTransitions(MaterialSharedAxis.X)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ContentDeleteAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arrayOf(
            binding.link1, binding.link2, binding.link3
        ).forEach {
            it.movementMethod = LinkMovementMethod.getInstance()
        }
    }
}