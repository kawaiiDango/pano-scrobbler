package com.arn.scrobble.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.arn.scrobble.ui.BottomSheetDialogParent
import com.arn.scrobble.ui.getActivityOrNull
import com.arn.scrobble.updates.ChangelogDialog
import com.arn.scrobble.updates.UpdateAvailableDialog
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff

@Composable
private fun PanoDialogs(
    dialogArgs: PanoDialog,
    onDismiss: () -> Unit,
    onBack: (() -> Unit)?,
    onNavigate: (PanoRoute) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    navMetadataList: () -> List<PanoNavMetadata>?,
    mainViewModel: MainViewModel,
) {
    BottomSheetDialogParent(
        padding = dialogArgs !is PanoDialog.MusicEntryInfo,
        onBack = onBack,
        onDismiss = onDismiss
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
                    onDismiss = onDismiss,
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

                        onDismiss()
                    },
                    onReauthenticate = {
                        onDismiss()
                        onNavigate(LoginDestinations.route(AccountType.LASTFM))
                    },
                    onNavigateToRegexEdits = {
                        onDismiss()
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
                    extraDataError = dialogArgs.extraDataError,
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
    onDismiss: () -> Unit,
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
            onDismiss = onDismiss,
            onBack = if (dialogStack.size == 1)
                null
            else {
                { popDialogArgs() }
            },
            onNavigate = { route ->
                onDismiss()
                onNavigate(route)
            },
            onOpenDialog = { pushDialogArgs(it) },
            navMetadataList = navMetadataList,
            mainViewModel = mainViewModel,
        )
    }
}