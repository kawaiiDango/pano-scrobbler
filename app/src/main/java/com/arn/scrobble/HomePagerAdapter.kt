package com.arn.scrobble


import com.arn.scrobble.api.AccountType
import com.arn.scrobble.charts.ChartsOverviewFragment
import com.arn.scrobble.friends.FriendsFragment
import com.arn.scrobble.recents.ScrobblesFragment
import com.arn.scrobble.recents.ScrobblesFragmentArgs

class HomePagerAdapter(fragment: BasePagerFragment, accountType: AccountType) :
    BasePagerAdapter(fragment) {
    override val tabMetadata = when (accountType) {
        AccountType.LASTFM,
        AccountType.LISTENBRAINZ,
        AccountType.CUSTOM_LISTENBRAINZ ->
            listOf(
                TabMetadata(R.string.scrobbles, R.drawable.vd_history) {
                    ScrobblesFragment()
                        .apply { arguments = ScrobblesFragmentArgs.Builder().build().toBundle() }
                },
                TabMetadata(R.string.friends, R.drawable.vd_friends) { FriendsFragment() },
                TabMetadata(R.string.charts, R.drawable.vd_charts) { ChartsOverviewFragment() },
            )

        AccountType.LIBREFM,
        AccountType.GNUFM -> listOf(
            TabMetadata(R.string.scrobbles, R.drawable.vd_history) {
                ScrobblesFragment()
                    .apply { arguments = ScrobblesFragmentArgs.Builder().build().toBundle() }
            },
            TabMetadata(R.string.charts, R.drawable.vd_charts) { ChartsOverviewFragment() },
        )

        AccountType.MALOJA,
        AccountType.PLEROMA,
        AccountType.FILE -> listOf(
            TabMetadata(R.string.scrobbles, R.drawable.vd_history) {
                val args = ScrobblesFragmentArgs.Builder()
                    .setShowChips(false)
                    .setShowAllMenuItems(false)
                    .build()
                    .toBundle()
                ScrobblesFragment().apply { arguments = args }
            },
        )

//        AccountType.FILE -> listOf(
//            TabMetadata(R.string.scrobble_to_file, R.drawable.vd_file) { FileInfoFragment() },
//        )
    }
}