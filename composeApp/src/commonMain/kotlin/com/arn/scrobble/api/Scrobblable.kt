package com.arn.scrobble.api

import co.touchlab.kermit.Logger
import com.arn.scrobble.api.cache.CacheStrategy
import com.arn.scrobble.api.file.FileScrobblable
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.GnuFm
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.LastfmUnscrobbler
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.PageAttr
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.api.maloja.Maloja
import com.arn.scrobble.api.pleroma.Pleroma
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn


abstract class Scrobblable(val userAccount: UserAccountSerializable) {

    abstract suspend fun updateNowPlaying(scrobbleData: ScrobbleData): Result<ScrobbleIgnored>

    abstract suspend fun scrobble(scrobbleData: ScrobbleData): Result<ScrobbleIgnored>

    abstract suspend fun scrobble(scrobbleDatas: List<ScrobbleData>): Result<ScrobbleIgnored>

    abstract suspend fun loveOrUnlove(track: Track, love: Boolean): Result<ScrobbleIgnored>

    abstract suspend fun delete(track: Track): Result<Unit>

    abstract suspend fun getRecents(
        page: Int,
        username: String = userAccount.user.name,
        cached: Boolean = false,
        from: Long = -1,
        to: Long = -1,
        includeNowPlaying: Boolean = false,
        limit: Int = 50,
    ): Result<PageResult<Track>>

    abstract suspend fun getLoves(
        page: Int,
        username: String = userAccount.user.name,
        cacheStrategy: CacheStrategy = if (Stuff.isOnline)
            CacheStrategy.NETWORK_ONLY
        else
            CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED,
        limit: Int = 50,
    ): Result<PageResult<Track>>

    abstract suspend fun getFriends(
        page: Int,
        username: String = userAccount.user.name,
        cached: Boolean = false,
        limit: Int = 50,
    ): Result<PageResult<User>>

    abstract suspend fun loadDrawerData(username: String): DrawerData?

    abstract suspend fun getCharts(
        type: Int,
        timePeriod: TimePeriod,
        page: Int,
        username: String = userAccount.user.name,
        cacheStrategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
        limit: Int = if (timePeriod.lastfmPeriod != null || timePeriod.tag != null) 50 else -1,
    ): Result<PageResult<out MusicEntry>>

    abstract suspend fun getListeningActivity(
        timePeriod: TimePeriod,
        user: UserCached?,
        cacheStrategy: CacheStrategy = CacheStrategy.NETWORK_ONLY,
    ): Map<TimePeriod, Int>


    suspend fun getChartsWithStonks(
        type: Int,
        timePeriod: TimePeriod,
        prevTimePeriod: TimePeriod?,
        page: Int,
        username: String = userAccount.user.name,
        networkOnly: Boolean = false,
        limit: Int = if (timePeriod.lastfmPeriod != null) 50 else -1,
    ): Result<PageResult<out MusicEntry>> {
        Logger.i { this::getChartsWithStonks.name + " $type timePeriod: $timePeriod prevTimePeriod: $prevTimePeriod" }

        // remove play counts and all the extra data
        fun toHashableEntry(entry: MusicEntry): MusicEntry = when (entry) {
            is Artist -> Artist(entry.name)

            is Album -> Album(entry.name, artist = Artist(entry.artist!!.name))

            is Track -> Track(
                entry.name,
                artist = Artist(entry.artist.name),
                album = entry.album?.let { Album(it.name) })
        }

        val prevCharts =
            if (prevTimePeriod != null && userAccount.type == AccountType.LASTFM)
                getCharts(
                    type,
                    prevTimePeriod,
                    1,
                    username,
                    CacheStrategy.CACHE_FIRST_ONE_DAY,
                    -1
                ).getOrNull()
            else
                null

        val prevChartsMap = prevCharts?.entries?.associate {
            toHashableEntry(it) to it.rank!!
        } ?: emptyMap()

        val cacheStrategy = if (networkOnly)
            CacheStrategy.NETWORK_ONLY
        else if (!Stuff.isOnline)
            CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
        else
            CacheStrategy.CACHE_FIRST

        val currentCharts = getCharts(type, timePeriod, page, username, cacheStrategy, limit)

        val doStonks =
            (limit == -1 || page * limit < (0.7 * prevChartsMap.size)) && (prevChartsMap.isNotEmpty())

        currentCharts
            .map { pr ->
                pr.entries.forEach {
                    val hashableEntry = toHashableEntry(it)
                    val prevRank = prevChartsMap[hashableEntry]
                    if (doStonks) {
                        it.stonksDelta = if (prevRank != null)
                            prevRank - it.rank!!
                        else
                            Int.MAX_VALUE
                    }
                }
            }
        return currentCharts
    }

    fun <T> createEmptyPageResult() = PageResult(
        PageAttr(1, 1, 0),
        listOf<T>(),
    )
}

data class ScrobbleIgnored(val ignored: Boolean)

enum class AccountType {
    LASTFM,
    LIBREFM,
    GNUFM,
    LISTENBRAINZ,
    CUSTOM_LISTENBRAINZ,
    MALOJA,
    PLEROMA,
    FILE
}

object Scrobblables {
    val all = PlatformStuff.mainPrefs.data
        .mapLatest { it.scrobbleAccounts }
        .distinctUntilChanged()
        .mapLatest { account ->
            account.distinctBy { it.type }
                .map {
                    accountToScrobblable(it)
                }
        }.stateIn(GlobalScope, SharingStarted.Eagerly, emptyList())

    val current = combine(
        all,
        PlatformStuff.mainPrefs.data.mapLatest { it.currentAccountType }) { all, currentType ->
        var account = all.firstOrNull { it.userAccount.type == currentType }

        if (all.isNotEmpty() && account == null) {
            account = all.first()
            PlatformStuff.mainPrefs.updateData { it.copy(currentAccountType = account.userAccount.type) }
        }

        account
    }.stateIn(GlobalScope, SharingStarted.Eagerly, null)

    val currentScrobblableUser
        get() =
            current.value?.userAccount?.user

    suspend fun deleteAllByType(type: AccountType) {
        PlatformStuff.mainPrefs.updateData { it.copy(scrobbleAccounts = it.scrobbleAccounts.filterNot { it.type == type }) }

        if (type == AccountType.LASTFM) {
            LastfmUnscrobbler.cookieStorage.clear()
        }
    }

    suspend fun add(userAccount: UserAccountSerializable) {
        // if already exists, remove it first
        PlatformStuff.mainPrefs.updateData { it.copy(scrobbleAccounts = it.scrobbleAccounts.filterNot { it.type == userAccount.type } + userAccount) }
    }

    suspend fun setCurrent(type: AccountType) {
        PlatformStuff.mainPrefs.updateData { it.copy(currentAccountType = type) }
    }

    fun accountToScrobblable(
        userAccount: UserAccountSerializable,
    ): Scrobblable {
        return when (userAccount.type) {
            AccountType.LASTFM -> LastFm(userAccount)
            AccountType.LIBREFM,
            AccountType.GNUFM,
                -> GnuFm(userAccount)

            AccountType.LISTENBRAINZ,
            AccountType.CUSTOM_LISTENBRAINZ,
                -> ListenBrainz(userAccount)

            AccountType.MALOJA -> Maloja(userAccount)

            AccountType.PLEROMA -> Pleroma(userAccount)

            AccountType.FILE -> FileScrobblable(userAccount)
        }
    }
}