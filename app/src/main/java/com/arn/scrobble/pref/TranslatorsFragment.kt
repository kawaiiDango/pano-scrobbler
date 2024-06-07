package com.arn.scrobble.pref

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.databinding.ContentTranslatorsBinding
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.google.android.material.transition.MaterialSharedAxis

class TranslatorsFragment : Fragment() {
    private var _binding: ContentTranslatorsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<TranslatorsVM>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.X)

        _binding = ContentTranslatorsBinding.inflate(inflater, container, false)
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
        val adapter = TranslatorsAdapter()
        binding.list.adapter = adapter

        adapter.submitList(viewModel.translators) {
            (view.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }
    }
}