package com.arn.scrobble.navigation

import android.content.Intent
import androidx.core.net.toUri
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff


actual fun handleNavigationFromInfoScreen(
    route: PanoRoute,
    usingInDialogActivity: Boolean,
    navigate: (PanoRoute) -> Unit,
) {
    if (usingInDialogActivity) {
        var deepLinkUri: String? = null
        when (route) {
            is PanoRoute.MusicEntryInfoPager -> {
                deepLinkUri =
                    Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.MusicEntryInfoPager::class.simpleName + "/" +
                            serializableType<Artist>().serializeAsValue(route.artist) + "/" +
                            serializableType<UserCached>().serializeAsValue(route.user) + "/" +
                            route.type
            }

            is PanoRoute.SimilarTracks -> {
                deepLinkUri =
                    Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.SimilarTracks::class.simpleName + "/" +
                            serializableType<Track>().serializeAsValue(route.track) + "/" +
                            serializableType<UserCached>().serializeAsValue(route.user)
            }

            is PanoRoute.TrackHistory -> {
                deepLinkUri =
                    Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.TrackHistory::class.simpleName + "/" +
                            serializableType<Track>().serializeAsValue(route.track) + "/" +
                            serializableType<UserCached>().serializeAsValue(route.user)
            }

            is PanoRoute.ImageSearch -> {
                deepLinkUri =
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
                deepLinkUri = deepLinkUri.removeSuffix("&")
            }

            else -> navigate(route)
        }

        if (deepLinkUri != null) {
            val intent = Intent(AndroidStuff.application, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                action = Intent.ACTION_VIEW
                data = deepLinkUri.toUri()
            }

            AndroidStuff.application.startActivity(intent)
        }
    } else {
        navigate(route)
    }
}