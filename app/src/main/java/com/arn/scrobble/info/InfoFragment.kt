package com.arn.scrobble.info

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.VMFactory
import com.arn.scrobble.databinding.ContentInfoBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.Track


class InfoFragment: BottomSheetDialogFragment() {

    private val viewModel by lazy { VMFactory.getVM(this, InfoVM::class.java) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val width = resources.getDimension(R.dimen.bottom_sheet_width)
            if (width > 0)
                dialog.window!!.setLayout(width.toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = ContentInfoBinding.inflate(inflater, container, false)

        val artist = arguments!!.getString(NLService.B_ARTIST)!!
        val album = arguments!!.getString(NLService.B_ALBUM)
        val track = arguments!!.getString(NLService.B_TITLE)
        val username = arguments!!.getString(Stuff.ARG_USERNAME)

        val adapter = InfoAdapter(viewModel, this, username)
        binding.infoList.layoutManager = LinearLayoutManager(context!!)
        (binding.infoList.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false

        val itemDecor = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        itemDecor.setDrawable(ColorDrawable(ContextCompat.getColor(context!!, R.color.lightInfoDivider)))
        binding.infoList.addItemDecoration(itemDecor)
        binding.infoList.adapter = adapter
        adapter.notifyDataSetChanged()
        if (viewModel.loadedTypes.isEmpty()) {
            if (!track.isNullOrEmpty())
                viewModel.info += NLService.B_TITLE to Track(track, null, artist)
            viewModel.info += NLService.B_ARTIST to Artist(artist, null)
            if (!album.isNullOrEmpty())
                viewModel.info += NLService.B_ALBUM to Album(album, null, artist)
            viewModel.loadInfo(activity!!, artist, album, track, username)
        }
        viewModel.receiver.observe(viewLifecycleOwner) {
            it ?: return@observe
            viewModel.loadedTypes += it.first
            val idx = viewModel.info.indexOfFirst { p ->
                p.first == it.first
            }
            if (it.second == null) {
                if (idx > -1)
                    adapter.notifyItemChanged(idx)
                return@observe
            } else {
                if (idx > -1) {
                    viewModel.info[idx] = it
                    adapter.notifyItemChanged(idx)
                } else if (it.first == NLService.B_ALBUM_ARTIST) {
                    var insertPos = viewModel.info.indexOfFirst { p ->
                        p.first == NLService.B_ALBUM
                    }
                    if (insertPos < 0)
                        insertPos = viewModel.info.size
                    viewModel.info.add(insertPos, it)
                    adapter.notifyItemRangeChanged(viewModel.info.size - 2, 2)
                } else {
                    viewModel.info += it
                    adapter.notifyItemChanged(viewModel.info.size - 1)
                }
            }
            synchronized(viewModel.receiver) {
                viewModel.receiver.value = null
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (view?.isInTouchMode == false) {
            val bottomSheetView = dialog!!.window!!.decorView.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            BottomSheetBehavior.from(bottomSheetView).state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        viewModel.cancel()
        super.onDestroyView()
    }
}