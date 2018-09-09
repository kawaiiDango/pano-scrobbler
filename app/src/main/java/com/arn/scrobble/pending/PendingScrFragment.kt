package com.arn.scrobble.pending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.TracksVM
import com.arn.scrobble.VMFactory
import kotlinx.android.synthetic.main.content_recents.view.*

/**
 * Created by arn on 21/09/2017.
 */
class PendingScrFragment: Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.content_recents, container, false)

        view.recents_list.layoutManager = LinearLayoutManager(context!!)
        view.recents_list.isNestedScrollingEnabled = false
        val adapter = PendingScrAdapter()
        view.recents_list.adapter = adapter
        view.recents_swipe_refresh.isEnabled = false

        VMFactory.getVM(this, TracksVM::class.java)
                .loadPending(1000)
                .observe(viewLifecycleOwner, Observer {
                    it ?: return@Observer
                    adapter.clear()
                    adapter.addAll(it.first)
                })
        return view
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.pending_scrobbles)
    }
}