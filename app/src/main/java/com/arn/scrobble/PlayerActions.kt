package com.arn.scrobble

import android.media.Rating
import android.media.session.MediaController
import co.touchlab.kermit.Logger
import com.arn.scrobble.utils.Stuff

object PlayerActions {

    class CustomRatingAction(
        val love: String,
        val unlove: String,
        val unlovedLabel: String? = null,
        val extrasIsArgs: Boolean = false
    ) {
        init {
            if (love == unlove && unlovedLabel == null)
                throw IllegalArgumentException("Love and unlove actions must be different or unlovedLabel must be provided.")
        }
    }

    private val customRatingActions = mapOf(
        Stuff.PACKAGE_SPOTIFY to CustomRatingAction("ADD_TO", "CHECK_FILL"),
        Stuff.PACKAGE_SOUNDCLOUD to CustomRatingAction(
            "com.soundcloud.android.playback.action.ACTION_LIKE_TRACK",
            "com.soundcloud.android.playback.action.ACTION_LIKE_TRACK",
            "UnLikeRatingAction"
        ),
//        Stuff.PACKAGE_OTO_MUSIC to CustomRatingAction(
//            "com.piyush.music.togglefavorite",
//        ),
//        Stuff.PACKAGE_PI_MUSIC to CustomRatingAction("action_favorite"),
//        Stuff.PACKAGE_SYMFONIUM to CustomRatingAction("symfonium.action_favorite"),
        Stuff.PACKAGE_PLEXAMP to CustomRatingAction(
            "tv.plex.labs.commonandroid.LOVE_TRACK",
            "tv.plex.labs.commonandroid.INDIFFERENCE_TRACK"
        ),
//        Stuff.PACKAGE_BANDCAMP to CustomRatingAction(
//            "bandcamp_wishlist",
//            "bandcamp_wishlist",
//            true
//        ),
    )

    fun List<MediaController>.skip() {
        forEach {
            it.transportControls.skipToNext()
        }
    }

    // default rating tested working on:
    // youtube, yt music, poweramp, aimp, musicolet, neutron, doubletwist, playerpro, pandora,
    // amazon music, iHeartRadio, apple music, nyx,

    // rating does not heart individual tracks on:
    // bandcamp (wishists the entire album), tunein radio (favorites the station),

    // tested not working on:
    // vlc, bandcamp, retro, vocacolle, shuttle 2, blackplayer, phonograph, pulsar, foobar,
    // jet audio, n7, plex, subtracks, listenbrainz, radio japan, media monkey, omnia,
    // gonemad, deezer, tidal, podcast addict, gensokyo radio, gaana, wynk, jiosaavn, DIFM,
    // antenna pod, hiby, fiio,

    fun List<MediaController>.love() {
        forEach {
            // custom actions override the rating
            if (it.packageName in customRatingActions) {
                val customAction = customRatingActions[it.packageName]!!

                val customActionFromPlayer = it.playbackState?.customActions?.find {
                    it.action == customAction.love &&
                            (customAction.unlovedLabel == null ||
                                    customAction.unlovedLabel == it.name)
                } ?: return@forEach

                val args = if (customRatingActions[it.packageName]!!.extrasIsArgs)
                    customActionFromPlayer.extras
                else
                    null

                it.transportControls.sendCustomAction(customActionFromPlayer, args)

                return@forEach
            }

            val rating = when (it.ratingType) {
                Rating.RATING_THUMB_UP_DOWN ->
                    Rating.newThumbRating(true)

                Rating.RATING_HEART ->
                    Rating.newHeartRating(true)

                Rating.RATING_3_STARS ->
                    Rating.newStarRating(Rating.RATING_3_STARS, 3F)

                Rating.RATING_4_STARS ->
                    Rating.newStarRating(Rating.RATING_4_STARS, 4F)

                Rating.RATING_5_STARS ->
                    Rating.newStarRating(Rating.RATING_5_STARS, 5F)

                Rating.RATING_PERCENTAGE ->
                    Rating.newPercentageRating(100F)

                else -> null
            }
            if (rating != null)
                it.transportControls.setRating(rating)

            Logger.i { "Rating type: ${it.ratingType}" }
        }
    }

    fun List<MediaController>.unlove() {
        forEach {
            // custom actions override the rating
            if (it.packageName in customRatingActions) {
                val customAction = customRatingActions[it.packageName]!!

                val customActionFromPlayer = it.playbackState?.customActions?.find {
                    it.action == customAction.unlove &&
                            (customAction.unlovedLabel == null ||
                                    customAction.unlovedLabel != it.name)
                } ?: return@forEach

                val args = if (customRatingActions[it.packageName]!!.extrasIsArgs)
                    customActionFromPlayer.extras
                else
                    null
                it.transportControls.sendCustomAction(customActionFromPlayer, args)

                return@forEach
            }

            val ratingSupported = it.ratingType != Rating.RATING_NONE
            if (ratingSupported)
                it.transportControls.setRating(Rating.newUnratedRating(it.ratingType))
        }
    }
}