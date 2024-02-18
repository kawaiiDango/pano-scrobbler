package com.arn.scrobble

import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaControllerCompat

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

    fun List<MediaControllerCompat>.skip() {
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

    fun List<MediaControllerCompat>.love() {
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
                RatingCompat.RATING_THUMB_UP_DOWN ->
                    RatingCompat.newThumbRating(true)

                RatingCompat.RATING_HEART ->
                    RatingCompat.newHeartRating(true)

                RatingCompat.RATING_3_STARS ->
                    RatingCompat.newStarRating(RatingCompat.RATING_3_STARS, 3F)

                RatingCompat.RATING_4_STARS ->
                    RatingCompat.newStarRating(RatingCompat.RATING_4_STARS, 4F)

                RatingCompat.RATING_5_STARS ->
                    RatingCompat.newStarRating(RatingCompat.RATING_5_STARS, 5F)

                RatingCompat.RATING_PERCENTAGE ->
                    RatingCompat.newPercentageRating(100F)

                else -> null
            }
            if (rating != null)
                it.transportControls.setRating(rating)

            Stuff.log("Rating type: ${it.ratingType}")
        }
    }

    fun List<MediaControllerCompat>.unlove() {
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

            val ratingSupported = it.ratingType != RatingCompat.RATING_NONE
            if (ratingSupported)
                it.transportControls.setRating(RatingCompat.newUnratedRating(it.ratingType))
        }
    }
}