package com.arn.scrobble

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.arn.scrobble.db.PendingScrobblesDb
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.content_main.view.*

/**
 * Created by arn on 21/09/2017.
 */
class PendingFragment: Fragment(){
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        val view = inflater.inflate(R.layout.content_main, container, false)
        view.recents_list.choiceMode = ListView.CHOICE_MODE_NONE
        view.recents_list.isNestedScrollingEnabled = false
        val adapter = PendingScroblesAdapter(activity, R.layout.list_item_recents)
        view.recents_list.adapter = adapter
        view.recents_swipe_refresh.isEnabled = false

        return view
    }

    override fun onResume() {
        super.onResume()
        Thread{
            val adapter = recents_list?.adapter as PendingScroblesAdapter? ?: return@Thread
            val dao = PendingScrobblesDb.getDb(activity).getDao()
            val allPending = dao.all(1000)
            recents_list?.post {
                adapter.clear()
                adapter.addAll(allPending)
            }
        }.start()
        Stuff.setTitle(activity, R.string.pending_scrobbles)
    }
}