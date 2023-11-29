package com.arn.scrobble.ui

import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.friends.UserCached


data class MusicEntryLoaderInput(
    val user: UserCached,
    val timePeriod: TimePeriod?,
    val type: Int,
    val page: Int = -1,
    val entry: MusicEntry? = null,
    val cacheBuster: Int = 0
) {
    fun copyCacheBusted(
        user: UserCached = this.user,
        timePeriod: TimePeriod? = this.timePeriod,
        type: Int = this.type,
        page: Int = this.page,
        entry: MusicEntry? = this.entry
    ) = copy(
        cacheBuster = cacheBuster + 1,
        user = user,
        timePeriod = timePeriod,
        type = type,
        page = page,
        entry = entry
    )
}