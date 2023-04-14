package com.arn.scrobble.scrobbleable

import com.arn.scrobble.App
import com.arn.scrobble.DrawerData
import com.arn.scrobble.LastfmUnscrobbler
import com.arn.scrobble.R
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.friends.UserSerializable
import de.umass.lastfm.Caller
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import de.umass.lastfm.User
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult

abstract class Scrobblable(val userAccount: UserAccountSerializable) {

    abstract suspend fun updateNowPlaying(scrobbleData: ScrobbleData): ScrobbleResult

    abstract suspend fun scrobble(scrobbleData: ScrobbleData): ScrobbleResult

    abstract suspend fun scrobble(scrobbleDatas: MutableList<ScrobbleData>): ScrobbleResult

    abstract suspend fun loveOrUnlove(track: Track, love: Boolean): Boolean

    abstract suspend fun delete(track: Track): Boolean

    abstract suspend fun getRecents(
        page: Int,
        usernamep: String?,
        cached: Boolean = false,
        from: Long = -1,
        to: Long = -1,
        includeNowPlaying: Boolean = false,
        limit: Int = 50,
    ): PaginatedResult<Track>

    abstract suspend fun getLoves(
        page: Int,
        usernamep: String?,
        cached: Boolean = false,
        limit: Int = 50,
    ): PaginatedResult<Track>

    abstract suspend fun getFriends(page: Int, usernamep: String?): PaginatedResult<User>

    abstract suspend fun loadDrawerData(username: String): DrawerData?

    abstract suspend fun getCharts(
        type: Int,
        timePeriod: TimePeriod,
        page: Int,
        usernamep: String?,
        cacheStrategy: Caller.CacheStrategy = Caller.CacheStrategy.CACHE_FIRST,
        limit: Int = if (timePeriod.period != null || timePeriod.tag != null) 50 else -1
    ): PaginatedResult<out MusicEntry>

    abstract suspend fun getListeningActivity(
        timePeriod: TimePeriod,
        user: UserSerializable?,
        cacheStrategy: Caller.CacheStrategy = Caller.CacheStrategy.NETWORK_ONLY,
    ): Map<TimePeriod, Int>
}

enum class AccountType {
    LASTFM,
    LIBREFM,
    GNUFM,
    LISTENBRAINZ,
    CUSTOM_LISTENBRAINZ
}

object Scrobblables {
    val all = mutableListOf<Scrobblable>()

    fun updateScrobblables() {
        val prefs = App.prefs
        synchronized(all) {
            all.clear()
            all.addAll(
                prefs.scrobbleAccounts.map {
                    when (it.type) {
                        AccountType.LASTFM -> Lastfm(it)
                        AccountType.LIBREFM,
                        AccountType.GNUFM -> GnuFm(it)

                        AccountType.LISTENBRAINZ,
                        AccountType.CUSTOM_LISTENBRAINZ -> ListenBrainz(it)
                    }
                }
            )
        }

        if (current == null) {
            prefs.currentAccountIdx = 0
        }
    }

    val current
        get() =
            all.getOrNull(App.prefs.currentAccountIdx)

    val currentScrobblableUser
        get() =
            current?.userAccount?.user

    fun setCurrent(userAccount: UserAccountSerializable) {
        val idx = all.indexOfFirst { it.userAccount == userAccount }
        if (idx != -1) {
            App.prefs.currentAccountIdx = idx
        }
    }

    fun byType(type: AccountType) = all.find { it.userAccount.type == type }

    fun allByType(type: AccountType) =
        all.filter { it.userAccount.type == type }.ifEmpty { null }

    fun deleteAllByType(type: AccountType) {
        val prefs = App.prefs
        prefs.scrobbleAccounts =
            prefs.scrobbleAccounts.toMutableList().apply { removeAll { it.type == type } }
        updateScrobblables()

        if (type == AccountType.LASTFM) {
            LastfmUnscrobbler().clearCookies()
        }
    }

    fun delete(userAccount: UserAccountSerializable) {
        val prefs = App.prefs
        prefs.scrobbleAccounts =
            prefs.scrobbleAccounts.toMutableList().apply { removeAll { it == userAccount } }
        updateScrobblables()

        if (userAccount.type == AccountType.LASTFM) {
            LastfmUnscrobbler().clearCookies()
        }
    }

    fun add(userAccount: UserAccountSerializable) {
        val prefs = App.prefs
        prefs.scrobbleAccounts += userAccount
        updateScrobblables()
    }

    fun getString(accountType: AccountType) = when (accountType) {
        AccountType.LASTFM -> App.context.getString(R.string.lastfm)
        AccountType.LIBREFM -> App.context.getString(R.string.librefm)
        AccountType.GNUFM -> App.context.getString(R.string.gnufm)
        AccountType.LISTENBRAINZ -> App.context.getString(R.string.listenbrainz)
        AccountType.CUSTOM_LISTENBRAINZ -> App.context.getString(R.string.custom_listenbrainz)
    }
}