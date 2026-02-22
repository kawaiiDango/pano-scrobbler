package com.arn.scrobble.navigation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.charts.ChartsLegendDialog
import com.arn.scrobble.charts.CollageGeneratorDialog
import com.arn.scrobble.charts.HiddenTagsDialog
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.edits.BlockedMetadataAddDialog
import com.arn.scrobble.edits.SimpleEditsAddScreen
import com.arn.scrobble.info.MusicEntryInfoDialog
import com.arn.scrobble.info.TagInfoDialog
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.onboarding.LoginDestinations
import com.arn.scrobble.onboarding.ShowLinkDialog
import com.arn.scrobble.pref.MediaSearchPrefDialog
import com.arn.scrobble.search.IndexerDialog
import com.arn.scrobble.ui.getActivityOrNull
import com.arn.scrobble.ui.verticalOverscanPadding
import com.arn.scrobble.updates.ChangelogDialog
import com.arn.scrobble.updates.UpdateAvailableDialog
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.VariantStuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

fun EntryProviderScope<PanoRoute>.panoModalNavGraph(
    navigate: (PanoRoute) -> Unit,
    goBack: () -> Unit,
    onSetDrawerData: (DrawerData) -> Unit,
    mainViewModel: MainViewModel,
) {
    modalEntry<PanoRoute.Modal.NavPopup> { route ->
        val user by if (route.otherUser != null)
            remember { mutableStateOf(route.otherUser) }
        else
            PlatformStuff.mainPrefs.data
                .collectAsStateWithInitialValue { it.currentAccount?.user }

        NavPopupDialog(
            user = user ?: return@modalEntry,
            initialDrawerData = route.initialDrawerData,
            drawSnowfall = mainViewModel.isItChristmas,
            onSetDrawerData = onSetDrawerData,
            onNavigate = navigate,
            modifier = modalModifier()
        )
    }

    modalEntry<PanoRoute.Modal.Changelog> { route ->
        ChangelogDialog(
            text = route.text,
            modifier = modalModifier()
        )
    }

    modalEntry<PanoRoute.Modal.ChartsLegend> {
        ChartsLegendDialog(
            modifier = modalModifier()
        )
    }

    modalEntry<PanoRoute.Modal.UpdateAvailable> { route ->
        UpdateAvailableDialog(
            updateAction = route.updateAction,
            modifier = modalModifier()
        )
    }

    modalEntry<PanoRoute.Modal.HiddenTags> {
        HiddenTagsDialog(
            modifier = modalModifier()
        )
    }

    modalEntry<PanoRoute.Modal.CollageGenerator> { route ->
        val activity = getActivityOrNull()

        CollageGeneratorDialog(
            collageType = route.collageType,
            timePeriod = route.timePeriod,
            user = route.user,
            onAskForReview = {
                VariantStuff.reviewPrompter.showIfNeeded(
                    activity,
                    { PlatformStuff.mainPrefs.data.map { it.lastReviewPromptTime }.first() }
                ) { t ->
                    PlatformStuff.mainPrefs.updateData { it.copy(lastReviewPromptTime = t) }
                }
            },
            modifier = modalModifier()
        )
    }

    modalEntry<PanoRoute.Modal.MusicEntryInfo> { route ->
        val scrollState = rememberScrollState()
        MusicEntryInfoDialog(
            musicEntry = route.artist ?: route.album ?: route.track!!,
            appId = route.appId,
            user = route.user,
            onNavigate = navigate,
            scrollState = scrollState,
            modifier = modalModifier(padding = false)
        )
    }

    modalEntry<PanoRoute.Modal.TagInfo> { route ->
        val scrollState = rememberScrollState()
        TagInfoDialog(
            tag = route.tag,
            scrollState = scrollState,
            modifier = modalModifier()
        )
    }

    modalEntry<PanoRoute.Modal.Index> {
        IndexerDialog(
            modifier = modalModifier(),
        )
    }

    modalEntry<PanoRoute.Modal.ShowLink> { route ->
        ShowLinkDialog(
            url = route.url,
            modifier = modalModifier(),
        )
    }

    modalEntry<PanoRoute.Modal.MediaSearchPref> {
        MediaSearchPrefDialog(
            modifier = modalModifier(),
        )
    }

    modalEntry<PanoRoute.Modal.BlockedMetadataAdd> { route ->
        BlockedMetadataAddDialog(
            blockedMetadata = route.blockedMetadata,
            ignoredArtist = route.ignoredArtist,
            hash = route.hash,
            onDismiss = goBack,
            onNavigateToBilling = {
                navigate(PanoRoute.Billing)
            },
            modifier = modalModifier()
        )
    }

    modalEntry<PanoRoute.Modal.EditScrobble> { route ->
        SimpleEditsAddScreen(
            simpleEdit = SimpleEdit(
                track = route.origScrobbleData.track,
                artist = route.origScrobbleData.artist,
                album = route.origScrobbleData.album.orEmpty(),
                albumArtist = route.origScrobbleData.albumArtist.orEmpty(),
                origTrack = route.origScrobbleData.track,
                origArtist = route.origScrobbleData.artist,
                origAlbum = route.origScrobbleData.album.orEmpty(),
                origAlbumArtist = route.origScrobbleData.albumArtist.orEmpty(),
                hasOrigTrack = true,
                hasOrigArtist = true,
                hasOrigAlbum = true,
                hasOrigAlbumArtist = false,
            ),
            origScrobbleData = route.origScrobbleData,
            msid = route.msid,
            hash = route.hash,
            key = route.key,
            onDone = goBack,
            onReauthenticate = {
                goBack()
                navigate(LoginDestinations.route(AccountType.LASTFM))
            },
            // this viewmodel should be scoped to the main viewmodel store owner
            viewModel = mainViewModel,
            modifier = modalModifier()
        )
    }
}


@Composable
fun modalModifier(
    padding: Boolean = true,
    scrollState: ScrollState = rememberScrollState()
): Modifier {
    return Modifier
        .fillMaxWidth()
        .then(
            if (padding)
                Modifier.padding(horizontal = 24.dp)
            else
                Modifier
        )
        .padding(bottom = verticalOverscanPadding())
        .verticalScroll(scrollState)
}

inline fun <reified K : PanoRoute.Modal> EntryProviderScope<PanoRoute>.modalEntry(
    noinline content: @Composable (K) -> Unit,
) {
    entry<K>(
        metadata = BottomSheetSceneStrategy.bottomSheet(),
        content = content
    )
}