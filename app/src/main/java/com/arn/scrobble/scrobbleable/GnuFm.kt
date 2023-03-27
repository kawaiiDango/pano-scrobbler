package com.arn.scrobble.scrobbleable

import com.arn.scrobble.App
import com.arn.scrobble.DrawerData
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.setMidnight
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.friends.UserSerializable
import com.arn.scrobble.pref.MainPrefs
import de.umass.lastfm.Caller
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Library
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Session
import de.umass.lastfm.Track
import de.umass.lastfm.User
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import java.util.Calendar

open class GnuFm(userAccount: UserAccountSerializable) : Scrobblable(userAccount) {

    open val apiKey: String = Stuff.LIBREFM_KEY
    open val secret: String = Stuff.LIBREFM_KEY

    private val session: Session by lazy {
        sessionCopy()
    }

    fun sessionCopy() = Session.createCustomRootSession(
        userAccount.apiRoot,
        apiKey,
        secret,
        userAccount.authKey
    )!!.also { it.isTlsNoVerify = userAccount.tlsNoVerify }

    override suspend fun updateNowPlaying(scrobbleData: ScrobbleData): ScrobbleResult {
        return try {
            Track.updateNowPlaying(scrobbleData, session)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
//     SocketTimeoutException extends InterruptedIOException
            e.printStackTrace()
            ScrobbleResult.createHttp200OKResult(0, e.cause?.message, "")
        }
    }

    override suspend fun scrobble(scrobbleData: ScrobbleData): ScrobbleResult {
        return try {
            Track.scrobble(scrobbleData, session)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
            ScrobbleResult.createHttp200OKResult(0, e.cause?.message, "")
        }
    }

    override suspend fun scrobble(scrobbleDatas: MutableList<ScrobbleData>): ScrobbleResult {
        return try {
            Track.scrobble(scrobbleDatas, session).first()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
            return ScrobbleResult.createHttp200OKResult(0, e.cause?.message, "")
        }
    }

    override suspend fun loveOrUnlove(track: Track, love: Boolean): Boolean {
        return try {
            val result = if (love)
                Track.love(track.artist, track.name, session)
            else
                Track.unlove(track.artist, track.name, session)
            result.errorCode == 6 || result.errorCode == 7 || result.isSuccessful
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
            false
        }
    }

