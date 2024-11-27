package com.arn.scrobble.navigation

import android.content.Intent
import androidx.core.net.toUri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.charts.CollageGeneratorScreen
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.edits.BlockedMetadataAddScreen
import com.arn.scrobble.edits.EditScrobbleDialog
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.info.MusicEntryInfoScreen
import com.arn.scrobble.info.TagInfoScreen
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.utils.Stuff
import kotlin.reflect.typeOf

fun NavGraphBuilder.panoDialogNavGraph(
    usingInDialogActivity: Boolean,
    navigate: (PanoRoute) -> Unit,
    goUp: () -> Unit,
    mainViewModel: MainViewModel,
) {
    composable<PanoRoute.BlankScreen> {

    }

    // dialogs

    // Deep link: pano-scrobbler://screen/BlockedMetadataAdd?blockedMetadata={blockedMetadata}&ignoredArtist={ignoredArtist}&hash={hash}
    dialog<PanoRoute.BlockedMetadataAdd>(
        deepLinks = listOf(
            navDeepLink<PanoRoute.BlockedMetadataAdd>(
                basePath = Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.BlockedMetadataAdd::class.simpleName,
                typeMap = mapOf(
                    typeOf<BlockedMetadata>() to serializableType<BlockedMetadata>()
                )
            )
        ),
        typeMap = mapOf(typeOf<BlockedMetadata>() to serializableType<BlockedMetadata>())
    ) {
        val arguments = it.toRoute<PanoRoute.BlockedMetadataAdd>()

        BlockedMetadataAddScreen(
            blockedMetadata = arguments.blockedMetadata,
            ignoredArtist = arguments.ignoredArtist,
            hash = arguments.hash,
            onNavigateToBilling = { navigate(PanoRoute.Billing) },
            onDismiss = goUp,
        )
    }

//    pano-scrobbler://screen/EditScrobble/{scrobbleData}/{msid}?hash={hash}
    dialog<PanoRoute.EditScrobble>(
        deepLinks = listOf(
            navDeepLink<PanoRoute.EditScrobble>(
                basePath = Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.EditScrobble::class.simpleName,
                typeMap = mapOf(
                    typeOf<ScrobbleData>() to serializableType<ScrobbleData>()
                )
            )
        ),
        typeMap = mapOf(typeOf<ScrobbleData>() to serializableType<ScrobbleData>())
    ) {
        val arguments = it.toRoute<PanoRoute.EditScrobble>()

        EditScrobbleDialog(
            scrobbleData = arguments.scrobbleData,
            onDone = {
                // todo: handle scrobble edit from notification
            },
            msid = arguments.msid,
            hash = arguments.hash,
            onNavigate = navigate,
            onDismiss = goUp,
        )
    }

//    Deep link: pano-scrobbler://screen/TagInfo/{tag}
    dialog<PanoRoute.TagInfo>(
        deepLinks = listOf(
            navDeepLink<PanoRoute.TagInfo>(
                basePath = Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.TagInfo::class.simpleName,
                typeMap = mapOf(
                    typeOf<Tag>() to serializableType<Tag>()
                )
            )
        ),
        typeMap = mapOf(typeOf<Tag>() to serializableType<Tag>())
    ) {
        val arguments = it.toRoute<PanoRoute.TagInfo>()

        TagInfoScreen(
            tag = arguments.tag,
            onDismiss = goUp
        )
    }

//    Deep link: pano-scrobbler://screen/MusicEntryInfo/{user}/{pkgName}?artist={artist}&album={album}&track={track}
    dialog<PanoRoute.MusicEntryInfo>(
        deepLinks = listOf(
            navDeepLink<PanoRoute.MusicEntryInfo>(
                basePath = Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.MusicEntryInfo::class.simpleName,
                typeMap = mapOf(
                    typeOf<Artist?>() to serializableType<Artist?>(),
                    typeOf<Album?>() to serializableType<Album?>(),
                    typeOf<Track?>() to serializableType<Track?>(),
                    typeOf<UserCached>() to serializableType<UserCached>()
                )
            )
        ),
        typeMap = mapOf(
            typeOf<Artist?>() to serializableType<Artist?>(),
            typeOf<Album?>() to serializableType<Album?>(),
            typeOf<Track?>() to serializableType<Track?>(),
            typeOf<UserCached>() to serializableType<UserCached>()
        )
    ) {
        val arguments = it.toRoute<PanoRoute.MusicEntryInfo>()

        MusicEntryInfoScreen(
            musicEntry = arguments.artist ?: arguments.album ?: arguments.track!!,
            pkgName = arguments.pkgName,
            user = arguments.user,
            onNavigate = { handleNavigationFromInfoScreen(it, usingInDialogActivity, navigate) },
            onDismiss = goUp
        )
    }

    // Deep link: pano-scrobbler://screen/CollageGenerator/{collageType}/{timePeriod}/{user}
    dialog<PanoRoute.CollageGenerator>(
        deepLinks = listOf(
            navDeepLink<PanoRoute.CollageGenerator>(
                basePath = Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.CollageGenerator::class.simpleName,
                typeMap = mapOf(
                    typeOf<TimePeriod>() to serializableType<TimePeriod>(),
                    typeOf<UserCached>() to serializableType<UserCached>()
                )
            )
        ),
        typeMap = mapOf(
            typeOf<TimePeriod>() to serializableType<TimePeriod>(),
            typeOf<UserCached>() to serializableType<UserCached>()
        )
    ) {
        val arguments = it.toRoute<PanoRoute.CollageGenerator>()

        CollageGeneratorScreen(
            collageType = arguments.collageType,
            timePeriod = arguments.timePeriod,
            user = arguments.user,
            onDismiss = goUp
        )
    }
}

private fun handleNavigationFromInfoScreen(
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
            val intent = Intent(PlatformStuff.application, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                action = Intent.ACTION_VIEW
                data = deepLinkUri.toUri()
            }

            PlatformStuff.application.startActivity(intent)
        }
    } else {
        navigate(route)
    }
}