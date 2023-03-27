package com.arn.scrobble.recents

import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.NavController
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.LastfmUnscrobbler
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.edits.EditDialogFragmentArgs
import com.arn.scrobble.scrobbleable.AccountType
import com.arn.scrobble.scrobbleable.LoginFlows
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.ui.UiUtils.showWithIcons
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.umass.lastfm.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PopupMenuUtils {

    fun cookieExists(navController: NavController): Boolean {
        val cookieExists = LastfmUnscrobbler().haveCsrfCookie() ||
                Scrobblables.byType(AccountType.LASTFM) == null // don't need cookies for others

        if (!cookieExists) {
            MaterialAlertDialogBuilder(navController.context)
                .setMessage(R.string.lastfm_reauth)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    LoginFlows(navController).go(AccountType.LASTFM)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        return cookieExists
    }

    fun editScrobble(navController: NavController, track: Track) {
        if (!Stuff.isOnline)
            navController.context.toast(R.string.unavailable_offline)
        else if (cookieExists(navController)) {
            val args = EditDialogFragmentArgs(
                artist = track.artist,
                album = track.album,
                track = track.name,
                msid = track.msid,
                timeMillis = track.playedWhen?.time ?: 0,
                nowPlaying = track.isNowPlaying
            ).toBundle()

            navController.navigate(R.id.editDialogFragment, args)
        }
    }

    fun deleteScrobble(
        navController: NavController,
        scope: CoroutineScope,
        track: Track,
        deleteAction: suspend (Boolean) -> Unit
    ) {
        if (!Stuff.isOnline)
            navController.context.toast(R.string.unavailable_offline)
        else if (cookieExists(navController)) {
            LFMRequester(scope).delete(track, deleteAction)
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

        val servicesList = mutableListOf<String>()

        @StringRes
        val state = when (p) {
            is PendingScrobble -> p.state
            is PendingLove -> p.state
            else -> throw RuntimeException("Not a Pending Item")
        }
        AccountType.values().forEach {
            if (state and (1 shl it.ordinal) != 0)
                servicesList += Scrobblables.getString(it)
        }

        if (p is PendingLove)
            popup.menu.removeItem(R.id.menu_love)

        if (servicesList.size == 1)
            popup.menu.removeItem(R.id.menu_services)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_services -> {
                    MaterialAlertDialogBuilder(context)
                        .setMessage(
                            context.getString(R.string.scrobble_services) + ":\n" +
                                    servicesList.joinToString(", ")
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
                        LFMRequester(scope).loveOrUnlove(
                            Track(
                                p.track,
                                null,
                                p.artist
                            ), true
                        )
                    }
                }
            }
            true
        }

        popup.showWithIcons()
    }
}