package com.arn.scrobble.scrobbleable

import com.arn.scrobble.LastfmUnscrobbler
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.mapConcurrently
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.friends.UserSerializable
import de.umass.lastfm.Caller
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import de.umass.lastfm.User
import kotlinx.coroutines.supervisorScope

class Lastfm(userAccount: UserAccountSerializable) : GnuFm(userAccount) {
    override val apiKey: String = Stuff.LAST_KEY
    override val secret: String = Stuff.LAST_SECRET

    override suspend fun delete(track: Track): Boolean {
        val unscrobbler = LastfmUnscrobbler()
        val success = unscrobbler.haveCsrfCookie() &&
                unscrobbler.unscrobble(track.artist, track.name, track.playedWhen.time)
        return success
    }

    override suspend fun getFriends(page: Int, usernamep: String?): PaginatedResult<User> {
        val _session = sessionCopy()
        _session.cacheStrategy = if (Stuff.isOnline)
            Caller.CacheStrategy.NETWORK_ONLY
        else
            Caller.CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED

        val username = usernamep ?: userAccount.user.name
        return User.getFriends(username, page, 30, _session)
    }

    override suspend fun getListeningActivity(
        timePeriod: TimePeriod,
        user: UserSerializable?,
        cacheStrategy: Caller.CacheStrategy
    ): Map<TimePeriod, Int> {
        val _session = sessionCopy()
        _session.cacheStrategy = cacheStrategy

        val username = user?.name ?: userAccount.user.name
        val registeredTime = user?.registeredTime ?: userAccount.user.registeredTime

        val timePeriods = TimePeriodsGenerator.getScrobblingActivityPeriods(
            timePeriod,
            registeredTime
        )

        val periodCountsMap = mutableMapOf<TimePeriod, Int>()
        timePeriods.forEach { periodCountsMap[it] = 0 }

        supervisorScope {
            timePeriods.mapConcurrently(5) {
                if (it.start < System.currentTimeMillis()) {
                    kotlin.runCatching {
                        val pr = User.getRecentTracks(
                            username,
                            1,
                            1,
                            false,
                            it.start / 1000,
                            it.end / 1000,
                            _session
                        )
                        periodCountsMap[it] = pr.total
                    }
                }
            }
        }
        return periodCountsMap
    }
}