    override suspend fun delete(track: Track): Boolean {
        return try {
            val result = Library.removeScrobble(
                track.artist,
                track.name,
                track.playedWhen.time / 1000,
                session
            )
            //     <error code="7">Invalid resource specified</error> (already deleted)
            result != null && (result.isSuccessful || result.errorCode == 7)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getRecents(
        page: Int,
        usernamep: String?,
        cached: Boolean,
        from: Long,
        to: Long,
        includeNowPlaying: Boolean,
        limit: Int,
    ): PaginatedResult<Track> {
        return coroutineScope {
            val _session = sessionCopy()
            _session.cacheStrategy =
                if (!Stuff.isOnline && cached)
                    Caller.CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
                else if (cached)
                    Caller.CacheStrategy.CACHE_FIRST
                else
                    Caller.CacheStrategy.NETWORK_ONLY

            val _from = if (to > 0L && from <= 0L) 1000L else from
            val _to = if (from > 0L && to <= 0L) System.currentTimeMillis() else to

            val pr = User.getRecentTracks(
                usernamep ?: userAccount.user.name, page, limit, true,
                _from / 1000, _to / 1000, _session
            )

            // remove np
            pr?.pageResults?.firstOrNull()?.let {
                if (!includeNowPlaying && it.isNowPlaying)
                    pr.pageResults.remove(it)
            }

            pr.isStale =
                _session.result.isFromCache && _session.cacheStrategy == Caller.CacheStrategy.CACHE_FIRST
            return@coroutineScope pr
        }
    }


    override suspend fun getLoves(
        page: Int,
        usernamep: String?,
        cached: Boolean,
        limit: Int,
    ): PaginatedResult<Track> {
        val _session = sessionCopy()
        _session.cacheStrategy =
            if (!Stuff.isOnline && cached)
                Caller.CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
            else if (cached)
                Caller.CacheStrategy.CACHE_FIRST
            else
                Caller.CacheStrategy.NETWORK_ONLY

        Stuff.log(this::getLoves.name + " " + page)
        val pr = User.getLovedTracks(
            usernamep ?: userAccount.user.name,
            limit,
            page,
            _session
        )
        pr.pageResults.forEach {
            it.isLoved = true
            it.imageUrlsMap = null
        }
        pr.isStale =
            _session.result.isFromCache && _session.cacheStrategy == Caller.CacheStrategy.CACHE_FIRST
        return pr
    }

    override suspend fun getFriends(page: Int, usernamep: String?): PaginatedResult<User> {
        return PaginatedResult<User>(1, 1, 0, listOf())
    }

    override suspend fun loadDrawerData(username: String): DrawerData? {
        Stuff.log(this::loadDrawerData.name)

        val user = User.getInfo(username, session)
        val isSelf = username == userAccount.user.name
        val prefs = MainPrefs(App.context)

        val cal = Calendar.getInstance()
        cal.setMidnight()
        val recents = User.getRecentTracks(
            username, 1, 1,
            cal.timeInMillis / 1000, System.currentTimeMillis() / 1000, session
        )

        val drawerData = if (user != null && recents != null)
            DrawerData(
                scrobblesToday = recents.total,
                scrobblesTotal = user.playcount,
                artistCount = user.artistCount,
                albumCount = user.albumCount,
                trackCount = user.trackCount,
                profilePicUrl = user.getWebpImageURL(ImageSize.EXTRALARGE)
            ).also {
                if (isSelf)
                    prefs.drawerDataCached = it
            }
        else if (isSelf)
            prefs.drawerDataCached
        else null

        return drawerData
    }

    override suspend fun getCharts(
        type: Int,
        timePeriod: TimePeriod,
        page: Int,
        usernamep: String?,
        cacheStrategy: Caller.CacheStrategy,
        limit: Int
    ): PaginatedResult<out MusicEntry> {
        val username = usernamep ?: userAccount.user.name
        val _session = sessionCopy()
        _session.cacheStrategy = cacheStrategy

        val pr: PaginatedResult<out MusicEntry>

        if (timePeriod.period != null) {
            pr = when (type) {
                Stuff.TYPE_ARTISTS -> User.getTopArtists(
                    username,
                    timePeriod.period,
                    limit,
                    page,
                    _session
                )

                Stuff.TYPE_ALBUMS -> User.getTopAlbums(
                    username,
                    timePeriod.period,
                    limit,
                    page,
                    _session
                )

                else -> User.getTopTracks(
                    username,
                    timePeriod.period,
                    limit,
                    page,
                    _session
                )
            }
        } else {
            val fromStr = (timePeriod.start / 1000).toString()
            val toStr = (timePeriod.end / 1000).toString()

            val chart = when (type) {
                Stuff.TYPE_ARTISTS -> User.getWeeklyArtistChart(
                    username,
                    fromStr,
                    toStr,
                    limit,
                    _session
                )

                Stuff.TYPE_ALBUMS -> User.getWeeklyAlbumChart(
                    username,
                    fromStr,
                    toStr,
                    limit,
                    _session
                )

                else -> User.getWeeklyTrackChart(
                    username,
                    fromStr,
                    toStr,
                    limit,
                    _session
                )
            }
            pr = PaginatedResult(1, 1, chart.entries.size, chart.entries)
        }
        return pr
    }

    override suspend fun getListeningActivity(
        timePeriod: TimePeriod,
        user: UserSerializable?,
        cacheStrategy: Caller.CacheStrategy
    ): Map<TimePeriod, Int> {
        return emptyMap()
    }

}