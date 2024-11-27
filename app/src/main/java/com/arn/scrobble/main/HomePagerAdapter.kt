package com.arn.scrobble.main


import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.charts.ChartsOverviewFragment
import com.arn.scrobble.recents.ScrobblesFragment
import com.arn.scrobble.recents.ScrobblesFragmentArgs

class HomePagerAdapter(fragment: BasePagerFragment, accountType: AccountType) :
    BasePagerAdapter(fragment) {
    override val tabMetadata = when (accountType) {
        AccountType.LASTFM,
        AccountType.LISTENBRAINZ,
        AccountType.CUSTOM_LISTENBRAINZ,
            ->
            listOf(
                TabMetadata(R.string.scrobbles, R.drawable.vd_history) {
                    ScrobblesFragment()
                        .apply { arguments = ScrobblesFragmentArgs().toBundle() }
                },
                TabMetadata(R.string.charts, R.drawable.vd_charts) { ChartsOverviewFragment() },
            )

        AccountType.LIBREFM,
        AccountType.GNUFM,
            -> listOf(
            TabMetadata(R.string.scrobbles, R.drawable.vd_history) {
                ScrobblesFragment()
                    .apply { arguments = ScrobblesFragmentArgs().toBundle() }
            },
            TabMetadata(R.string.charts, R.drawable.vd_charts) { ChartsOverviewFragment() },
        )

        AccountType.MALOJA,
        AccountType.PLEROMA,
        AccountType.FILE,
            -> listOf(
            TabMetadata(R.string.scrobbles, R.drawable.vd_history) {
                val args = ScrobblesFragmentArgs(
                    showChips = false,
                    showAllMenuItems = false
                ).toBundle()
                ScrobblesFragment().apply { arguments = args }
            },
        )

//        AccountType.FILE -> listOf(
//            TabMetadata(R.string.scrobble_to_file, R.drawable.vd_file) { FileInfoFragment() },
//        )
    }
}