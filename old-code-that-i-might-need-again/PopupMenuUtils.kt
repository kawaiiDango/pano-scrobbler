package com.arn.scrobble.recents

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.NavController
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.LastfmUnscrobbler
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.onboarding.LoginDestinations
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.showWithIcons
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PopupMenuUtils {

    fun showReauthenticatePrompt(navController: NavController) {
        MaterialAlertDialogBuilder(navController.context)
            .setMessage(R.string.lastfm_reauth)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                LoginDestinations.route(AccountType.LASTFM).let {
                    navController.navigate(it)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun editScrobble(navController: NavController, track: Track) {
        if (!Stuff.isOnline)
            navController.context.toast(R.string.unavailable_offline)
        else {
            val sd = ScrobbleData(
                track = track.name,
                artist = track.artist.name,
                album = track.album?.name,
                timestamp = track.date ?: 0,
                albumArtist = null,
                duration = null,
                packageName = null
            )

            val args = Bundle().apply {
                putParcelable(
                    "data",
                    sd
                )
                putString("msid", track.msid)
            }

            navController.navigate(R.id.editScrobbleDialogFragment, args)
        }
    }

    fun deleteScrobble(
        navController: NavController,
        scope: CoroutineScope,
        track: Track,
        deleteAction: suspend (Boolean) -> Unit,
    ) {
        scope.launch {
            val results = withContext(Dispatchers.IO) {
                ScrobbleEverywhere.delete(track)
            }
            withContext(Dispatchers.Main) {
                if (results.any { it.exceptionOrNull() is LastfmUnscrobbler.CookiesInvalidatedException })
                    showReauthenticatePrompt(navController)
                else
                    deleteAction(results.all { it.isSuccess })
            }
        }
    }

    fun openPendingPopupMenu(
        anchor: View,
        scope: CoroutineScope,
        p: Any,
    ) {
        val context = anchor.context
        val popup = PopupMenu(context, anchor)
        popup.menuInflater.inflate(R.menu.pending_item_menu, popup.menu)

        val accountTypesList = mutableListOf<AccountType>()

        @StringRes
        val state = when (p) {
            is PendingScrobble -> p.state
            is PendingLove -> p.state
            else -> throw RuntimeException("Not a Pending Item")
        }

        AccountType.entries.forEach {
            if (state and (1 shl it.ordinal) != 0)
                accountTypesList += it
        }

        if (p is PendingLove)
            popup.menu.removeItem(R.id.menu_love)

        if (accountTypesList.size == 1 && accountTypesList.first() == Scrobblables.current.value?.userAccount?.type)
            popup.menu.removeItem(R.id.menu_services)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_services -> {
                    MaterialAlertDialogBuilder(context)
                        .setMessage(
                            context.getString(R.string.scrobble_services) + ":\n" +
                                    accountTypesList.joinToString(", ") { Scrobblables.getString(it) }
                        )
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }

                R.id.menu_delete -> {
                    scope.launch(Dispatchers.IO) {
                        try {
                            if (p is PendingScrobble)
                                PanoDb.db.getPendingScrobblesDao().delete(p)
                            else if (p is PendingLove)
                                PanoDb.db.getPendingLovesDao().delete(p)
                        } catch (e: Exception) {
                        }
                    }
                }

                R.id.menu_love -> {
                    if (p is PendingScrobble) {
                        scope.launch(Dispatchers.IO) {
                            ScrobbleEverywhere.loveOrUnlove(
                                Track(
                                    p.track,
                                    p.album.ifEmpty { null }?.let {
                                        Album(
                                            it,
                                            p.albumArtist.ifEmpty { null }
                                                ?.let { Artist(it) })
                                    },
                                    Artist(p.artist)
                                ),
                                true
                            )
                        }
                    }
                }
            }
            true
        }

        popup.showWithIcons()
    }
}