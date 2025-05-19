package com.arn.scrobble.api.lastfm

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.postResult
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.cache.CacheStrategy
import com.arn.scrobble.charts.ListeningActivity
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.utils.Stuff
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody

class GnuFm(userAccount: UserAccountSerializable) : LastFm(userAccount) {
    override val apiKey: String = Stuff.LIBREFM_KEY
    override val secret: String = Stuff.LIBREFM_KEY

    override suspend fun delete(track: Track): Result<Unit> {
        val params = mapOf(
            "method" to "library.removeScrobble",
            "artist" to track.artist.name,
            "track" to track.name,
            "timestamp" to (track.date?.div(1000)?.toString()
                ?: return Result.failure(IllegalStateException("no date"))),
            "format" to "json",
            "api_key" to apiKey,
            "sk" to userAccount.authKey
        )

        val result = client.postResult<GnuFmResult> {
            setBody(FormDataContent(toFormParametersWithSig(params, secret)))
        }

        // {"error":{"#text":"Invalid resource specified","code":"7"}}
        // {"error": 3, "message": "Invalid method"}
        return if (result.isSuccess ||
            (result.exceptionOrNull() as? ApiException)?.code in arrayOf(7, 3)
        )
            Result.success(Unit)
        else
            Result.failure(result.exceptionOrNull()!!)
    }

    override suspend fun getCharts(
        type: Int,
        timePeriod: TimePeriod,
        page: Int,
        username: String,
        cacheStrategy: CacheStrategy,
        limit: Int,
    ): Result<PageResult<out MusicEntry>> {
        if (type == Stuff.TYPE_ALBUMS || timePeriod.lastfmPeriod == null) {
            return Result.success(createEmptyPageResult())
        }

        return super.getCharts(type, timePeriod, page, username, cacheStrategy, limit)
    }

    override suspend fun getFriends(
        page: Int,
        username: String,
        cached: Boolean,
        limit: Int,
    ): Result<PageResult<User>> {
        return Result.success(createEmptyPageResult())
    }

    override suspend fun getListeningActivity(
        timePeriod: TimePeriod,
        user: UserCached?,
        cacheStrategy: CacheStrategy,
    ): ListeningActivity {
        return ListeningActivity()
    }

    companion object {
        suspend fun authAndGetSession(
            apiRoot: String,
            username: String,
            password: String,
        ): Result<Session> {
            val apiKey = Stuff.LIBREFM_KEY
            val apiSecret = Stuff.LIBREFM_KEY

            val session = Requesters.lastfmUnauthedRequester.getMobileSession(
                apiRoot,
                apiKey,
                apiSecret,
                username,
                password
            ).onSuccess {
                val user = UserCached(
                    username,
                    "$apiRoot/user/$username",
                    username,
                    "",
                    -1,
                )

                val account = UserAccountSerializable(
                    AccountType.GNUFM,
                    user,
                    it.key,
                    apiRoot,
                )

                Scrobblables.add(account)
            }.onFailure {
                it.printStackTrace()
            }

            return session
        }
    }
}