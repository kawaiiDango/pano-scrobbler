package com.arn.scrobble.navigation

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.main.MainDialogActivity
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff


object DeepLinkUtils {
    inline fun <reified T : PanoDialog> buildDialogPendingIntent(
        dialogArgs: T,
    ): PendingIntent {
        val intent = Intent(AndroidStuff.applicationContext, MainDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            action = Intent.ACTION_VIEW
        }
        val className = T::class.simpleName
        val args = Stuff.myJson.encodeToString(dialogArgs)

        intent.putExtra("className", className)
            .putExtra("args", args)

        return PendingIntent.getActivity(
            AndroidStuff.applicationContext,
            dialogArgs.hashCode(),
            intent,
            AndroidStuff.updateCurrentOrImmutable
        )!!
    }

    fun fillInIntentForMusicEntryInfo(
        intent: Intent,
        artist: String,
        album: String?,
        track: String?,
    ): Intent {
        return intent.putExtra("artist", artist)
            .putExtra("album", album)
            .putExtra("track", track)
            .putExtra("className", PanoDialog.MusicEntryInfo::class.simpleName)
    }

    fun createMainActivityDeepLinkUri(
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
            data = createMainActivityDeepLinkUri(route)
        }

        AndroidStuff.applicationContext.startActivity(intent)
    }

    fun parseDialogDeepLink(
        intent: Intent,
    ): PanoDialog? {
        val dialogClassName = intent.extras?.getString("className")
        val dialogArgsSerialized = intent.extras?.getString("args")
        // Handle MusicEntryInfo passed via extras (from widgets)

        val dialogArgs = when (dialogClassName) {
            PanoDialog.CollageGenerator::class.simpleName -> {
                Stuff.myJson.decodeFromString(
                    PanoDialog.CollageGenerator.serializer(),
                    dialogArgsSerialized ?: return null
                )
            }

            PanoDialog.BlockedMetadataAdd::class.simpleName -> {
                Stuff.myJson.decodeFromString(
                    PanoDialog.BlockedMetadataAdd.serializer(),
                    dialogArgsSerialized ?: return null
                )
            }

            PanoDialog.EditScrobble::class.simpleName -> {
                Stuff.myJson.decodeFromString(
                    PanoDialog.EditScrobble.serializer(),
                    dialogArgsSerialized ?: return null
                )
            }

            PanoDialog.MusicEntryInfo::class.simpleName -> {
                if (dialogArgsSerialized != null) {
                    Stuff.myJson.decodeFromString(
                        PanoDialog.MusicEntryInfo.serializer(),
                        dialogArgsSerialized
                    )
                } else {
                    val currentUser = Scrobblables.current?.userAccount?.user ?: return null

                    val artist = intent.extras?.getString("artist") ?: return null
                    val album = intent.extras?.getString("album")
                    val track = intent.extras?.getString("track")

                    val musicEntry = when {
                        track != null -> Track(
                            name = track,
                            album = null,
                            artist = Artist(name = artist),
                        )

                        album != null -> Album(
                            name = album,
                            artist = Artist(name = artist),
                        )

                        else -> Artist(
                            name = artist,
                        )
                    }

                    PanoDialog.MusicEntryInfo(
                        artist = if (musicEntry is Artist) musicEntry else null,
                        album = if (musicEntry is Album) musicEntry else null,
                        track = if (musicEntry is Track) musicEntry else null,
                        user = currentUser,
                    )
                }
            }

            else -> return null
        }

        return dialogArgs
    }
}