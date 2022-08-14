package com.arn.scrobble.recents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.arn.scrobble.databinding.ContentScrobblesBinding
import com.arn.scrobble.ui.ItemClickListener

class ScrobblesFragment : Fragment(), ItemClickListener {

    private var _binding: ContentScrobblesBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<ScrobblesVM>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentScrobblesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onItemClick(view: View, position: Int) {
        TODO("Not yet implemented")
    }
}