package com.arn.scrobble.navigation

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.main.MainDialogActivity
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic


object DeepLinkUtils {
    private const val SCHEME = Stuff.DEEPLINK_SCHEME
    const val AUTHORITY = "screen"
    const val ROUTE = "route"

    val deepLinkSerializer by lazy {
        Json {
            explicitNulls = false
            serializersModule = SerializersModule {
                polymorphic(PanoRoute.DeepLinkable::class)
            }
        }
    }

    fun buildDialogPendingIntent(
        route: PanoRoute.DeepLinkable,
    ): PendingIntent {
        val intent = Intent(AndroidStuff.applicationContext, MainDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            action = Intent.ACTION_VIEW
        }
        val routeJson = deepLinkSerializer.encodeToString(route)

        println(routeJson)

        intent.putExtra(ROUTE, routeJson)

        return PendingIntent.getActivity(
            AndroidStuff.applicationContext,
            route.hashCode(),
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
            .putExtra("className", PanoRoute.Modal.MusicEntryInfo::class.simpleName)
    }

    fun createMainActivityDeepLinkUri(
        route: PanoRoute.DeepLinkable
    ): Uri {
        val routeStr = deepLinkSerializer.encodeToString(route)
        val uri = Uri.Builder()
            .scheme(SCHEME)
            .authority(AUTHORITY)
            .appendQueryParameter(ROUTE, routeStr)
            .build()

        return uri
    }

    fun handleNavigationFromInfoScreen(
        route: PanoRoute.DeepLinkable,
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
    ): PanoRoute.Modal? {
        val dialogClassName = intent.extras?.getString("className")

        // Handle MusicEntryInfo passed via extras (from widgets)
        if (dialogClassName == PanoRoute.Modal.MusicEntryInfo::class.simpleName) {
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

            return PanoRoute.Modal.MusicEntryInfo(
                artist = if (musicEntry is Artist) musicEntry else null,
                album = if (musicEntry is Album) musicEntry else null,
                track = if (musicEntry is Track) musicEntry else null,
                user = currentUser,
            )
        }

        // normal handling via route extra
        val routeJson = intent.getStringExtra(ROUTE) ?: return null
        val dialog = try {
            deepLinkSerializer.decodeFromString<PanoRoute.DeepLinkable>(routeJson)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        return dialog as? PanoRoute.Modal

        return null
    }

    fun parseDeepLink(
        intent: Intent,
    ): PanoRoute? {
        if (intent.action == android.service.quicksettings.TileService.ACTION_QS_TILE_PREFERENCES) {
            return PanoRoute.Prefs
        }

        val uri = intent.data ?: return null

        if (uri.scheme == SCHEME && uri.authority == AUTHORITY) {
            val routeStr = uri.getQueryParameter(ROUTE) ?: return null
            val route = try {
                deepLinkSerializer.decodeFromString<PanoRoute.DeepLinkable>(routeStr)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            return route
        }

        return null
    }
}