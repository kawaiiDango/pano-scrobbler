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
import com.arn.scrobble.api.pleroma.Pleroma
import com.arn.scrobble.charts.ListeningActivity
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable


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
    ): ListeningActivity


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

@Serializable
enum class AccountType(val id: Int) {
    LASTFM(0),
    LISTENBRAINZ(3),
    LIBREFM(1),
    CUSTOM_LISTENBRAINZ(4),
    GNUFM(2),

    //    MALOJA(5),
    PLEROMA(6),
    FILE(7)
}

object Scrobblables {
    private val scrobblablesCache = mutableMapOf<UserAccountSerializable, Scrobblable>()

    private val accounts = PlatformStuff.mainPrefs.data
        .mapLatest { it.scrobbleAccounts.distinctBy { it.type }.toSet() }
        .stateIn(
            GlobalScope,
            SharingStarted.Eagerly,
            Stuff.mainPrefsInitialValue.scrobbleAccounts.distinctBy { it.type }.toSet()
        )

    val currentAccount = PlatformStuff.mainPrefs.data
        .mapLatest { prefs ->
            val type =
                if (prefs.scrobbleAccounts.find { it.type == prefs.currentAccountType } == null) {
                    // if current account type is not in the list, set it to the first one
                    prefs.scrobbleAccounts.firstOrNull()?.type ?: AccountType.LASTFM
                } else {
                    prefs.currentAccountType
                }

            prefs.scrobbleAccounts.firstOrNull { it.type == type }
        }
        .stateIn(
            GlobalScope,
            SharingStarted.Eagerly,
            Stuff.mainPrefsInitialValue.let { prefs ->
                prefs.scrobbleAccounts.firstOrNull { it.type == prefs.currentAccountType }
            }
        )

    val all: Collection<Scrobblable>
        @Synchronized
        get() {
            if (scrobblablesCache.keys != accounts.value) {
                // delete all scrobblables that are not in the accounts list
                // was getting ConcurrentModificationException here, so use @Synchronized
                scrobblablesCache.keys.removeIf { it !in accounts.value }

                // create new scrobblables for the accounts that are not in the cache
                accounts.value.forEach { userAccount ->
                    if (userAccount !in scrobblablesCache) {
                        scrobblablesCache[userAccount] = accountToScrobblable(userAccount)
                    }
                }
            }

            return scrobblablesCache.values
        }

    val current
        @Synchronized
        get() = scrobblablesCache[currentAccount.value]
            ?: all.firstOrNull { it.userAccount == currentAccount.value }

    suspend fun deleteAllByType(type: AccountType) {
        PlatformStuff.mainPrefs.updateData {
            val remainingAccounts = it.scrobbleAccounts.filterNot { it.type == type }
            var currentAccountType = it.currentAccountType
            if (remainingAccounts.find { it.type == currentAccountType } == null) {
                // if current account type is not in the list, set it to the first one
                currentAccountType = remainingAccounts.firstOrNull()?.type ?: AccountType.LASTFM
            }
            it.copy(
                scrobbleAccounts = remainingAccounts,
                currentAccountType = currentAccountType
            )
        }

        if (type == AccountType.LASTFM) {
            LastfmUnscrobbler.cookieStorage.clear()
        }
    }

    suspend fun add(userAccount: UserAccountSerializable) {
        PlatformStuff.mainPrefs.updateData {
            // if already exists, remove it first
            val newAccounts =
                it.scrobbleAccounts.filterNot { it.type == userAccount.type } + userAccount
            it.copy(
                scrobbleAccounts = newAccounts,
                currentAccountType = userAccount.type
            )
        }
    }

    private fun accountToScrobblable(userAccount: UserAccountSerializable): Scrobblable {
        return when (userAccount.type) {
            AccountType.LASTFM -> LastFm(userAccount)
            AccountType.LIBREFM,
            AccountType.GNUFM,
                -> GnuFm(userAccount)

            AccountType.LISTENBRAINZ,
            AccountType.CUSTOM_LISTENBRAINZ,
                -> ListenBrainz(userAccount)

//            AccountType.MALOJA -> Maloja(userAccount)

            AccountType.PLEROMA -> Pleroma(userAccount)

            AccountType.FILE -> FileScrobblable(userAccount)
        }
    }
}