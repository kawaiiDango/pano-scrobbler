package com.arn.scrobble.api.lastfm

import com.arn.scrobble.api.Requesters.postResult
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.utils.Stuff

class GnuFm(userAccount: UserAccountSerializable) : LastFm(userAccount) {
    override val apiKey: String = Stuff.LIBREFM_KEY
    override val secret: String = Stuff.LIBREFM_KEY

    override suspend fun delete(track: Track): Boolean {
        track.date ?: return false

        val params = mapOf(
            "method" to "library.removeScrobble",
            "artist" to track.artist.name,
            "track" to track.name,
            "timestamp" to track.date.toString(),
            "format" to "json",
            "api_key" to apiKey,
            "sk" to userAccount.authKey
        )

        val result =
            client.postResult<GnuFmResult>(params = toFormParametersWithSig(params, secret))


        return result.isSuccess || (result.exceptionOrNull() as? FmException)?.code == 7
    }

    override suspend fun getFriends(
        page: Int,
        username: String,
        cached: Boolean,
        limit: Int
    ): Result<PageResult<User>> {
        val pr = PageResult(
            PageAttr(1, 1, 0),
            listOf<User>(),
            false
        )
        return Result.success(pr)
    }

    override suspend fun getListeningActivity(
        timePeriod: TimePeriod,
        user: UserCached?,
        cacheStrategy: CacheStrategy
    ): Map<TimePeriod, Int> {
        return emptyMap()
    }
}