package com.arn.scrobble.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ContentLoadingBinding
import com.arn.scrobble.friends.UserCached.Companion.toUserCached
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.Stuff.putSingle
import com.google.android.material.transition.MaterialSharedAxis

class LoadingFragment : Fragment() {
    private var _binding: ContentLoadingBinding? = null
    val binding get() = _binding!!

    private val viewModel by viewModels<UserLoaderVM>()
    private val args by navArgs<LoadingFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.Z)

        _binding = ContentLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        collectLatestLifecycleFlow(viewModel.userInfo) { user ->
            findNavController().navigate(
                R.id.action_loadingFragment_to_othersHomePagerFragment,
                Bundle().putSingle(user.toUserCached())
            )
        }

        // opened www.last.fm link
        binding.loadingUsername.text = args.lastfmUsername
        viewModel.fetchUserInfo(args.lastfmUsername!!)
    }
}