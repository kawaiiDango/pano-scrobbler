package com.arn.scrobble.api.atproto

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.ScrobbleResult
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.cache.CacheStrategy
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.charts.ListeningActivity
import com.arn.scrobble.charts.TimePeriod

val AccountType.playCollection: String
    get() = when (this) {
        AccountType.TEAL -> "fm.teal.feed.play"
        AccountType.ROCKSKY -> "app.rocksky.scrobble"
        else -> error("Not an AT Protocol history source")
    }

/** Adapter for AT Protocol scrobble destinations (Teal and Rocksky). */
class AtprotoScrobblable(account: UserAccountSerializable) : Scrobblable(account) {
    override suspend fun getRecents(
        page: Int,
        username: String,
        cached: Boolean,
        from: Long,
        to: Long,
        includeNowPlaying: Boolean,
        limit: Int,
    ) = unavailable<PageResult<Track>>()

    override suspend fun updateNowPlaying(scrobbleData: ScrobbleData) = Result.success(ScrobbleResult(false))

    override suspend fun scrobble(scrobbleData: ScrobbleData) = unavailable<ScrobbleResult>()

    override suspend fun scrobble(scrobbleDatas: List<ScrobbleData>) = unavailable<ScrobbleResult>()

    override suspend fun loveOrUnlove(track: Track, love: Boolean) = Result.success(ScrobbleResult(false))

    override suspend fun delete(track: Track) = unavailable<Nothing>()

    override suspend fun getLoves(page: Int, username: String, cacheStrategy: CacheStrategy, limit: Int) =
        unavailable<PageResult<Track>>()

    override suspend fun getFriends(page: Int, username: String, cached: Boolean, limit: Int) =
        unavailable<PageResult<User>>()

    override suspend fun loadDrawerData(username: String) = null

    override suspend fun getCharts(
        type: Int, timePeriod: TimePeriod, page: Int, username: String, cacheStrategy: CacheStrategy, limit: Int,
    ) = unavailable<PageResult<MusicEntry>>()

    override suspend fun getListeningActivity(
        timePeriod: TimePeriod, user: UserCached?, cacheStrategy: CacheStrategy,
    ): ListeningActivity =
        throw UnsupportedOperationException("Only Recents is available for this AT Protocol source")

    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Only Recents is available for this AT Protocol source"))
}
