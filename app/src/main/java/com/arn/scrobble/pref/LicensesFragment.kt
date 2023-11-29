package com.arn.scrobble.pref

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.databinding.ContentLicensesBinding
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.google.android.material.transition.MaterialSharedAxis

class LicensesFragment : Fragment() {
    private var _binding: ContentLicensesBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<LicensesVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAxisTransitions(MaterialSharedAxis.X)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentLicensesBinding.inflate(inflater, container, false)
        binding.list.setupInsets()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()

        binding.list.layoutManager = LinearLayoutManager(context)
        val adapter = LicensesAdapter()
        binding.list.adapter = adapter

        adapter.submitList(viewModel.libraries)

        (view.parent as? ViewGroup)?.doOnPreDraw {
            startPostponedEnterTransition()
        }
    }
}