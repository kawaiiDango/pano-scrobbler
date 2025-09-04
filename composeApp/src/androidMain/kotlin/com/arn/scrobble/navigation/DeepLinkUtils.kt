package com.arn.scrobble.navigation

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.main.MainDialogActivity
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff
import io.ktor.http.encodeURLPathPart


object DeepLinkUtils {
    inline fun <reified T> buildDeepLink(
        dialogArgs: T,
    ): String {
        val klassName = T::class.simpleName
        val args = Stuff.myJson.encodeToString(dialogArgs)

        return Stuff.DEEPLINK_BASE_PATH + "/" + klassName + "/" + args.encodeURLPathPart()
    }

    fun createDeepLinkUri(
        route: PanoRoute
    ): Uri? {
        return when (route) {
            is PanoRoute.MusicEntryInfoPager -> {
                Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.MusicEntryInfoPager::class.simpleName + "/" +
                        serializableType<Artist>().serializeAsValue(route.artist) + "/" +
                        serializableType<UserCached>().serializeAsValue(route.user) + "/" +
                        route.type
            }

            is PanoRoute.SimilarTracks -> {
                Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.SimilarTracks::class.simpleName + "/" +
                        serializableType<Track>().serializeAsValue(route.track) + "/" +
                        serializableType<UserCached>().serializeAsValue(route.user)
            }

            is PanoRoute.TrackHistory -> {
                Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.TrackHistory::class.simpleName + "/" +
                        serializableType<Track>().serializeAsValue(route.track) + "/" +
                        serializableType<UserCached>().serializeAsValue(route.user)
            }

            is PanoRoute.Billing -> {
                Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.Billing::class.simpleName
            }

            is PanoRoute.ImageSearch -> {
                val uri =
                    Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.ImageSearch::class.simpleName + "?" +
                            (if (route.artist != null) route::artist.name + "=" + serializableType<Artist>().serializeAsValue(
                                route.artist
                            ) + "&" else "") +
                            (if (route.originalArtist != null) route::originalArtist.name + "=" + serializableType<Artist>().serializeAsValue(
                                route.originalArtist
                            ) + "&" else "") +
                            (if (route.album != null) route::album.name + "=" + serializableType<Album>().serializeAsValue(
                                route.album
                            ) + "&" else "") +
                            (if (route.originalAlbum != null) route::originalAlbum.name + "=" + serializableType<Album>().serializeAsValue(
                                route.originalAlbum
                            ) + "&" else "")

                // remove the trailing "&"
                uri.removeSuffix("&")
            }

            is PanoRoute.SelfHomePager -> {
                Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.SelfHomePager::class.simpleName +
                        if (route.digestTypeStr != null)
                            ("?" + route::digestTypeStr.name + "=" + route.digestTypeStr)
                        else
                            ""
            }

            else -> null
        }
            ?.toUri()

    }

    fun handleNavigationFromInfoScreen(
        route: PanoRoute,
    ) {
        val intent = Intent(AndroidStuff.applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            action = Intent.ACTION_VIEW
            data = createDeepLinkUri(route)
        }

        AndroidStuff.applicationContext.startActivity(intent)
    }


    fun createDestinationPendingIntent(
        uri: String,
        mutable: Boolean = false,
    ) =
        PendingIntent.getActivity(
            AndroidStuff.applicationContext,
            uri.hashCode(),
            Intent(AndroidStuff.applicationContext, MainDialogActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                action = Intent.ACTION_VIEW
                data = uri.toUri()
            },
            if (mutable) AndroidStuff.updateCurrentOrMutable else AndroidStuff.updateCurrentOrImmutable
        )!!

    fun parseDeepLink(
        uri: Uri,
    ): PanoDialog? {
        val uriPathSegments = uri.pathSegments ?: return null
        if (uriPathSegments.size != 2) return null
        val dialogClassName = uriPathSegments[0]
        val dialogArgs = uriPathSegments[1]

        val serializer = when (dialogClassName) {
            PanoDialog.TagInfo::class.simpleName -> {
                PanoDialog.TagInfo.serializer()
            }

            PanoDialog.CollageGenerator::class.simpleName -> {
                PanoDialog.CollageGenerator.serializer()
            }

            PanoDialog.BlockedMetadataAdd::class.simpleName -> {
                PanoDialog.BlockedMetadataAdd.serializer()
            }

            PanoDialog.EditScrobble::class.simpleName -> {
                PanoDialog.EditScrobble.serializer()
            }

            PanoDialog.MusicEntryInfo::class.simpleName -> {
                PanoDialog.MusicEntryInfo.serializer()
            }

            else -> return null
        }

        val args = Stuff.myJson.decodeFromString(serializer, dialogArgs)

        return args
    }
}