package com.arn.scrobble.info

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.expandIfNeeded
import com.arn.scrobble.VMFactory
import com.arn.scrobble.databinding.ContentInfoBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.Track


class InfoFragment: BottomSheetDialogFragment() {

    private val viewModel by lazy { VMFactory.getVM(this, InfoVM::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = ContentInfoBinding.inflate(inflater, container, false)

        val artist = arguments!!.getString(NLService.B_ARTIST)!!
        val album = arguments!!.getString(NLService.B_ALBUM)
        val track = arguments!!.getString(NLService.B_TRACK)
        val username = arguments!!.getString(Stuff.ARG_USERNAME)
        val pkgName = arguments!!.getString(Stuff.ARG_PKG)

        val adapter = InfoAdapter(viewModel, this, username, pkgName)
        binding.infoList.layoutManager = LinearLayoutManager(context!!)
        binding.infoList.itemAnimator = null

//        val itemDecor = MaterialDividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
//        itemDecor.setDividerInsetStartResource(context!!, R.dimen.divider_inset)
//        itemDecor.setDividerInsetEndResource(context!!, R.dimen.divider_inset)
        // this puts divider after the last item

        val itemDecor = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        val colorDrawable = ColorDrawable(MaterialColors.getColor(context!!, R.attr.colorOutline, null))
        val inset = resources.getDimensionPixelSize(R.dimen.divider_inset)
        val insetDrawable = InsetDrawable(colorDrawable, inset, 0, inset, 0)
        itemDecor.setDrawable(insetDrawable)
        binding.infoList.addItemDecoration(itemDecor)
        binding.infoList.adapter = adapter
        if (viewModel.loadedTypes.isEmpty()) {
            if (!track.isNullOrEmpty())
                viewModel.info += NLService.B_TRACK to Track(track, null, artist)
            viewModel.info += NLService.B_ARTIST to Artist(artist, null)
            if (!album.isNullOrEmpty())
                viewModel.info += NLService.B_ALBUM to Album(album, null, artist)
            viewModel.loadInfo(artist, album, track, username)
        }
        adapter.notifyDataSetChanged()
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
        expandIfNeeded()
    }

    override fun onDestroyView() {
        viewModel.cancel()
        super.onDestroyView()
    }
}