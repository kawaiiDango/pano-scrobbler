package com.arn.scrobble.scrobbleable

import com.arn.scrobble.R
import com.arn.scrobble.pref.MainPrefs
import de.umass.lastfm.Result
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult

abstract class Scrobblable {
    protected abstract var apiRoot: String
    protected var token = ""

    fun setToken(token: String?): Scrobblable? {
        token ?: return null
        this.token = token
        return this
    }

    fun setApiRoot(apiRoot: String?): Scrobblable? {
        apiRoot ?: return null
        this.apiRoot = apiRoot
        return this
    }

    abstract fun updateNowPlaying(scrobbleData: ScrobbleData): ScrobbleResult

    abstract fun scrobble(scrobbleData: ScrobbleData): ScrobbleResult

    abstract fun scrobble(scrobbleDatas: MutableList<ScrobbleData>): ScrobbleResult

    abstract fun loveOrUnlove(track: Track, love: Boolean): Result

    companion object {

        fun getScrobblablesMap(
            prefs: MainPrefs,
            shouldSupportLove: Boolean = false
        ): Map<Int, Scrobblable?> {
            val map = mutableMapOf<Int, Scrobblable?>()
            if (!prefs.lastfmDisabled)
                map[R.string.lastfm] = Lastfm().setToken(prefs.lastfmSessKey)
            map[R.string.librefm] = Librefm().setToken(prefs.librefmSessKey)
            map[R.string.gnufm] =
                Librefm().setToken(prefs.gnufmSessKey)?.setApiRoot(prefs.gnufmRoot + "2.0/")

            if (!shouldSupportLove) {
                map[R.string.listenbrainz] = ListenBrainz().setToken(prefs.listenbrainzToken)
                map[R.string.custom_listenbrainz] =
                    ListenBrainz().setToken(prefs.customListenbrainzToken)
                        ?.setApiRoot(prefs.customListenbrainzRoot)
            }
            return map
        }
    }

}