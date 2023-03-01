package com.arn.scrobble


import com.arn.scrobble.charts.ChartsOverviewFragment
import com.arn.scrobble.friends.FriendsFragment
import com.arn.scrobble.recents.ScrobblesFragment

class HomePagerAdapter(fragment: BasePagerFragment) : BasePagerAdapter(fragment) {
    override val tabMetadata = listOf(
        TabMetadata(R.string.scrobbles, R.drawable.vd_history) { ScrobblesFragment() },
        TabMetadata(R.string.friends, R.drawable.vd_friends) { FriendsFragment() },
        TabMetadata(R.string.charts, R.drawable.vd_charts) { ChartsOverviewFragment() },
    )
}