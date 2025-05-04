package com.arn.scrobble.main

import androidx.compose.runtime.Composable
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.charts.ChartsLegendDialog
import com.arn.scrobble.charts.CollageGeneratorDialog
import com.arn.scrobble.charts.HiddenTagsDialog
import com.arn.scrobble.edits.BlockedMetadataAddDialog
import com.arn.scrobble.edits.EditScrobbleDialog
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

@Composable
fun PanoDialogs(
    dialogData: PanoDialog,
    onDismiss: () -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    navMetadataList: () -> List<PanoNavMetadata>?,
    mainViewModel: MainViewModel,
) {
    BottomSheetDialogParent(
        onDismiss = onDismiss
    ) { modifier ->
        when (dialogData) {
            is PanoDialog.NavPopup -> {
                NavPopupDialog(
                    otherUser = dialogData.otherUser,
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
                    githubReleases = dialogData.githubReleases,
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
                    collageType = dialogData.collageType,
                    timePeriod = dialogData.timePeriod,
                    user = dialogData.user,
                    onAskForReview = {
                        PlatformStuff.promptForReview(activity)
                    },
                    modifier = modifier
                )
            }

            is PanoDialog.MusicEntryInfo -> {
                MusicEntryInfoDialog(
                    musicEntry = dialogData.artist ?: dialogData.album ?: dialogData.track!!,
                    pkgName = dialogData.pkgName,
                    user = dialogData.user,
                    onNavigate = onNavigate,
                    onOpenDialog = onOpenDialog,
                    modifier = modifier
                )
            }

            is PanoDialog.TagInfo -> {
                TagInfoDialog(
                    tag = dialogData.tag,
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
                    blockedMetadata = dialogData.blockedMetadata,
                    ignoredArtist = dialogData.ignoredArtist,
                    hash = dialogData.hash,
                    onDismiss = onDismiss,
                    onNavigateToBilling = {
                        onNavigate(PanoRoute.Billing)
                    },
                    modifier = modifier
                )
            }

            is PanoDialog.EditScrobble -> {
                EditScrobbleDialog(
                    scrobbleData = dialogData.scrobbleData,
                    msid = dialogData.msid,
                    hash = dialogData.hash,
                    onDone = {
                        if (dialogData.hash != null) { // from notification
                            notifyPlayingTrackEvent(
                                PlayingTrackNotifyEvent.TrackCancelled(
                                    hash = dialogData.hash,
                                    showUnscrobbledNotification = false,
                                    markAsScrobbled = true,
                                )
                            )
                        } else if (dialogData.origTrack != null) { // from scrobble history
                            mainViewModel.notifyEdit(dialogData.origTrack, it)
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
        }
    }
}