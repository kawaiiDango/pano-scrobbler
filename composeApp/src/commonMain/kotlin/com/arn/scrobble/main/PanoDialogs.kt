package com.arn.scrobble.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.charts.ChartsLegendDialog
import com.arn.scrobble.charts.CollageGeneratorDialog
import com.arn.scrobble.charts.HiddenTagsDialog
import com.arn.scrobble.edits.BlockedMetadataAddDialog
import com.arn.scrobble.edits.EditScrobbleDialog
import com.arn.scrobble.friends.FriendDialog
import com.arn.scrobble.info.MusicEntryInfoDialog
import com.arn.scrobble.info.TagInfoDialog
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.navigation.NavPopupDialog
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.navigation.PanoNavMetadata
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.onboarding.FixItDialog
import com.arn.scrobble.onboarding.LoginDestinations
import com.arn.scrobble.search.IndexerDialog
import com.arn.scrobble.ui.getActivityOrNull
import com.arn.scrobble.updates.ChangelogDialog
import com.arn.scrobble.updates.UpdateAvailableDialog
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.back
import pano_scrobbler.composeapp.generated.resources.close

@Composable
private fun PanoDialogs(
    dialogArgs: PanoDialog,
    onDismissRequest: () -> Unit,
    onBack: (() -> Unit)?,
    onNavigate: (PanoRoute) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    navMetadataList: () -> List<PanoNavMetadata>?,
    mainViewModel: MainViewModel,
) {
    BottomSheetDialogParent(
        padding = dialogArgs !is PanoDialog.MusicEntryInfo,
        onBack = onBack,
        isNestedScrollable = dialogArgs is PanoDialog.NestedScrollable,
        onDismissRequest = onDismissRequest
    ) { modifier ->
        when (dialogArgs) {
            is PanoDialog.NavPopup -> {
                NavPopupDialog(
                    otherUser = dialogArgs.otherUser,
                    drawerDataFlow = mainViewModel.drawerDataFlow,
                    drawSnowfall = mainViewModel.isItChristmas,
                    loadOtherUserDrawerData = mainViewModel::loadOtherUserDrawerData,
                    navMetadataList = navMetadataList() ?: emptyList(),
                    onNavigate = onNavigate,
                    modifier = modifier
                )
            }

            is PanoDialog.Changelog -> {
                ChangelogDialog(
                    modifier = modifier
                )
            }

            is PanoDialog.ChartsLegend -> {
                ChartsLegendDialog(
                    modifier = modifier
                )
            }

            is PanoDialog.UpdateAvailable -> {
                UpdateAvailableDialog(
                    githubReleases = dialogArgs.githubReleases,
                    modifier = modifier
                )
            }

            is PanoDialog.HiddenTags -> {
                HiddenTagsDialog(
                    modifier = modifier
                )
            }

            is PanoDialog.CollageGenerator -> {
                val activity = getActivityOrNull()

                CollageGeneratorDialog(
                    collageType = dialogArgs.collageType,
                    timePeriod = dialogArgs.timePeriod,
                    user = dialogArgs.user,
                    onAskForReview = {
                        PlatformStuff.promptForReview(activity)
                    },
                    modifier = modifier
                )
            }

            is PanoDialog.MusicEntryInfo -> {
                MusicEntryInfoDialog(
                    musicEntry = dialogArgs.artist ?: dialogArgs.album ?: dialogArgs.track!!,
                    pkgName = dialogArgs.pkgName,
                    user = dialogArgs.user,
                    onNavigate = onNavigate,
                    onOpenDialog = onOpenDialog,
                    modifier = modifier
                )
            }

            is PanoDialog.TagInfo -> {
                TagInfoDialog(
                    tag = dialogArgs.tag,
                    modifier = modifier
                )
            }

            PanoDialog.Index -> {
                IndexerDialog(
                    modifier = modifier,
                )
            }

            PanoDialog.FixIt -> {
                FixItDialog(
                    modifier = modifier,
                )
            }

            is PanoDialog.BlockedMetadataAdd -> {
                BlockedMetadataAddDialog(
                    blockedMetadata = dialogArgs.blockedMetadata,
                    ignoredArtist = dialogArgs.ignoredArtist,
                    hash = dialogArgs.hash,
                    onDismiss = onDismissRequest,
                    onNavigateToBilling = {
                        onNavigate(PanoRoute.Billing)
                    },
                    modifier = modifier
                )
            }

            is PanoDialog.EditScrobble -> {
                EditScrobbleDialog(
                    scrobbleData = dialogArgs.scrobbleData,
                    msid = dialogArgs.msid,
                    hash = dialogArgs.hash,
                    onDone = {
                        if (dialogArgs.hash != null) { // from notification
                            notifyPlayingTrackEvent(
                                PlayingTrackNotifyEvent.TrackCancelled(
                                    hash = dialogArgs.hash,
                                    showUnscrobbledNotification = false,
                                    markAsScrobbled = true,
                                )
                            )
                        } else if (dialogArgs.origTrack != null) { // from scrobble history
                            mainViewModel.notifyEdit(dialogArgs.origTrack, it)
                        }

                        onDismissRequest()
                    },
                    onReauthenticate = {
                        onDismissRequest()
                        onNavigate(LoginDestinations.route(AccountType.LASTFM))
                    },
                    onNavigateToRegexEdits = {
                        onDismissRequest()
                        onNavigate(PanoRoute.RegexEdits)
                    },
                    modifier = modifier
                )
            }

            is PanoDialog.Friend -> {
                FriendDialog(
                    friend = dialogArgs.friend,
                    isPinned = dialogArgs.isPinned,
                    extraData = dialogArgs.extraData,
                    extraDataFlow = mainViewModel.friendExtraData,
                    onNavigate = onNavigate,
                    onOpenDialog = onOpenDialog,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
fun PanoDialogStack(
    initialDialogArgs: PanoDialog?,
    onNavigate: (PanoRoute) -> Unit,
    onDismissRequest: () -> Unit,
    navMetadataList: () -> List<PanoNavMetadata>?,
    mainViewModel: MainViewModel,
) {
    val dialogStack = rememberSaveable(
        initialDialogArgs,
        saver = listSaver(
            save = { it.map { Stuff.myJson.encodeToString(it) } },
            restore = {
                mutableStateListOf(*it.map { Stuff.myJson.decodeFromString<PanoDialog>(it) }
                    .toTypedArray())
            }
        )
    ) {
        if (initialDialogArgs == null)
            mutableStateListOf()
        else
            mutableStateListOf(initialDialogArgs)
    }

    fun pushDialogArgs(dialog: PanoDialog) {
        dialogStack.add(dialog)
    }

    fun popDialogArgs(): PanoDialog? {
        return if (dialogStack.isNotEmpty()) {
            dialogStack.removeAt(dialogStack.lastIndex)
        } else {
            null
        }
    }

    if (dialogStack.isNotEmpty()) {
        PanoDialogs(
            dialogArgs = dialogStack.last(),
            onDismissRequest = onDismissRequest,
            onBack = if (dialogStack.size == 1)
                null
            else {
                { popDialogArgs() }
            },
            onNavigate = { route ->
                onDismissRequest()
                onNavigate(route)
            },
            onOpenDialog = { pushDialogArgs(it) },
            navMetadataList = navMetadataList,
            mainViewModel = mainViewModel,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetDialogParent(
    onDismissRequest: () -> Unit,
    onBack: (() -> Unit)?,
    padding: Boolean,
    isNestedScrollable: Boolean, // disabling nested scrolling is a workaround until google fixes it
    skipPartiallyExpanded: Boolean = PlatformStuff.isTv || PlatformStuff.isDesktop,
    content: @Composable (modifier: Modifier) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
//    val sheetGesturesEnabled = !PlatformStuff.isTv && !PlatformStuff.isDesktop &&
//            (
//                    (!scrollState.canScrollBackward && !scrollState.canScrollForward) ||
//                            (scrollState.lastScrolledBackward && !scrollState.canScrollBackward) ||
//                            (scrollState.lastScrolledForward && !scrollState.canScrollForward)
//                    )

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded
    )

    val sheetGesturesEnabled by remember { mutableStateOf(!PlatformStuff.isTv && !PlatformStuff.isDesktop) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = if (PlatformStuff.isTv || PlatformStuff.isDesktop) null
        else {
            { BottomSheetDefaults.DragHandle() }
        },
        sheetGesturesEnabled = sheetGesturesEnabled,
        sheetState = sheetState,
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(Res.string.back),
                )
            }

        } else if (!sheetGesturesEnabled) {
            IconButton(
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismissRequest()
                    }
                },
                modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(Res.string.close),
                )
            }
        }

        content(
            Modifier
                .fillMaxWidth()
                .then(
                    if (padding)
                        if (PlatformStuff.isTv || PlatformStuff.isDesktop)
                            Modifier.padding(24.dp)
                        else
                            Modifier.padding(horizontal = 24.dp)
                    else
                        Modifier
                )
                .verticalScroll(scrollState, enabled = isNestedScrollable)
        )
    }
}