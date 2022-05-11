package com.arn.scrobble.pending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.recents.PopupMenuUtils
import com.arn.scrobble.recents.TracksVM
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.UiUtils.setTitle

/**
 * Created by arn on 21/09/2017.
 */
class PendingScrFragment : Fragment(), ItemClickListener {
    private val adapter by lazy { PendingScrAdapter(this) }
    private val viewModel by viewModels<TracksVM>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.content_simple_list, container, false) as RecyclerView

        view.layoutManager = LinearLayoutManager(context!!)
        view.isNestedScrollingEnabled = false
        adapter.isShowingAlbums = MainPrefs(context!!).showAlbumInRecents
        view.adapter = adapter

        viewModel.loadPending(1000, false)
            .observe(viewLifecycleOwner) {
                it ?: return@observe
                adapter.clear()
                adapter.addAll(it)
            }
        return view
    }

    override fun onStart() {
        super.onStart()
        setTitle(R.string.pending_scrobbles)
    }

    override fun onItemClick(view: View, position: Int) {
        val p = adapter.getPending(position)
        if (view.id == R.id.recents_menu)
            PopupMenuUtils.openPendingPopupMenu((view.parent as ViewGroup).findViewById(R.id.date_frame),
                viewLifecycleOwner.lifecycleScope,
                p,
                {
                    adapter.remove(position)
                },
                {
                    viewModel.loadPending(1000, false)
                }
            )
    }
}