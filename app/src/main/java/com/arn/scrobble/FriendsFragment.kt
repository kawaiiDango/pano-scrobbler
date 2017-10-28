package com.arn.scrobble

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arn.scrobble.ui.EndlessScrollListener
import kotlinx.android.synthetic.main.content_friends.*

/**
 * Created by arn on 09/07/2017.
 */

class FriendsFragment : Fragment() {

    private var adapter: FriendsAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.content_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FriendsAdapter(activity, R.layout.grid_item_friend)
        friends_grid.adapter = adapter
        friends_grid.setOnScrollListener(loadMoreListener)

        friends_swipe_refresh.setOnRefreshListener { adapter?.loadFriends(1) }
    }

    override fun onPause() {
        super.onPause()
        (friends_grid.adapter as FriendsAdapter?)
                ?.handler?.removeMessages(Stuff.RECENTS_REFRESH_INTERVAL.toInt())
    }

    override fun onResume() {
        super.onResume()
        Stuff.setTitle(activity, R.string.friends)
        adapter?.loadFriends(1)
    }

    private val loadMoreListener = object : EndlessScrollListener() {
        override fun onLoadMore(page: Int, totalItemsCount: Int): Boolean {
            return adapter?.loadFriends(page) ?: false
            // true ONLY if more data is actually being loaded; false otherwise.
        }
    }
}