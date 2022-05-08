package com.arn.scrobble.scrobbleable

import com.arn.scrobble.Stuff
import com.arn.scrobble.Tokens
import de.umass.lastfm.CallException
import de.umass.lastfm.Result
import de.umass.lastfm.Session
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult

open class Lastfm : Scrobblable() {
    override var apiRoot = Stuff.LASTFM_API_ROOT

    protected open val apiKey = Stuff.LAST_KEY
    protected open val secret = Stuff.LAST_SECRET

    val session: Session by lazy {
        Session.createCustomRootSession(
            apiRoot,
            apiKey,
            secret,
            token
        ).also { it.isTlsNoVerify = tlsNoVerify }
    }

    override fun updateNowPlaying(scrobbleData: ScrobbleData): ScrobbleResult {
        return try {
            Track.updateNowPlaying(scrobbleData, session)
        } catch (e: CallException) {
//     SocketTimeoutException extends InterruptedIOException
            e.printStackTrace()
            ScrobbleResult.createHttp200OKResult(0, e.cause?.message, "")
        }
    }

    override fun scrobble(scrobbleData: ScrobbleData): ScrobbleResult {
        return try {
            Track.scrobble(scrobbleData, session)
        } catch (e: CallException) {
            e.printStackTrace()
            ScrobbleResult.createHttp200OKResult(0, e.cause?.message, "")
        }
    }

    override fun scrobble(scrobbleDatas: MutableList<ScrobbleData>): ScrobbleResult {
        return try {
            Track.scrobble(scrobbleDatas, session).first()
        } catch (e: CallException) {
            e.printStackTrace()
            ScrobbleResult.createHttp200OKResult(0, e.cause?.message, "")
        } catch (e: NoSuchElementException) {
            e.printStackTrace()
            ScrobbleResult.createHttp200OKResult(0, e.cause?.message, "")
        }
    }

    override fun loveOrUnlove(track: Track, love: Boolean): Result {
        return try {
            if (love)
                Track.love(track.artist, track.name, session)
            else
                Track.unlove(track.artist, track.name, session)
        } catch (e: CallException) {
            e.printStackTrace()
            ScrobbleResult.createHttp200OKResult(0, e.cause?.message, "")
        }
    }
}