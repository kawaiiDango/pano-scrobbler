package com.arn.scrobble

import android.app.Fragment
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.arn.scrobble.db.PendingScrobblesDb

/**
 * Created by arn on 21/09/2017.
 */
class PendingFragment: Fragment(){
    private var list: ListView? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (savedInstanceState == null)
            setHasOptionsMenu(false)
        val vg = activity.findViewById<AppBarLayout>(R.id.app_bar)
        vg?.setExpanded(false, true)

        val ab = (activity as AppCompatActivity).supportActionBar
        ab?.setDisplayHomeAsUpEnabled(true)

        val view = inflater.inflate(R.layout.content_main, container, false)
        val list = view.findViewById<ListView>(R.id.recents_list)
        list.choiceMode = ListView.CHOICE_MODE_NONE
        list.isNestedScrollingEnabled = false
        val adapter = PendingScroblesAdapter(activity, R.layout.list_item_recents)
        list.adapter = adapter
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
        refresh.isEnabled = false

        this.list = list
        return view
    }

    override fun onResume() {
        super.onResume()
        Thread{
            val adapter = list?.adapter as PendingScroblesAdapter? ?: return@Thread
            val dao = PendingScrobblesDb.getDb(activity).getDao()
            val allPending = dao.all(1000)
            list?.post {
                adapter.clear()
                adapter.addAll(allPending)
            }
        }.start()
        Stuff.setTitle(activity, R.string.pending_scrobbles)
    }
}