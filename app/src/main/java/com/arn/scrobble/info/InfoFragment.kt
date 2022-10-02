package com.arn.scrobble.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ContentInfoBinding
import com.arn.scrobble.ui.UiUtils.expandIfNeeded
import com.arn.scrobble.ui.UiUtils.scheduleTransition
import com.arn.scrobble.ui.UiUtils.startFadeLoop
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.divider.MaterialDividerItemDecoration


class InfoFragment : BottomSheetDialogFragment() {

    private val viewModel by viewModels<InfoVM>()
    private val activityViewModel by activityViewModels<MainNotifierViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = ContentInfoBinding.inflate(inflater, container, false)

        val artist = arguments!!.getString(NLService.B_ARTIST)!!
        val album = arguments!!.getString(NLService.B_ALBUM)
        val track = arguments!!.getString(NLService.B_TRACK)
        val username = activityViewModel.currentUser.name
        val pkgName = arguments!!.getString(Stuff.ARG_PKG)

        val adapter = InfoAdapter(viewModel, activityViewModel, this, pkgName)
        binding.infoList.layoutManager = LinearLayoutManager(context!!)
        binding.infoList.itemAnimator = null

        val itemDecor =
            MaterialDividerItemDecoration(context!!, DividerItemDecoration.VERTICAL).apply {
                setDividerInsetStartResource(context!!, R.dimen.divider_inset)
                setDividerInsetEndResource(context!!, R.dimen.divider_inset)
                isLastItemDecorated = false
            }

        binding.infoList.addItemDecoration(itemDecor)
        binding.infoList.adapter = adapter

        if (viewModel.infoMap.isEmpty()) {
            binding.root.startFadeLoop()
            viewModel.loadInfo(artist, album, track, username)
        }

        viewModel.infoMapReceiver.observe(viewLifecycleOwner) {
            it ?: return@observe
            viewModel.infoMap.putAll(it)
            binding.root.clearAnimation()
            scheduleTransition()
            adapter.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        expandIfNeeded()
    }
}