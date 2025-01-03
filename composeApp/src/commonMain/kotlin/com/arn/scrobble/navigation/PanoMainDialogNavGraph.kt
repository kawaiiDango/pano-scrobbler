package com.arn.scrobble.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
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
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.info.MusicEntryInfoScreen
import com.arn.scrobble.info.TagInfoScreen
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.utils.Stuff
import kotlin.reflect.typeOf

fun NavGraphBuilder.panoDialogNavGraph(
    calledFromDialogActivity: Boolean,
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
            onNavigate = { handleNavigationFromInfoScreen(it, calledFromDialogActivity, navigate) },
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
