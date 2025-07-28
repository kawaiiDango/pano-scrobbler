package com.arn.scrobble.media

import android.media.Rating
import android.media.session.MediaController
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import co.touchlab.kermit.Logger
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff

object PlayerActions {

    class CustomRatingAction(
        val love: String,
        val unlove: String = love,
        val threshold: Float = 0.3f,
    )

    private val customRatingActions = mapOf(
        Stuff.PACKAGE_SPOTIFY to CustomRatingAction("ADD_TO", "CHECK_FILL"),
        Stuff.PACKAGE_SOUNDCLOUD to CustomRatingAction("com.soundcloud.android.playback.action.ACTION_LIKE_TRACK"),
        Stuff.PACKAGE_OTO_MUSIC to CustomRatingAction(
            "com.piyush.music.togglefavorite",
        ),
        Stuff.PACKAGE_PI_MUSIC to CustomRatingAction("action_favorite "), // yeah it has a space at the end
        Stuff.PACKAGE_SYMFONIUM to CustomRatingAction("symfonium.action_favorite"),
        Stuff.PACKAGE_PLEXAMP to CustomRatingAction(
            "tv.plex.labs.commonandroid.LOVE_TRACK",
            "tv.plex.labs.commonandroid.INDIFFERENCE_TRACK"
        ),
        Stuff.PACKAGE_ECHO to CustomRatingAction("liked", "unliked"),
        Stuff.PACKAGE_METROLIST to CustomRatingAction("TOGGLE_LIKE"),
        Stuff.PACKAGE_APPLE_MUSIC to CustomRatingAction("com.apple.android.music.playback.action.FAVORITE"),
        Stuff.PACKAGE_TIDAL to CustomRatingAction("com.aspiro.tidal.player.action.TOGGLE_FAVORITE"),
        Stuff.PACKAGE_OMNIA to CustomRatingAction("action_favorite", threshold = 0.15f),
    )

    fun List<MediaController>.skip() {
        forEach {
            it.transportControls.skipToNext()
        }
    }

    // default rating tested working on:
    // youtube, yt music, poweramp, aimp, musicolet, neutron, doubletwist, playerpro, pandora,
    // amazon music, iHeartRadio, apple music, nyx, aimp, neutron, media monkey,

    // rating does not heart individual tracks on:
    // bandcamp (wishists the entire album), tunein radio (favorites the station),

    // tested not working on:
    // vlc, bandcamp, retro, vocacolle, shuttle 2, blackplayer, phonograph, pulsar, foobar,
    // jet audio, n7, plex, subtracks, listenbrainz, radio japan, omnia, namida,
    // gonemad, deezer, tidal, podcast addict, gensokyo radio, gaana, wynk, jiosaavn, DIFM,
    // antenna pod, hiby, fiio,

    fun List<MediaController>.love() {
        forEach {
            // custom actions override the rating
            if (it.packageName in customRatingActions) {
                val customAction = customRatingActions[it.packageName]!!

                val customActionFromPlayer = it.playbackState?.customActions?.find {
                    it.action == customAction.love
                } ?: return@forEach

                if (customAction.love == customAction.unlove && isProbablyFilledHeart(
                        it.packageName,
                        customActionFromPlayer.icon,
                        customAction.threshold
                    ) == true
                ) {
                    // already loved
                    return@forEach
                }

                it.transportControls.sendCustomAction(customActionFromPlayer, null)

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
                    it.action == customAction.unlove
                } ?: return@forEach

                if (customAction.love == customAction.unlove && isProbablyFilledHeart(
                        it.packageName,
                        customActionFromPlayer.icon,
                        customAction.threshold
                    ) == false
                ) {
                    // already unloved
                    return@forEach
                }

                it.transportControls.sendCustomAction(customActionFromPlayer, null)

                return@forEach
            }

            val ratingSupported = it.ratingType != Rating.RATING_NONE
            if (ratingSupported)
                it.transportControls.setRating(Rating.newUnratedRating(it.ratingType))
        }
    }

    private fun isProbablyFilledHeart(
        packageName: String,
        iconResId: Int,
        threshold: Float
    ): Boolean? {
        // Check if the icon bitmap is at least 30% not transparent
        val resources =
            AndroidStuff.application.packageManager.getResourcesForApplication(packageName)

        val drawable = ResourcesCompat.getDrawable(resources, iconResId, null)
            ?: return null

        val bitmap = try {
            drawable.toBitmap()
        } catch (e: Exception) {
            Logger.w(e) { "Failed to convert drawable to bitmap for package: $packageName" }
            return null
        }

        val pixels = IntArray(bitmap.width * bitmap.height)
        // getPixels is an expensive jni call, do it only once
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val totalPixels = pixels.size
        val nonTransparentPixels = pixels.count { (it shr 24) != 0 }// alpha channel check

        val nonTransparentRatio = nonTransparentPixels.toFloat() / totalPixels

        Logger.d { "Non-transparent ratio for $packageName: $nonTransparentRatio" }

        // A filled heart has about 40% non-transparent pixels
        return nonTransparentRatio >= threshold
    }
}