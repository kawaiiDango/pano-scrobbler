package com.arn.scrobble.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.charts.ChartsLegendDialog
import com.arn.scrobble.charts.CollageGeneratorDialog
import com.arn.scrobble.charts.DateDialog
import com.arn.scrobble.charts.DateRangeDialog
import com.arn.scrobble.charts.HiddenTagsDialog
import com.arn.scrobble.charts.TimeDialog
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.edits.BlockedMetadataAddDialog
import com.arn.scrobble.edits.SimpleEditsAddScreen
import com.arn.scrobble.info.MusicEntryInfoDialog
import com.arn.scrobble.info.TagInfoDialog
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.onboarding.LoginDestinations
import com.arn.scrobble.onboarding.ShowLinkDialog
import com.arn.scrobble.pref.MediaSearchPrefDialog
import com.arn.scrobble.pref.ProxyPrefDialog
import com.arn.scrobble.ui.getActivityOrNull
import com.arn.scrobble.ui.navModal
import com.arn.scrobble.ui.verticalOverscanPadding
import com.arn.scrobble.updates.ChangelogDialog
import com.arn.scrobble.updates.UpdateAvailableDialog
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.VariantStuff
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.edit

fun EntryProviderScope<PanoRoute>.panoModalNavGraph(
    onSetTitle: (PanoRoute, String) -> Unit,
    navigate: (PanoRoute) -> Unit,
    goBack: () -> Unit,
    onExpandModal: (PanoRoute.Modal.CanExpand) -> Unit,
    mainViewModel: MainViewModel,
) {
    modalEntry<PanoRoute.Modal.Changelog> { route ->
        ChangelogDialog(
            text = route.text,
            modifier = Modifier.navModal()
        )
    }

    modalEntry<PanoRoute.Modal.ChartsLegend> {
        ChartsLegendDialog(
            modifier = Modifier.navModal()
        )
    }

    modalEntry<PanoRoute.Modal.UpdateAvailable> { route ->
        UpdateAvailableDialog(
            updateAction = route.updateAction,
            modifier = Modifier.navModal()
        )
    }

    modalEntry<PanoRoute.Modal.HiddenTags> {
        HiddenTagsDialog(
            modifier = Modifier.navModal()
        )
    }

    modalEntry<PanoRoute.Modal.CollageGenerator> { route ->
        val activity = getActivityOrNull()

        CollageGeneratorDialog(
            collageType = route.collageType,
            timePeriod = route.timePeriod,
            user = route.user,
            onAskForReview = {
                VariantStuff.reviewPrompter.showIfNeeded(activity)
            },
            modifier = Modifier.navModal()
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
            onExpand = { onExpandModal(route) },
            modifier = Modifier.navModal(scrollState, expanded = route.isExpanded, sides = false)
        )
    }

    modalEntry<PanoRoute.Modal.TagInfo> { route ->
        if (route.isExpanded)
            onSetTitle(route, route.tag.name)

        val scrollState = rememberScrollState()
        TagInfoDialog(
            tag = route.tag,
            isExpanded = route.isExpanded,
            onExpand = { onExpandModal(route) },
            scrollState = scrollState,
            modifier = Modifier.navModal(scrollState, expanded = route.isExpanded)
        )
    }

    modalEntry<PanoRoute.Modal.ShowLink> { route ->
        ShowLinkDialog(
            url = route.url,
            modifier = Modifier.navModal(),
        )
    }

    modalEntry<PanoRoute.Modal.MediaSearchPref> {
        MediaSearchPrefDialog(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = verticalOverscanPadding())
        )
    }

    modalEntry<PanoRoute.Modal.ProxyPref> {
        ProxyPrefDialog(
            modifier = Modifier.navModal(),
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
            modifier = Modifier.navModal()
        )
    }

    modalEntry<PanoRoute.Modal.EditScrobble> { route ->
        val activity = getActivityOrNull()

        if (route.isExpanded)
            onSetTitle(route, stringResource(Res.string.edit))

        SimpleEditsAddScreen(
            simpleEdit = SimpleEdit(
                track = route.scrobbleData.track,
                artist = route.scrobbleData.artist,
                album = route.scrobbleData.album.orEmpty(),
                albumArtist = route.scrobbleData.albumArtist.orEmpty(),
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
            onDone = {
                goBack()
                Stuff.appScope.launch {
                    VariantStuff.reviewPrompter.showIfNeeded(activity)
                }
            },
            onReauthenticate = {
                goBack()
                navigate(LoginDestinations.route(AccountType.LASTFM))
            },
            isExpanded = route.isExpanded,
            onExpand = { onExpandModal(route) },
            // this viewmodel should be scoped to the main viewmodel store owner
            viewModel = mainViewModel,
            modifier = Modifier.navModal(expanded = route.isExpanded)
        )
    }

    entry<PanoRoute.Modal.TimePicker>(
        metadata = BottomSheetSceneStrategy.bottomSheetNoGestures()
    ) { route ->
        val resultBus = LocalResultEventBus.current

        TimeDialog(
            h = route.initialHour,
            m = route.initialMinute,
            onTimeSelected = {
                resultBus.sendResult(it)
                goBack()
            },
            modifier = Modifier
                .fillMaxWidth()
        )
    }

    entry<PanoRoute.Modal.DateRangePicker>(
        metadata = BottomSheetSceneStrategy.bottomSheetNoGestures()
    ) { route ->
        val resultBus = LocalResultEventBus.current

        DateRangeDialog(
            selectedDateRange = route.selectedDateRange,
            allowedRange = route.allowedRange,
            onDateRangeSelected = {
                resultBus.sendResult(it)
                goBack()
            },
            modifier = Modifier
                .fillMaxWidth()
        )
    }

    entry<PanoRoute.Modal.DatePicker>(
        metadata = BottomSheetSceneStrategy.bottomSheetNoGestures()
    ) { route ->
        val resultBus = LocalResultEventBus.current

        DateDialog(
            selectedDate = route.selectedDate,
            allowedRange = route.allowedRange,
            weeksOnly = route.weeksOnly,
            onDateSelected = {
                resultBus.sendResult(it)
                goBack()
            },
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

inline fun <reified K : PanoRoute.Modal> EntryProviderScope<PanoRoute>.modalEntry(
    noinline content: @Composable (K) -> Unit,
) {
    entry<K>(
        metadata = { route ->
            if (route.isModal()) {
                BottomSheetSceneStrategy.bottomSheet()
            } else {
                emptyMap()
            }
        },
        clazzContentKey = { route ->
            if (route is PanoRoute.Modal.CanExpand) {
                route.copyExpanded().toString()
            } else {
                route.toString()
            }
        },
        content = content
    )
}
