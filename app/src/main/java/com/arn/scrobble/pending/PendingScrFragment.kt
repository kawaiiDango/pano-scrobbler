package com.arn.scrobble.pending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.TracksVM
import com.arn.scrobble.VMFactory
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.PendingMenu

/**
 * Created by arn on 21/09/2017.
 */
class PendingScrFragment: Fragment(), ItemClickListener {
    private lateinit var adapter: PendingScrAdapter
    private lateinit var vm: TracksVM

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.content_simple_list, container, false) as RecyclerView

        view.layoutManager = LinearLayoutManager(context!!)
        view.isNestedScrollingEnabled = false
        adapter = PendingScrAdapter(this)
        view.adapter = adapter

        vm = VMFactory.getVM(this, TracksVM::class.java)
        vm.loadPending(1000, false)
                .observe(viewLifecycleOwner, {
                    it ?: return@observe
                    adapter.clear()
                    adapter.addAll(it)
                })
        return view
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.pending_scrobbles)
    }

    override fun onItemClick(view: View, position: Int) {
        val p = adapter.getPending(position)
        if (view.id == R.id.recents_menu)
            PendingMenu.openPendingPopupMenu((view.parent as ViewGroup).findViewById(R.id.date_frame), p,
                    {
                    activity?.runOnUiThread { adapter.remove(position) }
                    },
                    {
                        vm.loadPending(1000, false)
                    }
            )
    }
}