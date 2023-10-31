package com.arn.scrobble

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.arn.scrobble.Stuff.putSingle
import com.arn.scrobble.databinding.ContentLoadingBinding
import com.arn.scrobble.friends.UserSerializable.Companion.toUserSerializable
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.google.android.material.transition.MaterialSharedAxis

class LoadingFragment : Fragment() {
    var _binding: ContentLoadingBinding? = null
    val binding get() = _binding!!

    private val viewModel by viewModels<UserLoaderVM>()
    private val args by navArgs<LoadingFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAxisTransitions(MaterialSharedAxis.Z)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewModel.userInfo.observe(viewLifecycleOwner) { user ->
            findNavController().navigate(R.id.action_loadingFragment_to_othersHomePagerFragment, Bundle().putSingle(user.toUserSerializable()))
        }

        if (args.lastfmUsername != null) {
            // opened www.last.fm link
            binding.loadingUsername.text = args.lastfmUsername
            viewModel.fetchUserInfo(args.lastfmUsername!!)
        }
    }
}